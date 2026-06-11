package com.pricetag.scanner.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pricetag.scanner.data.db.entity.JobEntity
import com.pricetag.scanner.presentation.theme.*
import com.pricetag.scanner.utils.toDisplayDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack:    () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state   by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.snackbar) {
        state.snackbar?.let {
            snackbar.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.dismissSnackbar()
        }
    }

    Scaffold(
        snackbarHost   = { SnackbarHost(snackbar) },
        containerColor = BgDark,
        topBar = {
            TopAppBar(
                title = {
                    Text("Print History  (${state.filtered.size})",
                         fontWeight = FontWeight.Bold, color = TextPrimary)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearAll() }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear all",
                             tint = ErrorRed)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgSurface),
            )
        },
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            // Search bar
            OutlinedTextField(
                value         = state.query,
                onValueChange = { viewModel.search(it) },
                placeholder   = { Text("Search barcode, tag type…", color = TextSecondary) },
                leadingIcon   = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary)
                },
                trailingIcon  = if (state.query.isNotEmpty()) ({
                    IconButton(onClick = { viewModel.search("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                    }
                }) else null,
                singleLine    = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor     = TextPrimary,
                    unfocusedTextColor   = TextPrimary,
                    focusedBorderColor   = PrimaryTeal,
                    unfocusedBorderColor = BorderColor,
                    cursorColor          = PrimaryTeal,
                ),
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                shape    = RoundedCornerShape(10.dp),
            )

            if (state.filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.History, contentDescription = null,
                             tint = TextSecondary, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("No jobs found", color = TextSecondary, fontSize = 15.sp)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(state.filtered, key = { it.id }) { job ->
                        HistoryRow(
                            job        = job,
                            isResending = job.id in state.isResending,
                            onResend   = { viewModel.resend(job) },
                            onDelete   = { viewModel.delete(job.id) },
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    job:        JobEntity,
    isResending: Boolean,
    onResend:   () -> Unit,
    onDelete:   () -> Unit,
) {
    val statusColor = when (job.status) {
        JobEntity.STATUS_SENT    -> PrimaryGreen
        JobEntity.STATUS_PENDING -> WarningAmber
        else                     -> ErrorRed
    }
    val statusLabel = when (job.status) {
        JobEntity.STATUS_SENT    -> "SENT"
        JobEntity.STATUS_PENDING -> "PENDING"
        else                     -> "FAILED"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(BgSurface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            // Barcodes
            Text(
                text       = job.barcodes.replace(",", "  ·  "),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize   = 15.sp,
                color      = TextPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Chip(job.tagType, PrimaryTeal)
                Chip(job.unitType, TextSecondary)
                Chip("×${job.copies}", TextSecondary)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text     = job.timestamp.toDisplayDateTime(),
                color    = TextSecondary,
                fontSize = 11.sp,
            )
            if (job.errorMessage.isNotBlank()) {
                Text(
                    text     = job.errorMessage,
                    color    = ErrorRed,
                    fontSize = 11.sp,
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        // Status badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(statusColor.copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(statusLabel, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }

        Spacer(Modifier.width(6.dp))

        // Re-send button
        if (job.status != JobEntity.STATUS_SENT) {
            IconButton(onClick = onResend, modifier = Modifier.size(40.dp)) {
                if (isResending) {
                    CircularProgressIndicator(
                        color = PrimaryGreen, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Send, contentDescription = "Resend",
                         tint = PrimaryGreen, modifier = Modifier.size(22.dp))
                }
            }
        }

        // Delete button
        IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "Delete",
                 tint = ErrorRed, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun Chip(text: String, color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}
