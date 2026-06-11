package com.pricetag.scanner.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pricetag.scanner.data.network.model.ConnectionState
import com.pricetag.scanner.presentation.theme.*

@Composable
fun ConnectionBadge(
    state:     ConnectionState,
    serverIp:  String,
    port:      Int,
    modifier:  Modifier = Modifier,
) {
    val (dotColor, label) = when (state) {
        is ConnectionState.Connected    -> PrimaryGreen  to "CONNECTED"
        is ConnectionState.Connecting   -> WarningAmber  to "CONNECTING…"
        is ConnectionState.Disconnected -> ErrorRed      to "DISCONNECTED"
        is ConnectionState.Error        -> ErrorRed      to "ERROR"
    }

    Row(
        modifier  = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(BgSurface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Indicator dot
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = label,
                color      = dotColor,
                fontWeight = FontWeight.Bold,
                fontSize   = 13.sp,
            )
            Text(
                text     = "$serverIp : $port",
                color    = TextSecondary,
                fontSize = 12.sp,
            )
        }
    }
}
