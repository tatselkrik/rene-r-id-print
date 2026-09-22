package com.idphoto.printing.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.camera.view.TransformExperimental
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.idphoto.printing.core.CaptureGuideMapper
import com.idphoto.printing.core.ImageSize
import com.idphoto.printing.image.BitmapLoader
import com.idphoto.printing.core.FloatRect
import com.idphoto.printing.print.PrinterConnectionState
import java.io.File
import java.util.Locale

data class CapturedPhoto(
    val file: File,
    val cameraSquare: FloatRect,
)

@androidx.annotation.OptIn(markerClass = [TransformExperimental::class])
@Composable
fun CaptureScreen(
    printerState: PrinterConnectionState,
    onCaptured: (CapturedPhoto) -> Unit,
    onPrinterSetup: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var isCapturing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasPermission = granted }
    val controller = remember {
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            imageCaptureMode = ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    DisposableEffect(hasPermission, lifecycleOwner) {
        if (hasPermission) controller.bindToLifecycle(lifecycleOwner)
        onDispose { controller.unbind() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (hasPermission) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f),
            ) {
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { viewContext ->
                        PreviewView(viewContext).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            this.controller = controller
                            previewView = this
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
                FaceGuide(modifier = Modifier.fillMaxSize())
            }
        } else {
            PermissionPrompt(onRequest = {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            })
        }

        if (hasPermission) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                        ),
                    )
                    .background(Color.Black.copy(alpha = 0.54f))
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Frame the complete 2 × 2 photo inside the square.",
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Passport size keeps this height and trims only the left and right sides.",
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White.copy(alpha = 0.82f),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal,
                        ),
                    )
                    .background(Color.Black.copy(alpha = 0.62f))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                errorMessage?.let {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            text = it,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }

                Button(
                    onClick = {
                        errorMessage = null
                        val activePreview = previewView
                        val previewTransform = activePreview?.outputTransform
                        if (
                            activePreview == null ||
                            activePreview.width <= 0 ||
                            activePreview.height <= 0 ||
                            previewTransform == null
                        ) {
                            errorMessage =
                                "The camera is still starting. Wait a moment, then tap Take Photo again."
                            return@Button
                        }
                        isCapturing = true
                        val previewSize = ImageSize(activePreview.width, activePreview.height)
                        val guide = squareGuideRect(
                            width = activePreview.width.toFloat(),
                            height = activePreview.height.toFloat(),
                        )
                        val captureDir = File(context.cacheDir, "captures").apply { mkdirs() }
                        val photoFile = File(
                            captureDir,
                            String.format(Locale.US, "id-photo-%d.jpg", System.currentTimeMillis()),
                        )
                        val metadata = ImageCapture.Metadata().apply {
                            setReversedHorizontal(false)
                        }
                        val options = ImageCapture.OutputFileOptions.Builder(photoFile)
                            .setMetadata(metadata)
                            .build()
                        controller.takePicture(
                            options,
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    try {
                                        // LifecycleCameraController binds both use cases to PreviewView's
                                        // viewport. Saved JPEGs are cropped to it by CameraX; normalize
                                        // in upright coordinates rather than mixing raw buffer rotations.
                                        val mappedGuide = CaptureGuideMapper.map(
                                            previewSize, BitmapLoader.uprightSize(photoFile), guide,
                                        )
                                        isCapturing = false
                                        onCaptured(
                                            CapturedPhoto(
                                                file = photoFile,
                                                cameraSquare = mappedGuide,
                                            ),
                                        )
                                    } catch (error: Exception) {
                                        photoFile.delete()
                                        isCapturing = false
                                        errorMessage = error.message
                                            ?: "The square guide could not be matched to the saved photo."
                                    }
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    isCapturing = false
                                    errorMessage = exception.message ?: "The camera could not save the photo."
                                }
                            },
                        )
                    },
                    enabled = !isCapturing,
                    modifier = Modifier.size(width = 168.dp, height = 56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = PineDark,
                    ),
                ) {
                    if (isCapturing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = Pine,
                        )
                    } else {
                        Text("Take Photo", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = printerState.message,
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = onPrinterSetup,
                    enabled = !isCapturing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.78f)),
                ) {
                    Text("Printer Setup", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun FaceGuide(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val source = squareGuideRect(size.width, size.height)
        val guide = Rect(source.left, source.top, source.right, source.bottom)
        val shade = Color.Black.copy(alpha = 0.34f)
        drawRect(
            color = shade,
            size = androidx.compose.ui.geometry.Size(size.width, guide.top),
        )
        drawRect(
            color = shade,
            topLeft = Offset(0f, guide.bottom),
            size = androidx.compose.ui.geometry.Size(size.width, size.height - guide.bottom),
        )
        drawRect(
            color = shade,
            topLeft = Offset(0f, guide.top),
            size = androidx.compose.ui.geometry.Size(guide.left, guide.height),
        )
        drawRect(
            color = shade,
            topLeft = Offset(guide.right, guide.top),
            size = androidx.compose.ui.geometry.Size(size.width - guide.right, guide.height),
        )
        drawRect(
            color = Color.White.copy(alpha = 0.92f),
            topLeft = guide.topLeft,
            size = guide.size,
            style = Stroke(width = 4.dp.toPx()),
        )
        val eyeY = guide.top + guide.height * 0.40f
        drawLine(
            color = Color.White.copy(alpha = 0.65f),
            start = Offset(guide.left + guide.width * 0.18f, eyeY),
            end = Offset(guide.right - guide.width * 0.18f, eyeY),
            strokeWidth = 2.dp.toPx(),
        )
    }
}

private fun squareGuideRect(width: Float, height: Float): FloatRect {
    val side = width * 0.72f
    val left = (width - side) / 2f
    val top = (height - side) / 2f
    return FloatRect(left, top, left + side, top + side)
}

@Composable
private fun PermissionPrompt(onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Camera access is needed to take an ID photo.",
            color = Color.White,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onRequest) { Text("Allow Camera") }
    }
}
