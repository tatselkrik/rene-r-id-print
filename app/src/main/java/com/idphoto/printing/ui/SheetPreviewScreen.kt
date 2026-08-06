package com.idphoto.printing.ui

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.idphoto.printing.analysis.PhotoReview
import com.idphoto.printing.core.PhotoKind
import com.idphoto.printing.core.PrintScale
import com.idphoto.printing.core.SheetCombination
import com.idphoto.printing.core.SheetLayout
import com.idphoto.printing.print.DirectIppPrinter
import com.idphoto.printing.print.DirectPrinterSettings
import com.idphoto.printing.print.PhotoColorTone
import com.idphoto.printing.print.SheetJpegActions
import com.idphoto.printing.print.SheetJpegGenerator
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SheetPreviewScreen(
    bitmap: Bitmap,
    review: PhotoReview,
    combination: SheetCombination,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val directPrinter = remember {
        DirectPrinterSettings.load(context)?.takeIf { it.readyForDirectPrint }
    }
    var pendingSaveFile by remember(bitmap, review, combination) { mutableStateOf<File?>(null) }
    var isExporting by remember { mutableStateOf(false) }
    var exportMessage by remember { mutableStateOf<String?>(null) }
    var exportFailed by remember { mutableStateOf(false) }
    var isPrinting by remember { mutableStateOf(false) }
    var printMessage by remember { mutableStateOf<String?>(null) }
    var printFailed by remember { mutableStateOf(false) }
    val isBusy = isExporting || isPrinting

    suspend fun prepareJpeg(
        fileName: String = "id-photo-5x7-share.jpg",
        printScale: PrintScale = PrintScale.IDENTITY,
        photoColorTone: PhotoColorTone = PhotoColorTone.NEUTRAL,
    ): File = withContext(Dispatchers.IO) {
        SheetJpegGenerator.generate(
            outputFile = File(context.cacheDir, "print/$fileName"),
            bitmap = bitmap,
            review = review,
            combination = combination,
            printScale = printScale,
            photoColorTone = photoColorTone,
        )
    }

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/jpeg"),
    ) { destination ->
        val source = pendingSaveFile
        pendingSaveFile = null
        if (destination != null && source != null) {
            scope.launch {
                isExporting = true
                try {
                    withContext(Dispatchers.IO) {
                        SheetJpegActions.save(context, source, destination)
                    }
                    exportFailed = false
                    exportMessage = "JPEG saved."
                } catch (error: Exception) {
                    exportFailed = true
                    exportMessage = error.message ?: "The JPEG could not be saved."
                } finally {
                    isExporting = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasCream)
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Text(
            text = "5 × 7 Print Preview",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Ink,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Portrait sheet • exact proportions • cut boundaries shown",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            color = Muted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(14.dp))

        SheetPreview(
            bitmap = bitmap,
            review = review,
            combination = combination,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            if (combination.largeCount > 0) {
                CountPill(combination.largeCount.toString(), "2 × 2")
            }
            if (combination.largeCount > 0 && combination.passportCount > 0) {
                Spacer(Modifier.size(8.dp))
            }
            if (combination.passportCount > 0) {
                CountPill(combination.passportCount.toString(), "Passport")
            }
            if (
                combination.smallCount > 0 &&
                (combination.largeCount > 0 || combination.passportCount > 0)
            ) {
                Spacer(Modifier.size(8.dp))
            }
            if (combination.smallCount > 0) {
                CountPill(combination.smallCount.toString(), "1 × 1")
            }
        }

        exportMessage?.let {
            Text(
                text = it,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                color = if (exportFailed) MaterialTheme.colorScheme.error else Pine,
                textAlign = TextAlign.Center,
            )
        }
        printMessage?.let {
            Text(
                text = it,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                color = if (printFailed) MaterialTheme.colorScheme.error else Pine,
                textAlign = TextAlign.Center,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = {
                    scope.launch {
                        isExporting = true
                        exportMessage = null
                        try {
                            pendingSaveFile = prepareJpeg()
                            val timestamp = SimpleDateFormat(
                                "yyyyMMdd-HHmmss",
                                Locale.US,
                            ).format(Date())
                            saveLauncher.launch("ID-photo-5x7-$timestamp.jpg")
                        } catch (error: Exception) {
                            pendingSaveFile = null
                            exportFailed = true
                            exportMessage = error.message ?: "The JPEG could not be prepared."
                        } finally {
                            isExporting = false
                        }
                    }
                },
                enabled = !isBusy && review.cropPlan != null,
                modifier = Modifier.weight(1f),
            ) {
                Text(if (isExporting) "Preparing…" else "Save")
            }
            OutlinedButton(
                onClick = {
                    scope.launch {
                        isExporting = true
                        exportMessage = null
                        try {
                            val jpeg = prepareJpeg()
                            SheetJpegActions.share(context, jpeg)
                            exportFailed = false
                            exportMessage = "Sharing options opened."
                        } catch (error: Exception) {
                            exportFailed = true
                            exportMessage = error.message ?: "The JPEG could not be shared."
                        } finally {
                            isExporting = false
                        }
                    }
                },
                enabled = !isBusy && review.cropPlan != null,
                modifier = Modifier.weight(1f),
            ) {
                Text("Share")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onBack,
                enabled = !isBusy,
                modifier = Modifier.weight(1f),
            ) {
                Text("Back")
            }
            Button(
                onClick = {
                    val printer = directPrinter
                    if (printer == null) {
                        printFailed = true
                        printMessage = "No direct printer is ready. Return to the camera and open Printer Setup."
                        return@Button
                    }
                    scope.launch {
                        isPrinting = true
                        printMessage = null
                        try {
                            val result = withContext(Dispatchers.IO) {
                                val jpeg = prepareJpeg(
                                    fileName = "id-photo-5x7-direct.jpg",
                                    printScale = PrintScale.L15150_RC_WOVEN_MATTE,
                                    photoColorTone = PhotoColorTone.DIRECT_PRINT_WARM,
                                )
                                DirectIppPrinter.printJpeg(
                                    profile = printer,
                                    jpeg = jpeg,
                                    jobName = "ID photo 5x7 sheet",
                                )
                            }
                            printFailed = false
                            printMessage = result.message
                        } catch (error: Exception) {
                            printFailed = true
                            printMessage = error.message ?: "Direct printing failed."
                        } finally {
                            isPrinting = false
                        }
                    }
                },
                enabled = review.readyToPrint && !isBusy,
                modifier = Modifier.weight(1f),
            ) {
                Text(if (isPrinting) "Printing…" else "Print")
            }
        }
    }
}

@Composable
fun SheetPreview(
    bitmap: Bitmap,
    review: PhotoReview,
    combination: SheetCombination,
    modifier: Modifier = Modifier,
) {
    val cropPlan = requireNotNull(review.cropPlan)
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        val paperWidth = minOf(maxWidth, maxHeight * (5f / 7f))
        val paperHeight = paperWidth * (7f / 5f)
        Box(
            modifier = Modifier
                .size(paperWidth, paperHeight)
                .shadow(7.dp, RoundedCornerShape(2.dp))
                .background(Color.White),
        ) {
            val guideLength = paperWidth.mm(
                SheetLayout.CUT_GUIDE_LENGTH_MM,
                SheetLayout.PAPER_WIDTH_MM,
            )
            val guideStrokeWidth = maxOf(
                1.dp,
                paperWidth.mm(
                    SheetLayout.CUT_GUIDE_STROKE_MM,
                    SheetLayout.PAPER_WIDTH_MM,
                ),
            )
            SheetLayout.cellsFor(combination).forEach { cell ->
                val crop = when (cell.kind) {
                    PhotoKind.LARGE_SQUARE,
                    PhotoKind.SMALL_SQUARE -> cropPlan.square
                    PhotoKind.PASSPORT_35X45 -> cropPlan.passport
                }
                Box(
                    modifier = Modifier
                        .offset(
                            x = paperWidth.mm(cell.bounds.left, SheetLayout.PAPER_WIDTH_MM),
                            y = paperHeight.mm(cell.bounds.top, SheetLayout.PAPER_HEIGHT_MM),
                        )
                        .size(
                            width = paperWidth.mm(cell.bounds.width, SheetLayout.PAPER_WIDTH_MM),
                            height = paperHeight.mm(cell.bounds.height, SheetLayout.PAPER_HEIGHT_MM),
                        ),
                ) {
                    PhotoCrop(
                        bitmap = bitmap,
                        crop = crop,
                        originalSize = review.imageSize,
                        modifier = Modifier.fillMaxSize(),
                    )
                    CornerCutGuides(
                        length = guideLength,
                        strokeWidth = guideStrokeWidth,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

private fun Dp.mm(value: Float, totalMm: Float): Dp = this * (value / totalMm)

@Composable
private fun CornerCutGuides(
    length: Dp,
    strokeWidth: Dp,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val stroke = strokeWidth.toPx()
        val inset = stroke / 2f
        val guide = length.toPx().coerceAtMost(minOf(size.width, size.height) / 3f)
        val left = inset
        val top = inset
        val right = size.width - inset
        val bottom = size.height - inset

        drawLine(Color.Black, Offset(left, top), Offset(left + guide, top), stroke)
        drawLine(Color.Black, Offset(left, top), Offset(left, top + guide), stroke)
        drawLine(Color.Black, Offset(right, top), Offset(right - guide, top), stroke)
        drawLine(Color.Black, Offset(right, top), Offset(right, top + guide), stroke)
        drawLine(Color.Black, Offset(left, bottom), Offset(left + guide, bottom), stroke)
        drawLine(Color.Black, Offset(left, bottom), Offset(left, bottom - guide), stroke)
        drawLine(Color.Black, Offset(right, bottom), Offset(right - guide, bottom), stroke)
        drawLine(Color.Black, Offset(right, bottom), Offset(right, bottom - guide), stroke)
    }
}

@Composable
private fun CountPill(count: String, label: String) {
    Surface(
        color = Mint,
        shape = RoundedCornerShape(50),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(count, color = PineDark, fontWeight = FontWeight.Bold)
            Spacer(Modifier.size(4.dp))
            Text(label, color = PineDark, style = MaterialTheme.typography.labelSmall)
        }
    }
}
