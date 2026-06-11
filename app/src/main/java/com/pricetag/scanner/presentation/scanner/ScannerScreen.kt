package com.pricetag.scanner.presentation.scanner

import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.pricetag.scanner.presentation.theme.*
import com.pricetag.scanner.utils.BarcodeValidator
import java.util.concurrent.Executors

/**
 * Full-screen CameraX barcode scanner.
 *
 * @param onBarcodeScanned  Called (on main thread) when a valid barcode is detected.
 * @param onDismiss         Called when user closes the scanner.
 * @param beepEnabled       Whether to trigger a beep on successful scan (handled by ViewModel).
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScannerScreen(
    onBarcodeScanned: (String) -> Unit,
    onDismiss:        () -> Unit,
) {
    val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (cameraPermission.status.isGranted) {
            CameraPreview(
                onBarcodeScanned = onBarcodeScanned,
                modifier         = Modifier.fillMaxSize(),
            )
        } else {
            // Permission denied — show explanation
            Column(
                modifier              = Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement   = Arrangement.Center,
                horizontalAlignment   = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Camera permission is required to scan barcodes.",
                    color = TextPrimary,
                    fontSize = 16.sp,
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { cameraPermission.launchPermissionRequest() },
                    colors  = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                ) {
                    Text("Grant Permission", color = TextOnPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ── Scan overlay ───────────────────────────────────────────────────────
        ScanOverlay(modifier = Modifier.fillMaxSize())

        // ── Close button ───────────────────────────────────────────────────────
        IconButton(
            onClick  = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(48.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close scanner",
                tint = Color.White, modifier = Modifier.size(28.dp))
        }

        // ── Label ──────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 24.dp, vertical = 10.dp),
        ) {
            Text(
                text       = "Point camera at barcode",
                color      = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize   = 16.sp,
            )
        }
    }
}

@Composable
private fun CameraPreview(
    onBarcodeScanned: (String) -> Unit,
    modifier:         Modifier = Modifier,
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor       = remember { Executors.newSingleThreadExecutor() }

    val options = remember {
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_ALL_FORMATS
            )
            .build()
    }
    val scanner = remember { BarcodeScanning.getClient(options) }

    var torchEnabled by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            scanner.close()
            executor.shutdown()
            BarcodeValidator.clear()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory  = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val provider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setTargetRotation(previewView.display?.rotation ?: 0)
                        .build()

                    imageAnalysis.setAnalyzer(executor) { imageProxy ->
                        processImage(imageProxy, scanner, onBarcodeScanned)
                    }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        provider.unbindAll()
                        val camera = provider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis,
                        )
                        cameraControl = camera.cameraControl
                    } catch (e: Exception) {
                        Log.e("ScannerScreen", "Camera bind failed: ${e.message}")
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
        )

        // Torch toggle button
        IconButton(
            onClick  = {
                torchEnabled = !torchEnabled
                cameraControl?.enableTorch(torchEnabled)
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .size(48.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
        ) {
            Icon(
                imageVector = if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                contentDescription = "Toggle torch",
                tint = if (torchEnabled) WarningAmber else Color.White,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

@Composable
private fun ScanOverlay(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(260.dp, 160.dp)
                .border(
                    width = 3.dp,
                    color = PrimaryTeal,
                    shape = RoundedCornerShape(12.dp),
                )
        )
    }
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun processImage(
    imageProxy:       ImageProxy,
    scanner:          com.google.mlkit.vision.barcode.BarcodeScanner,
    onBarcodeScanned: (String) -> Unit,
) {
    val mediaImage = imageProxy.image
    if (mediaImage == null) {
        imageProxy.close()
        return
    }
    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
    scanner.process(image)
        .addOnSuccessListener { barcodes ->
            for (barcode in barcodes) {
                val raw = barcode.rawValue ?: continue
                if (raw.isBlank()) continue
                if (!BarcodeValidator.shouldProcess(raw)) continue   // duplicate suppression
                onBarcodeScanned(raw)
                break   // only emit first barcode per frame
            }
        }
        .addOnFailureListener { Log.e("ScannerScreen", "Scan failed: ${it.message}") }
        .addOnCompleteListener { imageProxy.close() }
}
