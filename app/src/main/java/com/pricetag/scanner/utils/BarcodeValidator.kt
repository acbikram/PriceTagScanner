package com.pricetag.scanner.utils

object BarcodeValidator {
    private const val DUP_WINDOW_MS = 2_000L

    // Per-session duplicate protection: barcode → last-scan time
    private val recentScans = mutableMapOf<String, Long>()

    /**
     * Returns true if this barcode should be processed.
     * Returns false if it was seen within the duplicate window (2 seconds).
     */
    fun shouldProcess(barcode: String): Boolean {
        val now  = System.currentTimeMillis()
        val last = recentScans[barcode] ?: 0L
        if (now - last < DUP_WINDOW_MS) return false
        recentScans[barcode] = now
        cleanup(now)
        return true
    }

    fun clear() = recentScans.clear()

    private fun cleanup(now: Long) {
        val cutoff = now - DUP_WINDOW_MS * 10
        recentScans.entries.removeAll { it.value < cutoff }
    }
}
