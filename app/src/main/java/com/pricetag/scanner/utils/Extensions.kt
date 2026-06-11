package com.pricetag.scanner.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Long.toDisplayTime(): String =
    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(this))

fun Long.toDisplayDateTime(): String =
    SimpleDateFormat("dd/MM/yyyy  HH:mm:ss", Locale.getDefault()).format(Date(this))

/** Returns true if the barcode looks valid (at least 3 digits/chars). */
fun String.isValidBarcode(): Boolean = this.trim().length >= 3
