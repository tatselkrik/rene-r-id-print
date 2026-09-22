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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.idphoto.printing.print.PrinterConnection
import com.idphoto.printing.print.watchWifi
import com.idphoto.printing.analysis.CheckStatus
import com.idphoto.printing.analysis.BlurDetector
import com.idphoto.printing.analysis.PhotoAnalyzer
import com.idphoto.printing.analysis.PhotoReview
import com.idphoto.printing.core.ImageSize
import com.idphoto.printing.core.SheetCombination
import com.idphoto.printing.image.BackgroundWhitener
import com.idphoto.printing.image.BitmapLoader
import com.idphoto.printing.image.PhotoAdjustments
import com.idphoto.printing.image.PhotoAdjustmentProcessor
import com.idphoto.printing.ui.CapturedPhoto
import com.idphoto.printing.ui.CaptureScreen
import com.idphoto.printing.ui.CombinationScreen
import com.idphoto.printing.ui.IdPhotoTheme
import com.idphoto.printing.ui.PrinterSetupScreen
import com.idphoto.printing.ui.ReviewScreen
import com.idphoto.printing.ui.SheetPreviewScreen
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

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
    COMBINATION,
    PREVIEW,
    PRINTER_SETUP,
}

@Composable
private fun IdPhotoWorkflow() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val printerConnection = remember { PrinterConnection.create(context) }
    val printerState by printerConnection.state.collectAsStateWithLifecycle()
    LaunchedEffect(lifecycleOwner, printerConnection) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            printerConnection.watchWifi(context)
        }
    }
    val analyzer = remember { PhotoAnalyzer(context) }
    val backgroundWhitener = remember { BackgroundWhitener() }
    var screen by remember { mutableStateOf(WorkflowScreen.CAPTURE) }
    var capturedPhoto by remember { mutableStateOf<CapturedPhoto?>(null) }
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var whiteBackgroundBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var whiteBackgroundEnabled by remember { mutableStateOf(true) }
    var review by remember { mutableStateOf<PhotoReview?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var selectedCombination by remember { mutableStateOf(SheetCombination.DEFAULT) }
    var adjustments by remember { mutableStateOf(PhotoAdjustments.NEUTRAL) }
    var autoAdjust by remember { mutableStateOf(false) }
    var automaticAdjustments by remember { mutableStateOf(PhotoAdjustments.NEUTRAL) }
    var automaticSource by remember { mutableStateOf<Bitmap?>(null) }
    var adjustedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var appliedSource by remember { mutableStateOf<Bitmap?>(null) }
    var appliedSettings by remember { mutableStateOf<PhotoAdjustments?>(null) }
    var adjustmentError by remember { mutableStateOf<String?>(null) }

    val sourceBitmap = if (whiteBackgroundEnabled) {
        whiteBackgroundBitmap ?: originalBitmap
    } else {
        originalBitmap
    }
    val effectiveAdjustments = if (autoAdjust) automaticAdjustments else adjustments
    val adjustmentsReady = (!autoAdjust || automaticSource === sourceBitmap) &&
        (effectiveAdjustments == PhotoAdjustments.NEUTRAL ||
            (appliedSource === sourceBitmap && appliedSettings == effectiveAdjustments))
    val activeBitmap = if (effectiveAdjustments == PhotoAdjustments.NEUTRAL) sourceBitmap
        else if (appliedSource === sourceBitmap) adjustedBitmap ?: sourceBitmap else sourceBitmap

    LaunchedEffect(sourceBitmap, review?.cropPlan) {
        automaticAdjustments = PhotoAdjustments.NEUTRAL
        automaticSource = null
        val source = sourceBitmap ?: return@LaunchedEffect
        val currentReview = review ?: return@LaunchedEffect
        val crop = currentReview.cropPlan?.square ?: return@LaunchedEffect
        automaticAdjustments = withContext(Dispatchers.Default) {
            PhotoAdjustmentProcessor.automatic(source, crop, currentReview.imageSize)
        }
        automaticSource = source
    }
    LaunchedEffect(sourceBitmap, effectiveAdjustments) {
        adjustmentError = null
        val source = sourceBitmap ?: return@LaunchedEffect
        if (effectiveAdjustments == PhotoAdjustments.NEUTRAL) {
            adjustedBitmap = null
            appliedSource = null
            appliedSettings = null
            return@LaunchedEffect
        }
        delay(80) // Coalesce rapid slider updates; only a complete result becomes exportable.
        try {
            val result = withContext(Dispatchers.Default) {
                PhotoAdjustmentProcessor.apply(source, effectiveAdjustments)
            }
            adjustedBitmap = result
            appliedSource = source
            appliedSettings = effectiveAdjustments
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            adjustmentError = "Adjustments could not be applied. Try Reset or retake."
        }
    }

    DisposableEffect(analyzer, backgroundWhitener) {
        onDispose {
            analyzer.close()
            backgroundWhitener.close()
        }
    }

    // Let Android reclaim displayed bitmaps once the renderer and cancellable workers
    // release them. Explicit recycling here races background adjustment processing.

    LaunchedEffect(capturedPhoto) {
        val capture = capturedPhoto ?: return@LaunchedEffect
        val file = capture.file
        isAnalyzing = true
        review = null
        originalBitmap = null
        whiteBackgroundBitmap = null
        whiteBackgroundEnabled = true
        adjustments = PhotoAdjustments.NEUTRAL
        autoAdjust = false
        adjustedBitmap = null
        try {
            val analyzed = analyzer.analyze(Uri.fromFile(file), capture.cameraSquare)
            val loaded = withContext(Dispatchers.IO) { BitmapLoader.load(file) }
            originalBitmap = loaded
            val checkedReview = analyzed.withDecodedBitmapChecks(loaded)
            review = checkedReview
            if (checkedReview.cropPlan != null) {
                whiteBackgroundBitmap = backgroundWhitener.whiten(loaded)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            if (originalBitmap == null || review == null) {
                review = PhotoReview(
                    imageSize = ImageSize(0, 0),
                    cropPlan = null,
                    checks = emptyList(),
                    readyToPrint = false,
                    failureMessage = error.message ?: "The captured photo could not be prepared.",
                )
            }
        } finally {
            isAnalyzing = false
        }
    }

    fun retake() {
        originalBitmap = null
        whiteBackgroundBitmap = null
        adjustedBitmap = null
        appliedSource = null
        appliedSettings = null
        adjustments = PhotoAdjustments.NEUTRAL
        autoAdjust = false
        whiteBackgroundEnabled = true
        review = null
        capturedPhoto?.file?.delete()
        capturedPhoto = null
        screen = WorkflowScreen.CAPTURE
    }

    BackHandler(enabled = screen != WorkflowScreen.CAPTURE) {
        when (screen) {
            WorkflowScreen.CAPTURE -> Unit
            WorkflowScreen.REVIEW -> retake()
            WorkflowScreen.COMBINATION -> screen = WorkflowScreen.REVIEW
            WorkflowScreen.PREVIEW -> screen = WorkflowScreen.COMBINATION
            WorkflowScreen.PRINTER_SETUP -> screen = WorkflowScreen.CAPTURE
        }
    }

    when (screen) {
        WorkflowScreen.CAPTURE -> CaptureScreen(
            printerState = printerState,
            onCaptured = { capture ->
                capturedPhoto = capture
                screen = WorkflowScreen.REVIEW
            },
            onPrinterSetup = {
                screen = WorkflowScreen.PRINTER_SETUP
            },
        )
        WorkflowScreen.REVIEW -> ReviewScreen(
            bitmap = activeBitmap,
            review = review,
            isLoading = isAnalyzing,
            whiteBackgroundEnabled = whiteBackgroundEnabled,
            whiteBackgroundAvailable = whiteBackgroundBitmap != null,
            onWhiteBackgroundChange = { whiteBackgroundEnabled = it },
            adjustments = effectiveAdjustments,
            autoAdjust = autoAdjust,
            onAdjustmentsChange = { adjustments = it },
            onAutoChange = { adjustments = PhotoAdjustments.NEUTRAL; autoAdjust = it },
            adjustmentsReady = adjustmentsReady,
            adjustmentError = adjustmentError,
            onRetake = ::retake,
            onContinue = { screen = WorkflowScreen.COMBINATION },
        )
        WorkflowScreen.COMBINATION -> CombinationScreen(
            selectedCombination = selectedCombination,
            onCombinationSelected = { selectedCombination = it },
            onBack = { screen = WorkflowScreen.REVIEW },
            onPreview = { screen = WorkflowScreen.PREVIEW },
        )
        WorkflowScreen.PREVIEW -> SheetPreviewScreen(
            printerConnection = printerConnection,
            printerState = printerState,
            bitmap = requireNotNull(activeBitmap),
            review = requireNotNull(review),
            combination = selectedCombination,
            onBack = { screen = WorkflowScreen.COMBINATION },
        )
        WorkflowScreen.PRINTER_SETUP -> PrinterSetupScreen(
            printerConnection = printerConnection,
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
