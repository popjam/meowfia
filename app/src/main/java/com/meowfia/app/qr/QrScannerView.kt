package com.meowfia.app.qr

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.meowfia.app.data.model.RoleId
import java.util.concurrent.Executors

/**
 * Compose wrapper around CameraX preview + ML Kit barcode scanner.
 * Scans QR codes continuously and reports recognized Meowfia role cards.
 */
@Composable
fun QrScannerView(
    onCardScanned: (RoleId) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scanner = remember { QrScanner() }
    val scannedCards = remember { mutableSetOf<RoleId>() }
    val executor = remember { Executors.newSingleThreadExecutor() }

    val previewView = remember { PreviewView(context) }

    DisposableEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { imageAnalysis ->
                        imageAnalysis.setAnalyzer(executor) { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val inputImage = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )
                                val barcodeScanner = BarcodeScanning.getClient()
                                barcodeScanner.process(inputImage)
                                    .addOnSuccessListener { barcodes ->
                                        for (barcode in barcodes) {
                                            if (barcode.format == Barcode.FORMAT_QR_CODE) {
                                                barcode.rawValue?.let { raw ->
                                                    val roleId = scanner.parseQrContent(raw)
                                                    if (roleId != null && roleId !in scannedCards) {
                                                        scannedCards.add(roleId)
                                                        onCardScanned(roleId)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            } else {
                                imageProxy.close()
                            }
                        }
                    }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
            } catch (e: Exception) {
                Log.e("QrScannerView", "Camera setup failed", e)
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            try {
                val cameraProvider = cameraProviderFuture.get()
                cameraProvider.unbindAll()
            } catch (_: Exception) {}
            executor.shutdown()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier.fillMaxSize()
    )
}
