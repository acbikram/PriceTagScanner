package com.pricetag.scanner.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pricetag.scanner.presentation.theme.*

/**
 * 2×2 grid showing the 4 slots for 4PCS / 4PCS_DATE workflows.
 * Each slot shows its barcode when filled, or an empty placeholder.
 */
@Composable
fun SlotGrid(
    slots:    List<String>,    // exactly 4 items; blank = empty
    modifier: Modifier = Modifier,
) {
    val filledCount = slots.count { it.isNotBlank() }

    Column(modifier = modifier) {
        // Progress indicator
        Row(
            modifier          = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text       = "$filledCount / 4 scanned",
                color      = if (filledCount == 4) PrimaryGreen else WarningAmber,
                fontWeight = FontWeight.Bold,
                fontSize   = 16.sp,
            )
            // Mini progress dots
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(4) { idx ->
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(
                                if (idx < filledCount) PrimaryGreen
                                else BgInput
                            )
                    )
                }
            }
        }

        // 2 × 2 grid
        for (row in 0..1) {
            Row(
                modifier              = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (col in 0..1) {
                    val idx     = row * 2 + col
                    val barcode = slots.getOrElse(idx) { "" }
                    val filled  = barcode.isNotBlank()
                    SlotCell(
                        slotNumber = idx + 1,
                        barcode    = barcode,
                        filled     = filled,
                        modifier   = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SlotCell(
    slotNumber: Int,
    barcode:    String,
    filled:     Boolean,
    modifier:   Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(88.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (filled) SlotFilled else SlotEmpty)
            .border(
                width = if (filled) 2.dp else 1.dp,
                color = if (filled) SlotFilledBorder else BorderColor,
                shape = RoundedCornerShape(10.dp),
            )
            .padding(10.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (filled) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text       = barcode,
                    color      = TextPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 13.sp,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                )
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text     = "Slot $slotNumber",
                    color    = TextSecondary,
                    fontSize = 12.sp,
                )
            }
        }
    }
}
