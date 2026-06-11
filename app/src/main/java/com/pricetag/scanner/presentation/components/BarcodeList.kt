package com.pricetag.scanner.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pricetag.scanner.presentation.theme.*
import com.pricetag.scanner.utils.toDisplayTime

/** Scrollable list of scanned barcodes. Newest first. Large text. Retail-friendly. */
@Composable
fun BarcodeList(
    barcodes:  List<Pair<String, Long>>,   // barcode → timestamp
    modifier:  Modifier = Modifier,
) {
    if (barcodes.isEmpty()) {
        Box(
            modifier          = modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(BgSurface),
            contentAlignment  = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.QrCode,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(36.dp),
                )
                Spacer(Modifier.height(8.dp))
                Text("No barcodes scanned yet", color = TextSecondary, fontSize = 14.sp)
            }
        }
        return
    }

    LazyColumn(
        modifier              = modifier,
        verticalArrangement   = Arrangement.spacedBy(4.dp),
        reverseLayout         = true,   // newest at bottom visible = newest on top when reversed
    ) {
        itemsIndexed(barcodes) { index, (barcode, ts) ->
            val isNewest = index == barcodes.lastIndex
            BarcodeRow(
                barcode   = barcode,
                timestamp = ts,
                isNewest  = isNewest,
            )
        }
    }
}

@Composable
private fun BarcodeRow(
    barcode:   String,
    timestamp: Long,
    isNewest:  Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isNewest) SlotFilled else BgSurface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.QrCode,
            contentDescription = null,
            tint = if (isNewest) PrimaryGreen else TextSecondary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text       = barcode,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isNewest) FontWeight.Bold else FontWeight.Normal,
            fontSize   = 18.sp,
            color      = if (isNewest) TextPrimary else TextPrimary,
            modifier   = Modifier.weight(1f),
        )
        Text(
            text     = timestamp.toDisplayTime(),
            color    = TextSecondary,
            fontSize = 12.sp,
        )
    }
}
