package com.idphoto.printing.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.idphoto.printing.image.PhotoAdjustments

@Composable
fun PhotoAdjustmentControls(
    settings: PhotoAdjustments,
    auto: Boolean,
    onChange: (PhotoAdjustments) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        AdjustmentSlider("Brightness", settings.brightness, !auto) { onChange(settings.copy(brightness = it)) }
        AdjustmentSlider("Contrast", settings.contrast, !auto) { onChange(settings.copy(contrast = it)) }
        AdjustmentSlider("Vibrance", settings.vibrance, !auto) { onChange(settings.copy(vibrance = it)) }
        AdjustmentSlider("Saturation", settings.saturation, !auto) { onChange(settings.copy(saturation = it)) }
        AdjustmentSlider("Temperature", settings.temperature, !auto) { onChange(settings.copy(temperature = it)) }
    }
}

@Composable
private fun AdjustmentSlider(label: String, value: Float, enabled: Boolean, onChange: (Float) -> Unit) {
    Text(label, style = MaterialTheme.typography.bodyMedium)
    Slider(
        value = value, onValueChange = onChange, valueRange = -1f..1f,
        enabled = enabled,
        modifier = Modifier.semantics { contentDescription = label },
    )
}
