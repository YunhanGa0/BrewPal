package com.omelan.cofi.components

import android.Manifest
import android.content.pm.PackageManager
import android.provider.MediaStore
import android.util.Size
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.omelan.cofi.R
import java.util.concurrent.Executors
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode

@OptIn(ExperimentalGetImage::class)
@Composable
fun QRCodeScannerDialog(
    onDismiss: () -> Unit,
    onPickImage: () -> Unit,
    onResult: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // 确保相机资源被释放
            cameraProvider?.unbindAll()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.import_recipe_scan)) },
        text = {
            Column {
                if (hasCameraPermission) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                    ) {
                        AndroidView(
                            factory = { context ->
                                PreviewView(context).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                                    scaleType = PreviewView.ScaleType.FILL_CENTER
                                }.also { previewView ->
                                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                                    cameraProviderFuture.addListener({
                                        cameraProvider = cameraProviderFuture.get()
                                        val preview = Preview.Builder()
                                            .build()
                                            .also {
                                                it.setSurfaceProvider(previewView.surfaceProvider)
                                            }

                                        val imageAnalysis = ImageAnalysis.Builder()
                                            .setTargetResolution(Size(1280, 720))
                                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                            .setImageQueueDepth(1)
                                            .build()
                                            .also {
                                                it.setAnalyzer(
                                                    ContextCompat.getMainExecutor(context)
                                                ) { imageProxy ->
                                                    try {
                                                        val mediaImage = imageProxy.image
                                                        if (mediaImage != null) {
                                                            val image = InputImage.fromMediaImage(
                                                                mediaImage,
                                                                imageProxy.imageInfo.rotationDegrees
                                                            )
                                                            
                                                            val options = BarcodeScannerOptions.Builder()
                                                                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                                                                .build()
                                                            
                                                            val scanner = BarcodeScanning.getClient(options)
                                                            
                                                            scanner.process(image)
                                                                .addOnSuccessListener { barcodes ->
                                                                    for (barcode in barcodes) {
                                                                        barcode.rawValue?.let { value ->
                                                                            onResult(value)
                                                                            onDismiss()
                                                                        }
                                                                    }
                                                                    imageProxy.close()
                                                                }
                                                                .addOnFailureListener { e ->
                                                                    e.printStackTrace()
                                                                    imageProxy.close()
                                                                }
                                                        } else {
                                                            imageProxy.close()
                                                        }
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                        imageProxy.close()
                                                    }
                                                }
                                            }

                                        try {
                                            cameraProvider?.let { provider ->
                                                provider.unbindAll()
                                                provider.bindToLifecycle(
                                                    lifecycleOwner,
                                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                                    preview,
                                                    imageAnalysis
                                                )
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }, ContextCompat.getMainExecutor(context))
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(stringResource(R.string.camera_permission_required))
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                        ) {
                            Text(stringResource(android.R.string.ok))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedButton(
                    onClick = onPickImage,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_image),
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(stringResource(R.string.import_recipe_pick_image))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}
