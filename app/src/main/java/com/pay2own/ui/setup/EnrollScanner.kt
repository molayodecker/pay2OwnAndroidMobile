package com.pay2own.ui.setup

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.pay2own.Pay2OwnDeviceAdminReceiver
import com.pay2own.R
import com.pay2own.domain.EnrollLink
import com.pay2own.domain.parseEnrollLink
import com.pay2own.ui.components.SecondaryButton
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun EnrollScanner(
    padding: PaddingValues,
    onLink: (EnrollLink) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    var allowed by remember { mutableStateOf(hasCamera(context)) }
    var rejected by remember { mutableStateOf(false) }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        allowed = it
    }
    LaunchedEffect(Unit) {
        grantCameraIfDeviceOwner(context)
        if (hasCamera(context)) allowed = true else request.launch(Manifest.permission.CAMERA)
    }
    val done = remember { AtomicBoolean(false) }
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.setup_scan_body), style = MaterialTheme.typography.bodyLarge)
        if (allowed) {
            QrCamera(
                modifier = Modifier.fillMaxWidth().height(320.dp).clip(RoundedCornerShape(16.dp)),
                onRaw = { raw ->
                    if (done.get()) return@QrCamera
                    val link = parseEnrollLink(raw)
                    if (link == null) {
                        rejected = true
                    } else if (done.compareAndSet(false, true)) {
                        onLink(link)
                    }
                },
            )
            if (rejected) {
                Text(
                    stringResource(R.string.setup_scan_wrong),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        } else {
            Text(stringResource(R.string.setup_scan_permission), style = MaterialTheme.typography.bodyLarge)
            SecondaryButton(stringResource(R.string.setup_scan_allow)) {
                request.launch(Manifest.permission.CAMERA)
            }
        }
        SecondaryButton(stringResource(R.string.setup_scan_close), onClose)
    }
}

@Composable
private fun QrCamera(
    modifier: Modifier,
    onRaw: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    DisposableEffect(lifecycleOwner) {
        val executor = Executors.newSingleThreadExecutor()
        val scanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build(),
        )
        val future = ProcessCameraProvider.getInstance(context)
        val mainExecutor = ContextCompat.getMainExecutor(context)
        future.addListener({
            val provider = future.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(executor, QrAnalyzer(scanner) { raw ->
                mainExecutor.execute { onRaw(raw) }
            })
            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis,
            )
        }, mainExecutor)
        onDispose {
            if (future.isDone) future.get().unbindAll()
            executor.shutdown()
            scanner.close()
        }
    }
    AndroidView(factory = { previewView }, modifier = modifier)
}

private class QrAnalyzer(
    private val scanner: BarcodeScanner,
    private val onRaw: (String) -> Unit,
) : ImageAnalysis.Analyzer {
    override fun analyze(image: ImageProxy) {
        val media = image.image
        if (media == null) {
            image.close()
            return
        }
        val input = InputImage.fromMediaImage(media, image.imageInfo.rotationDegrees)
        scanner.process(input)
            .addOnSuccessListener { codes ->
                codes.firstNotNullOfOrNull { it.rawValue }?.let(onRaw)
            }
            .addOnCompleteListener { image.close() }
    }
}

private fun hasCamera(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

private fun grantCameraIfDeviceOwner(context: Context) {
    val manager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    if (!manager.isDeviceOwnerApp(context.packageName)) return
    val admin = android.content.ComponentName(context, Pay2OwnDeviceAdminReceiver::class.java)
    try {
        manager.setPermissionGrantState(
            admin,
            context.packageName,
            Manifest.permission.CAMERA,
            DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED,
        )
    } catch (_: SecurityException) {
    }
}
