package com.pricetag.scanner.domain.model

import com.pricetag.scanner.domain.model.TagType
import com.pricetag.scanner.domain.model.UnitType

/** In-memory representation of a print job built by the user. */
data class ScanJob(
    val id:        Long     = 0L,
    val barcodes:  List<String>,   // 1 item for A4/VEG/4PCS_SAME, up to 4 for 4PCS/4PCS_DATE
    val tagType:   TagType,
    val unitType:  UnitType,
    val copies:    Int      = 1,
    val timestamp: Long     = System.currentTimeMillis(),
) {
    /** Wire format sent to the Python server. */
    fun toWireFormat(): String {
        val barcodeField = barcodes.joinToString(",")
        return "$barcodeField|${tagType.protocol}|${unitType.protocol}|$copies|$timestamp"
    }
}
