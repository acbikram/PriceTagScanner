package com.pricetag.scanner.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pricetag.scanner.domain.model.AppSettings
import com.pricetag.scanner.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack:     () -> Unit,
    viewModel:  SettingsViewModel = hiltViewModel(),
) {
    val current  by viewModel.settings.collectAsStateWithLifecycle()
    val saved    by viewModel.saved.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(saved) {
        if (saved) {
            snackbar.showSnackbar("Settings saved ✅", duration = SnackbarDuration.Short)
            viewModel.clearSaved()
        }
    }

    // Local edit state
    var ip         by remember(current) { mutableStateOf(current.serverIp) }
    var port       by remember(current) { mutableStateOf(current.serverPort.toString()) }
    var autoConn   by remember(current) { mutableStateOf(current.autoConnect) }
    var beep       by remember(current) { mutableStateOf(current.beepEnabled) }
    var vibrate    by remember(current) { mutableStateOf(current.vibrateEnabled) }
    var autoSend   by remember(current) { mutableStateOf(current.autoSendAfterScan) }

    Scaffold(
        snackbarHost  = { SnackbarHost(snackbar) },
        containerColor = BgDark,
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
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
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── Server section ────────────────────────────────────────────────
            SettingsCard(title = "Server Connection") {
                SettingsTextField(
                    label = "Server IP Address",
                    value = ip,
                    onChange = { ip = it },
                    hint = "e.g. 192.168.1.100",
                    keyboardType = KeyboardType.Uri,
                )
                Spacer(Modifier.height(10.dp))
                SettingsTextField(
                    label = "Port",
                    value = port,
                    onChange = { if (it.length <= 5 && (it.isEmpty() || it.all(Char::isDigit))) port = it },
                    hint = "Default: 5000",
                    keyboardType = KeyboardType.Number,
                )
                Spacer(Modifier.height(10.dp))
                SettingsToggle(
                    label   = "Auto Connect on Start",
                    checked = autoConn,
                    onToggle = { autoConn = it },
                )
            }

            // ── Scan feedback section ─────────────────────────────────────────
            SettingsCard(title = "Scan Feedback") {
                SettingsToggle(
                    label    = "Beep on Successful Scan",
                    checked  = beep,
                    onToggle = { beep = it },
                )
                Spacer(Modifier.height(8.dp))
                SettingsToggle(
                    label    = "Vibrate on Successful Scan",
                    checked  = vibrate,
                    onToggle = { vibrate = it },
                )
                Spacer(Modifier.height(8.dp))
                SettingsToggle(
                    label    = "Auto Send After Single Scan (A4 / VEG)",
                    checked  = autoSend,
                    onToggle = { autoSend = it },
                )
            }

            // Protocol info card
            SettingsCard(title = "Protocol Info") {
                InfoRow("Format:", "BARCODES|TAG_TYPE|UNIT_TYPE|COPIES|TIMESTAMP")
                InfoRow("4PCS example:", "111,222,333,444|4PCS|PCS|2|1712345678")
                InfoRow("A4 example:",   "1234567890123|A4|CTN|1|1712345678")
                InfoRow("VEG example:",  "69051|VEG|KGS|3|1712345678")
                InfoRow("4PCS_SAME:",    "123,123,123,123|4PCS_SAME|PCS|1|1712345678")
            }

            // ── Save button ────────────────────────────────────────────────────
            Button(
                onClick = {
                    viewModel.save(
                        AppSettings(
                            serverIp          = ip.trim(),
                            serverPort        = port.toIntOrNull() ?: 5000,
                            autoConnect       = autoConn,
                            beepEnabled       = beep,
                            vibrateEnabled    = vibrate,
                            autoSendAfterScan = autoSend,
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
            ) {
                Text("Save Settings", color = TextOnPrimary,
                     fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BgSurface)
            .padding(16.dp),
    ) {
        Text(title, color = PrimaryTeal, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsTextField(
    label:       String,
    value:       String,
    onChange:    (String) -> Unit,
    hint:        String          = "",
    keyboardType: KeyboardType   = KeyboardType.Text,
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onChange,
        label         = { Text(label, color = TextSecondary, fontSize = 13.sp) },
        placeholder   = { Text(hint, color = TextSecondary.copy(alpha = 0.5f)) },
        singleLine    = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor     = TextPrimary,
            unfocusedTextColor   = TextPrimary,
            focusedBorderColor   = PrimaryTeal,
            unfocusedBorderColor = BorderColor,
            cursorColor          = PrimaryTeal,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SettingsToggle(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Switch(
            checked          = checked,
            onCheckedChange  = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor      = TextOnPrimary,
                checkedTrackColor      = PrimaryTeal,
                uncheckedThumbColor    = TextSecondary,
                uncheckedTrackColor    = BgInput,
            ),
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(110.dp))
        Text(value, color = TextPrimary,   fontSize = 12.sp, modifier = Modifier.weight(1f))
    }
}
