package com.pricetag.scanner.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pricetag.scanner.data.network.SocketManager
import com.pricetag.scanner.data.network.model.ConnectionState
import com.pricetag.scanner.domain.model.AppSettings
import com.pricetag.scanner.domain.model.ScanJob
import com.pricetag.scanner.domain.model.TagType
import com.pricetag.scanner.domain.model.UnitType
import com.pricetag.scanner.domain.repository.JobRepository
import com.pricetag.scanner.domain.repository.SettingsRepository
import com.pricetag.scanner.domain.usecase.SendJobUseCase
import com.pricetag.scanner.domain.usecase.SendResult
import com.pricetag.scanner.utils.SoundHapticManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── UI State ──────────────────────────────────────────────────────────────────

data class MainUiState(
    // Connection
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val settings:        AppSettings     = AppSettings(),

    // Tag / Unit selection
    val selectedTagType:  TagType  = TagType.FOUR_PCS,
    val selectedUnitType: UnitType = UnitType.PCS,

    // Barcode slots: 4 for 4PCS variants; 1 slot for A4/VEG
    // Stored as MutableList internally but exposed as List
    val slots: List<String> = List(4) { "" },    // always 4 slots; extra ignored for single

    // Single-barcode scanned items list (newest first shown in UI)
    val scannedList: List<Pair<String, Long>> = emptyList(),  // barcode → timestamp

    // 4PCS_SAME: scan one barcode, user picks how many slots it fills
    val sameBarcode: String = "",

    // Copies
    val copies: Int = 1,

    // Flow control
    val showScanner:     Boolean = false,
    val showCopiesDialog:Boolean = false,
    val showSameSlotsDialog: Boolean = false,
    val showSendConfirm: Boolean = false,   // partial 4PCS confirmation
    val pendingBarcode:  String  = "",      // barcode waiting for copies input

    // Feedback
    val snackbarMessage:  String? = null,
    val isSending:        Boolean = false,
    val lastSendStatus:   SendStatus? = null,

    // Offline queue count
    val pendingJobsCount: Int = 0,
)

enum class SendStatus { SUCCESS, QUEUED, FAILURE }

// ── Events ────────────────────────────────────────────────────────────────────

sealed class MainEvent {
    // User actions
    object OpenScanner              : MainEvent()
    object CloseScanner             : MainEvent()
    data class TagTypeSelected(val type:     TagType)  : MainEvent()
    data class UnitTypeSelected(val type:    UnitType) : MainEvent()
    data class BarcodeScanned(val barcode:   String)   : MainEvent()
    data class CopiesEntered(val copies:     Int)      : MainEvent()
    data class SameSlotsChosen(val slotCount: Int)     : MainEvent()
    object SendPressed              : MainEvent()
    object SendConfirmed            : MainEvent()     // partial 4PCS confirmed
    object ClearPressed             : MainEvent()
    object RemoveLastBarcode        : MainEvent()
    object SnackbarDismissed        : MainEvent()
    // Settings applied from SettingsScreen
    data class SettingsUpdated(val s: AppSettings) : MainEvent()
    // Reconnect button
    object Reconnect                : MainEvent()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class MainViewModel @Inject constructor(
    private val socketManager:   SocketManager,
    private val settingsRepo:    SettingsRepository,
    private val jobRepository:   JobRepository,
    private val sendJobUseCase:  SendJobUseCase,
    private val soundHaptic:     SoundHapticManager,
) : ViewModel() {

    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    private var retryJob: Job? = null

    init {
        observeSettings()
        observeConnectionState()
        observePendingCount()
        startOfflineRetryLoop()
    }

    // ── Event handler ─────────────────────────────────────────────────────────

    fun onEvent(event: MainEvent) {
        when (event) {
            is MainEvent.OpenScanner    -> _state.update { it.copy(showScanner = true) }
            is MainEvent.CloseScanner   -> _state.update { it.copy(showScanner = false) }

            is MainEvent.TagTypeSelected -> {
                val newSlots = when {
                    event.type.is4PcsVariant && event.type != TagType.FOUR_PCS_SAME ->
                        List(4) { "" }
                    else -> List(4) { "" }
                }
                _state.update {
                    it.copy(
                        selectedTagType  = event.type,
                        slots            = newSlots,
                        scannedList      = emptyList(),
                        sameBarcode      = "",
                        copies           = 1,
                    )
                }
            }

            is MainEvent.UnitTypeSelected ->
                _state.update { it.copy(selectedUnitType = event.type) }

            is MainEvent.BarcodeScanned  -> handleBarcodeScanned(event.barcode)

            is MainEvent.CopiesEntered   -> handleCopiesEntered(event.copies)

            is MainEvent.SameSlotsChosen -> handleSameSlotsChosen(event.slotCount)

            is MainEvent.SendPressed     -> handleSendPressed()

            is MainEvent.SendConfirmed   -> doSend()

            is MainEvent.ClearPressed    -> clearAll()

            is MainEvent.RemoveLastBarcode -> removeLastBarcode()

            is MainEvent.SnackbarDismissed -> _state.update { it.copy(snackbarMessage = null) }

            is MainEvent.SettingsUpdated -> applySettings(event.s)

            is MainEvent.Reconnect -> {
                val s = _state.value.settings
                socketManager.reconnect(s.serverIp, s.serverPort)
            }
        }
    }

    // ── Barcode scan handling ─────────────────────────────────────────────────

    private fun handleBarcodeScanned(barcode: String) {
        val trimmed = barcode.trim()
        if (trimmed.isBlank()) return
        val s = _state.value

        soundHaptic.beepSuccess(s.settings.beepEnabled)
        soundHaptic.vibrate(s.settings.vibrateEnabled)

        when {
            // 4PCS_SAME: scan one barcode → ask how many slots
            s.selectedTagType == TagType.FOUR_PCS_SAME -> {
                _state.update {
                    it.copy(
                        showScanner          = false,
                        sameBarcode          = trimmed,
                        showSameSlotsDialog  = true,
                    )
                }
            }

            // 4PCS / 4PCS_DATE: fill next empty slot
            s.selectedTagType.is4PcsVariant -> {
                val newSlots = s.slots.toMutableList()
                val emptyIdx = newSlots.indexOfFirst { it.isBlank() }
                if (emptyIdx == -1) {
                    // All slots full — ignore extra scan
                    _state.update { it.copy(showScanner = false) }
                    return
                }
                newSlots[emptyIdx] = trimmed
                val allFilled = newSlots.all { it.isNotBlank() }
                _state.update {
                    it.copy(
                        slots       = newSlots,
                        showScanner = !allFilled,   // keep scanner open if more slots needed
                    )
                }
                // Auto-trigger copies dialog when 4/4 filled
                if (allFilled) {
                    _state.update { it.copy(showCopiesDialog = true) }
                }
            }

            // A4 / VEG: single or multi-scan — ask copies after each scan
            else -> {
                _state.update {
                    it.copy(
                        showScanner    = false,
                        pendingBarcode = trimmed,
                        showCopiesDialog = true,
                    )
                }
            }
        }
    }

    // ── Copies dialog ─────────────────────────────────────────────────────────

    private fun handleCopiesEntered(copies: Int) {
        val s = _state.value
        _state.update { it.copy(showCopiesDialog = false, copies = copies) }

        when {
            s.selectedTagType.is4PcsVariant && s.selectedTagType != TagType.FOUR_PCS_SAME -> {
                // 4PCS — slots already filled; proceed to send
                doSend()
            }
            else -> {
                // A4 / VEG — add barcode to list; check auto-send
                val newEntry = Pair(s.pendingBarcode, System.currentTimeMillis())
                val newList  = s.scannedList + newEntry
                _state.update { it.copy(scannedList = newList, pendingBarcode = "") }
                if (s.settings.autoSendAfterScan) doSend()
            }
        }
    }

    // ── 4PCS_SAME slots chosen ────────────────────────────────────────────────

    private fun handleSameSlotsChosen(slotCount: Int) {
        val barcode   = _state.value.sameBarcode
        val newSlots  = List(4) { idx -> if (idx < slotCount) barcode else "" }
        _state.update {
            it.copy(
                slots               = newSlots,
                showSameSlotsDialog = false,
                showCopiesDialog    = true,
            )
        }
    }

    // ── Send ──────────────────────────────────────────────────────────────────

    private fun handleSendPressed() {
        val s = _state.value
        // If 4PCS has partial fill, ask for confirmation
        if (s.selectedTagType.is4PcsVariant && s.selectedTagType != TagType.FOUR_PCS_SAME) {
            val filledCount = s.slots.count { it.isNotBlank() }
            if (filledCount < 4 && filledCount > 0) {
                _state.update { it.copy(showSendConfirm = true) }
                return
            }
        }
        doSend()
    }

    private fun doSend() {
        _state.update { it.copy(showSendConfirm = false, isSending = true) }
        val s = _state.value

        // Build barcode list
        val barcodes: List<String> = when {
            s.selectedTagType.is4PcsVariant ->
                s.slots.map { it.ifBlank { "000000" } }  // pad empty slots
            else ->
                s.scannedList.map { it.first }.ifEmpty {
                    if (s.pendingBarcode.isNotBlank()) listOf(s.pendingBarcode) else emptyList()
                }
        }

        if (barcodes.isEmpty() || barcodes.all { it.isBlank() || it == "000000" }) {
            _state.update {
                it.copy(isSending = false, snackbarMessage = "Nothing to send — scan a barcode first")
            }
            return
        }

        val job = ScanJob(
            barcodes  = barcodes,
            tagType   = s.selectedTagType,
            unitType  = s.selectedUnitType,
            copies    = s.copies,
        )

        viewModelScope.launch {
            val result = sendJobUseCase(job)
            _state.update { st ->
                when (result) {
                    is SendResult.Success -> st.copy(
                        isSending        = false,
                        lastSendStatus   = SendStatus.SUCCESS,
                        snackbarMessage  = "✅ Sent! Job ${result.serverResponse}",
                        scannedList      = emptyList(),
                        slots            = List(4) { "" },
                        sameBarcode      = "",
                        copies           = 1,
                    )
                    is SendResult.Queued  -> st.copy(
                        isSending        = false,
                        lastSendStatus   = SendStatus.QUEUED,
                        snackbarMessage  = "📥 Saved offline — will send when connected",
                        scannedList      = emptyList(),
                        slots            = List(4) { "" },
                        sameBarcode      = "",
                        copies           = 1,
                    )
                    is SendResult.Failure -> st.copy(
                        isSending        = false,
                        lastSendStatus   = SendStatus.FAILURE,
                        snackbarMessage  = "❌ ${result.message}",
                    )
                }
            }
        }
    }

    // ── Clear ─────────────────────────────────────────────────────────────────

    private fun clearAll() {
        BarcodeValidator.clear()
        _state.update {
            it.copy(
                slots           = List(4) { "" },
                scannedList     = emptyList(),
                sameBarcode     = "",
                copies          = 1,
                pendingBarcode  = "",
                snackbarMessage = null,
                lastSendStatus  = null,
            )
        }
    }

    private fun removeLastBarcode() {
        val s = _state.value
        if (s.selectedTagType.is4PcsVariant) {
            // Clear last filled slot
            val newSlots = s.slots.toMutableList()
            val lastFilled = newSlots.indexOfLast { it.isNotBlank() }
            if (lastFilled >= 0) newSlots[lastFilled] = ""
            _state.update { it.copy(slots = newSlots) }
        } else {
            if (s.scannedList.isNotEmpty()) {
                _state.update { it.copy(scannedList = s.scannedList.dropLast(1)) }
            }
        }
    }

    // ── Settings / connection observers ───────────────────────────────────────

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepo.getSettings().collect { settings ->
                _state.update { it.copy(settings = settings) }
                socketManager.configure(settings.serverIp, settings.serverPort, settings.autoConnect)
                if (settings.autoConnect) socketManager.connect()
            }
        }
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            socketManager.connectionState.collect { connState ->
                _state.update { it.copy(connectionState = connState) }
                // Attempt to flush pending jobs when we come online
                if (connState is ConnectionState.Connected) {
                    flushPendingJobs()
                }
            }
        }
    }

    private fun observePendingCount() {
        viewModelScope.launch {
            jobRepository.getPendingJobs().collect { list ->
                _state.update { it.copy(pendingJobsCount = list.size) }
            }
        }
    }

    private fun applySettings(s: AppSettings) {
        viewModelScope.launch {
            settingsRepo.saveSettings(s)
            socketManager.reconnect(s.serverIp, s.serverPort)
        }
    }

    // ── Offline retry loop ────────────────────────────────────────────────────

    private fun startOfflineRetryLoop() {
        retryJob = viewModelScope.launch {
            while (true) {
                delay(15_000L)   // check every 15 s
                if (socketManager.isConnected) flushPendingJobs()
            }
        }
    }

    private suspend fun flushPendingJobs() {
        val pending = jobRepository.getPendingList()
        if (pending.isEmpty()) return
        for (job in pending) {
            if (!socketManager.isConnected) break
            val response = socketManager.send(job.payload) ?: continue
            if (response.startsWith("OK")) {
                jobRepository.markSent(job.id, response)
            } else {
                jobRepository.markFailed(job.id, response)
            }
            delay(200L)   // small gap between retried jobs
        }
    }

    override fun onCleared() {
        super.onCleared()
        retryJob?.cancel()
    }
}
