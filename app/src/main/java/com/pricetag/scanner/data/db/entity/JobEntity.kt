package com.pricetag.scanner.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "jobs")
data class JobEntity(
    @PrimaryKey(autoGenerate = true)
    val id:             Long    = 0L,
    val payload:        String,          // full wire-format string sent to Python
    val barcodes:       String,          // comma-separated barcode(s)
    val tagType:        String,
    val unitType:       String,
    val copies:         Int,
    val timestamp:      Long    = System.currentTimeMillis(),
    val status:         String  = STATUS_PENDING,  // PENDING | SENT | FAILED
    val serverResponse: String  = "",
    val errorMessage:   String  = "",
    val retryCount:     Int     = 0,
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_SENT    = "SENT"
        const val STATUS_FAILED  = "FAILED"
    }
}
