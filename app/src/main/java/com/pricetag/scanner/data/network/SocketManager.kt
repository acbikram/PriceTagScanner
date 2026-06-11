package com.pricetag.scanner.data.network

import android.util.Log
import com.pricetag.scanner.data.network.model.ConnectionState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.*
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production-grade TCP SocketManager.
 *
 * Responsibilities:
 *  - Connect / disconnect
 *  - Automatic reconnect (exponential backoff, max 30 s)
 *  - Heartbeat: sends PING every 20 s, expects PONG within 5 s
 *  - Send print jobs; returns server response string
 *  - Thread-safe via coroutine dispatcher (Dispatchers.IO)
 */
@Singleton
class SocketManager @Inject constructor() {

    companion object {
        private const val TAG               = "SocketManager"
        private const val CONNECT_TIMEOUT   = 5_000       // ms
        private const val READ_TIMEOUT      = 8_000       // ms
        private const val HEARTBEAT_INTERVAL = 20_000L    // ms
        private const val HEARTBEAT_TIMEOUT = 5_000L      // ms
        private const val MAX_RECONNECT_DELAY = 30_000L   // ms
        private const val INITIAL_RECONNECT_DELAY = 2_000L
    }

    // ── Public state ──────────────────────────────────────────────────────────
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    val isConnected: Boolean get() = _connectionState.value is ConnectionState.Connected

    // ── Internals ─────────────────────────────────────────────────────────────
    private val scope          = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var socket:        Socket?        = null
    private var writer:        PrintWriter?   = null
    private var reader:        BufferedReader? = null
    private val sendMutex      = kotlinx.coroutines.sync.Mutex()

    private var serverIp       = ""
    private var serverPort     = 5000
    private var autoReconnect  = true
    private var connectJob:    Job? = null
    private var heartbeatJob:  Job? = null
    private var reconnectDelay = INITIAL_RECONNECT_DELAY

    // ── Public API ────────────────────────────────────────────────────────────

    fun configure(ip: String, port: Int, autoReconnect: Boolean = true) {
        serverIp         = ip.trim()
        serverPort       = port
        this.autoReconnect = autoReconnect
    }

    fun connect() {
        if (_connectionState.value is ConnectionState.Connected ||
            _connectionState.value is ConnectionState.Connecting) return
        connectJob?.cancel()
        connectJob = scope.launch { doConnect() }
    }

    fun disconnect() {
        autoReconnect = false
        connectJob?.cancel()
        heartbeatJob?.cancel()
        closeSocket()
        _connectionState.value = ConnectionState.Disconnected
        Log.i(TAG, "Disconnected (user request)")
    }

    fun reconnect(ip: String, port: Int) {
        configure(ip, port, autoReconnect = true)
        disconnect()
        autoReconnect = true
        reconnectDelay = INITIAL_RECONNECT_DELAY
        connect()
    }

    /**
     * Send a payload to the Python server and return the server's response.
     * Returns null on connection error or timeout.
     * Thread-safe (uses Mutex).
     */
    suspend fun send(payload: String): String? = withContext(Dispatchers.IO) {
        sendMutex.withLock {
            val w = writer
            val r = reader
            if (w == null || r == null || socket?.isConnected != true) {
                Log.w(TAG, "send() called but not connected")
                return@withLock null
            }
            return@withLock try {
                w.println(payload)
                w.flush()
                socket?.soTimeout = READ_TIMEOUT
                val response = r.readLine()
                Log.d(TAG, "Server response: $response")
                response
            } catch (e: SocketTimeoutException) {
                Log.e(TAG, "Read timeout waiting for server response")
                handleConnectionLost("Read timeout")
                null
            } catch (e: IOException) {
                Log.e(TAG, "IO error during send: ${e.message}")
                handleConnectionLost(e.message ?: "IO error")
                null
            }
        }
    }

    // ── Private connection logic ──────────────────────────────────────────────

    private suspend fun doConnect() {
        if (serverIp.isBlank()) {
            _connectionState.value = ConnectionState.Error("No server IP configured")
            return
        }
        _connectionState.value = ConnectionState.Connecting
        Log.i(TAG, "Connecting to $serverIp:$serverPort …")

        while (currentCoroutineContext().isActive) {
            try {
                val s = Socket()
                s.connect(InetSocketAddress(serverIp, serverPort), CONNECT_TIMEOUT)
                s.soTimeout     = READ_TIMEOUT
                s.keepAlive     = true
                s.tcpNoDelay    = true
                s.setSendBufferSize(8192)
                s.setReceiveBufferSize(8192)

                socket = s
                writer = PrintWriter(BufferedWriter(OutputStreamWriter(s.getOutputStream())), true)
                reader = BufferedReader(InputStreamReader(s.getInputStream()))

                _connectionState.value = ConnectionState.Connected
                reconnectDelay = INITIAL_RECONNECT_DELAY
                Log.i(TAG, "Connected to $serverIp:$serverPort")

                startHeartbeat()
                return   // connected; heartbeat loop maintains the connection

            } catch (e: Exception) {
                Log.w(TAG, "Connection failed: ${e.message}. Retry in ${reconnectDelay}ms")
                _connectionState.value = ConnectionState.Error(e.message ?: "Connection failed")
                closeSocket()
                if (!autoReconnect) return
                delay(reconnectDelay)
                reconnectDelay = minOf(reconnectDelay * 2, MAX_RECONNECT_DELAY)
            }
        }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(HEARTBEAT_INTERVAL)
                sendMutex.withLock {
                    val w = writer ?: return@withLock
                    val r = reader ?: return@withLock
                    try {
                        w.println("PING")
                        w.flush()
                        socket?.soTimeout = HEARTBEAT_TIMEOUT.toInt()
                        val resp = r.readLine()
                        socket?.soTimeout = READ_TIMEOUT
                        if (resp == null) {
                            handleConnectionLost("Server closed connection")
                        }
                        // PONG or any response is fine — just confirms the connection is alive
                    } catch (e: Exception) {
                        handleConnectionLost("Heartbeat failed: ${e.message}")
                    }
                }
            }
        }
    }

    private fun handleConnectionLost(reason: String) {
        Log.w(TAG, "Connection lost: $reason")
        heartbeatJob?.cancel()
        closeSocket()
        _connectionState.value = ConnectionState.Disconnected
        if (autoReconnect) {
            connectJob?.cancel()
            connectJob = scope.launch { doConnect() }
        }
    }

    private fun closeSocket() {
        try { writer?.close() }    catch (_: Exception) {}
        try { reader?.close() }    catch (_: Exception) {}
        try { socket?.close() }    catch (_: Exception) {}
        socket = null
        writer = null
        reader = null
    }
}
