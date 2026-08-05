package com.idphoto.printing.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.idphoto.printing.analysis.CheckStatus
import com.idphoto.printing.analysis.PhotoReview

@Composable
fun ReviewScreen(
    bitmap: Bitmap?,
    review: PhotoReview?,
    isLoading: Boolean,
    onRetake: () -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasCream)
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Text(
            text = "Automatic Check",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Ink,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "The original photo is checked before either crop is created.",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            color = Muted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(18.dp))

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CropPreviewCard(
                        label = "Square Crop",
                        modifier = Modifier.weight(1f),
                    ) {
                        PhotoCrop(
                            bitmap = bitmap,
                            crop = review.cropPlan.square,
                            originalSize = review.imageSize,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f),
                        )
                    }
                    CropPreviewCard(
                        label = "Passport Size (35 × 45 mm)",
                        modifier = Modifier.weight(1f),
                    ) {
                        PhotoCrop(
                            bitmap = bitmap,
                            crop = review.cropPlan.passport,
                            originalSize = review.imageSize,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(35f / 45f),
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
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
                enabled = review?.cropPlan != null && bitmap != null && !isLoading,
                modifier = Modifier.weight(1f),
            ) {
                Text("Preview")
            }
        }
    }
}

@Composable
private fun CropPreviewCard(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 2.dp,
        ) {
            content()
        }
        Text(
            text = label,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = Muted,
            textAlign = TextAlign.Center,
        )
    }
}
