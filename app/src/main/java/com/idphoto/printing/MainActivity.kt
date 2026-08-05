package com.idphoto.printing

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.idphoto.printing.analysis.CheckStatus
import com.idphoto.printing.analysis.BlurDetector
import com.idphoto.printing.analysis.PhotoAnalyzer
import com.idphoto.printing.analysis.PhotoReview
import com.idphoto.printing.core.ImageSize
import com.idphoto.printing.image.BitmapLoader
import com.idphoto.printing.ui.CapturedPhoto
import com.idphoto.printing.ui.CaptureScreen
import com.idphoto.printing.ui.IdPhotoTheme
import com.idphoto.printing.ui.PrinterSetupScreen
import com.idphoto.printing.ui.ReviewScreen
import com.idphoto.printing.ui.SheetPreviewScreen
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IdPhotoTheme {
                IdPhotoWorkflow()
            }
        }
    }
}

private enum class WorkflowScreen {
    CAPTURE,
    REVIEW,
    PREVIEW,
    PRINTER_SETUP,
}

@Composable
private fun IdPhotoWorkflow() {
    val context = LocalContext.current
    val analyzer = remember { PhotoAnalyzer(context) }
    var screen by remember { mutableStateOf(WorkflowScreen.CAPTURE) }
    var capturedPhoto by remember { mutableStateOf<CapturedPhoto?>(null) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var review by remember { mutableStateOf<PhotoReview?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }

    DisposableEffect(analyzer) {
        onDispose {
            analyzer.close()
        }
    }

    DisposableEffect(bitmap) {
        val activeBitmap = bitmap
        onDispose {
            if (activeBitmap?.isRecycled == false) activeBitmap.recycle()
        }
    }

    LaunchedEffect(capturedPhoto) {
        val capture = capturedPhoto ?: return@LaunchedEffect
        val file = capture.file
        isAnalyzing = true
        review = null
        bitmap = null
        try {
            val analyzed = analyzer.analyze(Uri.fromFile(file), capture.cameraSquare)
            val loaded = withContext(Dispatchers.IO) { BitmapLoader.load(file) }
            bitmap = loaded
            review = analyzed.withDecodedBitmapChecks(loaded)
        } catch (error: Exception) {
            review = PhotoReview(
                imageSize = ImageSize(0, 0),
                cropPlan = null,
                checks = emptyList(),
                readyToPrint = false,
                failureMessage = error.message ?: "The captured photo could not be prepared.",
            )
        } finally {
            isAnalyzing = false
        }
    }

    fun retake() {
        bitmap?.recycle()
        bitmap = null
        review = null
        capturedPhoto?.file?.delete()
        capturedPhoto = null
        screen = WorkflowScreen.CAPTURE
    }

    BackHandler(enabled = screen != WorkflowScreen.CAPTURE) {
        when (screen) {
            WorkflowScreen.CAPTURE -> Unit
            WorkflowScreen.REVIEW -> retake()
            WorkflowScreen.PREVIEW -> screen = WorkflowScreen.REVIEW
            WorkflowScreen.PRINTER_SETUP -> screen = WorkflowScreen.CAPTURE
        }
    }

    when (screen) {
        WorkflowScreen.CAPTURE -> CaptureScreen(
            onCaptured = { capture ->
                capturedPhoto = capture
                screen = WorkflowScreen.REVIEW
            },
            onPrinterSetup = {
                screen = WorkflowScreen.PRINTER_SETUP
            },
        )
        WorkflowScreen.REVIEW -> ReviewScreen(
            bitmap = bitmap,
            review = review,
            isLoading = isAnalyzing,
            onRetake = ::retake,
            onContinue = { screen = WorkflowScreen.PREVIEW },
        )
        WorkflowScreen.PREVIEW -> SheetPreviewScreen(
            bitmap = requireNotNull(bitmap),
            review = requireNotNull(review),
            onBack = { screen = WorkflowScreen.REVIEW },
        )
        WorkflowScreen.PRINTER_SETUP -> PrinterSetupScreen(
            onBack = { screen = WorkflowScreen.CAPTURE },
        )
    }
}

private fun PhotoReview.withDecodedBitmapChecks(bitmap: Bitmap): PhotoReview {
    val plan = cropPlan ?: return this
    val scaleX = bitmap.width.toFloat() / imageSize.width.coerceAtLeast(1)
    val scaleY = bitmap.height.toFloat() / imageSize.height.coerceAtLeast(1)
    val decodedResolutionSufficient =
        plan.square.width * scaleX >= 600f &&
            plan.square.height * scaleY >= 600f &&
            plan.passport.width * scaleX >= 414f &&
            plan.passport.height * scaleY >= 532f
    val blur = BlurDetector.analyze(bitmap, plan.square, imageSize)

    return copy(
        checks = checks.map { check ->
            when (check.label) {
                "Print resolution" -> if (decodedResolutionSufficient) {
                    check
                } else {
                    check.copy(
                        status = CheckStatus.FAIL,
                        detail = "The working crop is below 300 dpi. Move closer and retake.",
                    )
                }
                "Blur" -> check.copy(
                    status = if (blur.isSharp) CheckStatus.PASS else CheckStatus.FAIL,
                    detail = if (blur.isSharp) {
                        "Sharpness check passed (score ${blur.score.roundToInt()})."
                    } else {
                        "The photo looks soft (score ${blur.score.roundToInt()}; minimum 45). Hold the phone steady, improve the light, and retake."
                    },
                )
                else -> check
            }
        },
        readyToPrint = readyToPrint && decodedResolutionSufficient && blur.isSharp,
    )
}
