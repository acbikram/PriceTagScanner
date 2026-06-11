package com.pricetag.scanner.data.network.model

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting   : ConnectionState()
    object Connected    : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}
