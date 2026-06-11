package com.pricetag.scanner.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pricetag.scanner.data.network.model.ConnectionState
import com.pricetag.scanner.domain.model.TagType
import com.pricetag.scanner.domain.model.UnitType
import com.pricetag.scanner.presentation.components.BarcodeList
import com.pricetag.scanner.presentation.components.ConnectionBadge
import com.pricetag.scanner.presentation.components.SlotGrid
import com.pricetag.scanner.presentation.scanner.ScannerScreen
import com.pricetag.scanner.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateHistory:  () -> Unit,
    onNavigateSettings: () -> Unit,
    viewModel: MainViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show snackbar messages
    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.onEvent(MainEvent.SnackbarDismissed)
        }
    }

    // Scanner overlay (full screen)
    if (state.showScanner) {
        ScannerScreen(
            onBarcodeScanned = { viewModel.onEvent(MainEvent.BarcodeScanned(it)) },
            onDismiss        = { viewModel.onEvent(MainEvent.CloseScanner) },
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BgDark,
        topBar = {
            AppTopBar(
                pendingCount    = state.pendingJobsCount,
                onHistoryClick  = onNavigateHistory,
                onSettingsClick = onNavigateSettings,
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            // ── Connection status ─────────────────────────────────────────────
            ConnectionBadge(
                state     = state.connectionState,
                serverIp  = state.settings.serverIp,
                port      = state.settings.serverPort,
            )

            // Reconnect button when disconnected
            if (state.connectionState !is ConnectionState.Connected) {
                RetailButton(
                    text     = "⟳  Reconnect",
                    color    = WarningAmber,
                    onClick  = { viewModel.onEvent(MainEvent.Reconnect) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                )
            }

            // ── Tag Type selector ─────────────────────────────────────────────
            SectionLabel("Tag Type")
            TagTypeSelector(
                selected = state.selectedTagType,
                onSelect = { viewModel.onEvent(MainEvent.TagTypeSelected(it)) },
            )

            // ── Unit Type selector ────────────────────────────────────────────
            SectionLabel("Unit Type")
            UnitTypeSelector(
                selected = state.selectedUnitType,
                onSelect = { viewModel.onEvent(MainEvent.UnitTypeSelected(it)) },
            )

            // ── Barcode area ──────────────────────────────────────────────────
            SectionLabel(
                if (state.selectedTagType.is4PcsVariant && state.selectedTagType != TagType.FOUR_PCS_SAME)
                    "Slots  (${state.slots.count { it.isNotBlank() }}/4)"
                else
                    "Scanned Barcodes"
            )

            if (state.selectedTagType.is4PcsVariant && state.selectedTagType != TagType.FOUR_PCS_SAME) {
                SlotGrid(slots = state.slots, modifier = Modifier.fillMaxWidth())
            } else {
                BarcodeList(
                    barcodes = state.scannedList,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp, max = 220.dp),
                )
            }

            Spacer(Modifier.height(4.dp))

            // ── Action buttons ────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // SCAN button
                RetailButton(
                    text     = "📷  SCAN",
                    color    = PrimaryTeal,
                    onClick  = { viewModel.onEvent(MainEvent.OpenScanner) },
                    modifier = Modifier.weight(1f).height(64.dp),
                )
                // UNDO button
                RetailButton(
                    text     = "⌫",
                    color    = BgSurface,
                    textColor= TextSecondary,
                    onClick  = { viewModel.onEvent(MainEvent.RemoveLastBarcode) },
                    modifier = Modifier.width(64.dp).height(64.dp),
                    outlined = true,
                )
            }

            // SEND button
            RetailButton(
                text     = if (state.isSending) "Sending…" else "▶  SEND TO PRINTER",
                color    = PrimaryGreen,
                onClick  = { if (!state.isSending) viewModel.onEvent(MainEvent.SendPressed) },
                modifier = Modifier.fillMaxWidth().height(68.dp),
                loading  = state.isSending,
            )

            // CLEAR button
            RetailButton(
                text     = "✕  CLEAR ALL",
                color    = BgSurface,
                textColor= ErrorRed,
                onClick  = { viewModel.onEvent(MainEvent.ClearPressed) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                outlined = true,
            )

            Spacer(Modifier.height(16.dp))
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────
    if (state.showCopiesDialog) {
        CopiesDialog(
            onConfirm = { viewModel.onEvent(MainEvent.CopiesEntered(it)) },
            onDismiss = { viewModel.onEvent(MainEvent.CopiesEntered(1)) },
        )
    }

    if (state.showSameSlotsDialog) {
        SameSlotsDialog(
            barcode   = state.sameBarcode,
            onConfirm = { viewModel.onEvent(MainEvent.SameSlotsChosen(it)) },
            onDismiss = { viewModel.onEvent(MainEvent.ClearPressed) },
        )
    }

    if (state.showSendConfirm) {
        PartialSendDialog(
            filledCount = state.slots.count { it.isNotBlank() },
            onConfirm   = { viewModel.onEvent(MainEvent.SendConfirmed) },
            onDismiss   = { _state -> viewModel.onEvent(MainEvent.SnackbarDismissed) },
        )
    }
}

// ── Top App Bar ───────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(
    pendingCount:    Int,
    onHistoryClick:  () -> Unit,
    onSettingsClick: () -> Unit,
) {
    TopAppBar(
        title = {
            Column {
                Text("Price Tag Scanner", fontWeight = FontWeight.Bold,
                     color = TextPrimary, fontSize = 17.sp)
            }
        },
        actions = {
            if (pendingCount > 0) {
                BadgedBox(
                    badge = {
                        Badge(containerColor = ErrorRed) {
                            Text("$pendingCount", fontSize = 10.sp, color = TextPrimary)
                        }
                    },
                    modifier = Modifier.padding(end = 4.dp),
                ) {
                    Icon(Icons.Default.CloudQueue, contentDescription = "Pending jobs",
                         tint = WarningAmber, modifier = Modifier.size(26.dp))
                }
            }
            IconButton(onClick = onHistoryClick) {
                Icon(Icons.Default.History, contentDescription = "History",
                     tint = TextSecondary, modifier = Modifier.size(26.dp))
            }
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Default.Settings, contentDescription = "Settings",
                     tint = TextSecondary, modifier = Modifier.size(26.dp))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = BgSurface,
            titleContentColor = TextPrimary,
        ),
    )
}

// ── Tag Type Selector (horizontal chips) ─────────────────────────────────────
@Composable
private fun TagTypeSelector(
    selected: TagType,
    onSelect: (TagType) -> Unit,
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TagType.values().forEach { type ->
            val isSelected = type == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) PrimaryTeal else BgSurface)
                    .clickable { onSelect(type) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text       = type.label,
                    color      = if (isSelected) TextOnPrimary else TextSecondary,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize   = 13.sp,
                    textAlign  = TextAlign.Center,
                    maxLines   = 2,
                )
            }
        }
    }
}

// ── Unit Type Selector ────────────────────────────────────────────────────────
@Composable
private fun UnitTypeSelector(
    selected: UnitType,
    onSelect: (UnitType) -> Unit,
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        UnitType.values().forEach { type ->
            val isSelected = type == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) PrimaryTeal.copy(alpha = 0.85f) else BgSurface)
                    .clickable { onSelect(type) }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text       = type.label,
                    color      = if (isSelected) TextOnPrimary else TextSecondary,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize   = 15.sp,
                    textAlign  = TextAlign.Center,
                )
            }
        }
    }
}

// ── Copies Dialog ─────────────────────────────────────────────────────────────
@Composable
private fun CopiesDialog(
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf("1") }
    val parsed = text.toIntOrNull()?.coerceIn(1, 99) ?: 1

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(BgSurface)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("How many copies?", fontWeight = FontWeight.Bold,
                 fontSize = 20.sp, color = TextPrimary)
            Spacer(Modifier.height(16.dp))

            // Quick quantity buttons
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(1, 2, 4, 6).forEach { qty ->
                    OutlinedButton(
                        onClick = { text = qty.toString() },
                        colors  = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (parsed == qty) PrimaryTeal else Color.Transparent,
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, if (parsed == qty) PrimaryTeal else BorderColor),
                        modifier = Modifier.size(56.dp),
                    ) {
                        Text("$qty",
                             color = if (parsed == qty) TextOnPrimary else TextPrimary,
                             fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value         = text,
                onValueChange = { if (it.length <= 2 && (it.isEmpty() || it.all(Char::isDigit))) text = it },
                label         = { Text("Custom quantity", color = TextSecondary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors          = OutlinedTextFieldDefaults.colors(
                    focusedTextColor    = TextPrimary,
                    unfocusedTextColor  = TextPrimary,
                    focusedBorderColor  = PrimaryTeal,
                    unfocusedBorderColor= BorderColor,
                    cursorColor         = PrimaryTeal,
                ),
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(20.dp))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick  = onDismiss,
                    modifier = Modifier.weight(1f).height(52.dp),
                    border   = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                ) {
                    Text("Cancel", color = TextSecondary, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick  = { onConfirm(parsed) },
                    modifier = Modifier.weight(1f).height(52.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                ) {
                    Text("Confirm ×$parsed", color = TextOnPrimary,
                         fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

// ── 4PCS_SAME: How many slots? ────────────────────────────────────────────────
@Composable
private fun SameSlotsDialog(
    barcode:   String,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(BgSurface)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("4PCS SAME", fontWeight = FontWeight.Bold,
                 color = PrimaryTeal, fontSize = 18.sp)
            Spacer(Modifier.height(6.dp))
            Text("Barcode: $barcode", color = TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(16.dp))
            Text("How many slots should use this barcode?",
                 color = TextPrimary, fontSize = 15.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                listOf(1, 2, 3, 4).forEach { n ->
                    Button(
                        onClick  = { onConfirm(n) },
                        modifier = Modifier.weight(1f).height(60.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                    ) {
                        Text("$n", color = TextOnPrimary, fontWeight = FontWeight.Bold,
                             fontSize = 20.sp)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    }
}

// ── Partial send confirmation ─────────────────────────────────────────────────
@Composable
private fun PartialSendDialog(
    filledCount: Int,
    onConfirm:   () -> Unit,
    onDismiss:   (Unit) -> Unit,
) {
    AlertDialog(
        onDismissRequest    = { onDismiss(Unit) },
        containerColor      = BgSurface,
        title = {
            Text("Partial Print?", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Text(
                "Only $filledCount of 4 slots are filled.\nEmpty slots will be padded.\nContinue printing?",
                color = TextSecondary,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors  = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
            ) {
                Text("YES, PRINT", color = TextOnPrimary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss(Unit) }) {
                Text("No", color = TextSecondary)
            }
        },
    )
}

// ── Reusable retail button ────────────────────────────────────────────────────
@Composable
private fun RetailButton(
    text:      String,
    color:     Color,
    onClick:   () -> Unit,
    modifier:  Modifier = Modifier,
    textColor: Color    = TextOnPrimary,
    outlined:  Boolean  = false,
    loading:   Boolean  = false,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (outlined) Color.Transparent else color)
            .then(
                if (outlined)
                    Modifier.background(Color.Transparent)
                        .then(Modifier)   // border applied below via border modifier
                else Modifier
            )
            .clickable { if (!loading) onClick() }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(
                color    = textColor,
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.5.dp,
            )
        } else {
            Text(
                text       = text,
                color      = if (outlined) color else textColor,
                fontWeight = FontWeight.Bold,
                fontSize   = 16.sp,
                textAlign  = TextAlign.Center,
            )
        }
    }
}

// ── Section label ─────────────────────────────────────────────────────────────
@Composable
private fun SectionLabel(text: String) {
    Text(
        text       = text.uppercase(),
        color      = TextSecondary,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 11.sp,
        modifier   = Modifier.padding(top = 4.dp, bottom = 2.dp),
    )
}

// Workaround for Dialog dismiss lambda type
private val _state = Unit
