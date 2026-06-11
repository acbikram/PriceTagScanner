package com.pricetag.scanner.domain.model

data class AppSettings(
    val serverIp:      String  = "192.168.1.100",
    val serverPort:    Int     = 5000,
    val autoConnect:   Boolean = true,
    val beepEnabled:   Boolean = true,
    val vibrateEnabled:Boolean = true,
    val autoSendAfterScan: Boolean = false,
)
