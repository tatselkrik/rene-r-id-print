package com.idphoto.printing.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.idphoto.printing.analysis.CheckStatus
import com.idphoto.printing.analysis.PhotoReview
import com.idphoto.printing.image.PhotoAdjustments

@Composable
fun ReviewScreen(
    bitmap: Bitmap?,
    review: PhotoReview?,
    isLoading: Boolean,
    whiteBackgroundEnabled: Boolean,
    whiteBackgroundAvailable: Boolean,
    onWhiteBackgroundChange: (Boolean) -> Unit,
    adjustments: PhotoAdjustments,
    autoAdjust: Boolean,
    onAdjustmentsChange: (PhotoAdjustments) -> Unit,
    onAutoChange: (Boolean) -> Unit,
    adjustmentsReady: Boolean,
    adjustmentError: String?,
    onRetake: () -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasCream)
            .safeDrawingPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = "Automatic Check",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Ink,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Pine)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Finding face and eye landmarks…",
                            color = Muted,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else if (review?.failureMessage != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        text = review.failureMessage,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center,
                    )
                }
            } else if (bitmap != null && review?.cropPlan != null) {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val photoHeight = minOf((maxWidth - 12.dp) / 2, 180.dp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(shape = RoundedCornerShape(8.dp)) {
                            PhotoCrop(bitmap, review.cropPlan.square, review.imageSize,
                                Modifier.height(photoHeight).aspectRatio(1f))
                        }
                        Surface(shape = RoundedCornerShape(8.dp)) {
                            PhotoCrop(bitmap, review.cropPlan.passport, review.imageSize,
                                Modifier.height(photoHeight).aspectRatio(35f / 45f))
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Background", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = whiteBackgroundEnabled && whiteBackgroundAvailable,
                            onCheckedChange = onWhiteBackgroundChange,
                            enabled = whiteBackgroundAvailable,
                            modifier = Modifier.semantics { contentDescription = "Background" },
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Auto", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = autoAdjust,
                            onCheckedChange = onAutoChange,
                            modifier = Modifier.semantics { contentDescription = "Auto" },
                        )
                    }
                }
                PhotoAdjustmentControls(adjustments, autoAdjust, onAdjustmentsChange)
                adjustmentError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
            review?.checks?.forEach { check ->
                val statusColor = when (check.status) {
                    CheckStatus.PASS -> Pine
                    CheckStatus.FAIL -> ErrorRed
                }
                val statusText = when (check.status) {
                    CheckStatus.PASS -> "✓"
                    CheckStatus.FAIL -> "!"
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(statusColor.copy(alpha = 0.12f), CircleShape)
                            .border(1.dp, statusColor.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(statusText, color = statusColor, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.size(12.dp))
                    Column {
                        Text(
                            text = check.label,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Ink,
                        )
                        Text(
                            text = check.detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = Muted,
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onRetake,
                modifier = Modifier.weight(1f),
            ) {
                Text("Retake")
            }
            Button(
                onClick = onContinue,
                enabled = review?.cropPlan != null && bitmap != null && !isLoading && adjustmentsReady,
                modifier = Modifier.weight(1f),
            ) {
                Text("Layout")
            }
        }
    }
}

