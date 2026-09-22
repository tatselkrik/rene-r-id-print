package com.idphoto.printing.image

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Normalized, non-destructive controls. Zero is neutral; temperature is cool to warm. */
data class PhotoAdjustments(
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val vibrance: Float = 0f,
    val saturation: Float = 0f,
    val temperature: Float = 0f,
) {
    init {
        require(listOf(brightness, contrast, vibrance, saturation, temperature).all {
            it.isFinite() && it in -1f..1f
        })
    }

    fun apply(argb: Int): Int {
        if (this == NEUTRAL || argb ushr 24 == 0) return argb
        var r = (argb ushr 16 and 255) / 255f
        var g = (argb ushr 8 and 255) / 255f
        var b = (argb and 255) / 255f
        val highest = max(r, max(g, b))
        val lowest = min(r, min(g, b))
        val chroma = if (highest > 0f) (highest - lowest) / highest else 0f
        // Vibrance favors muted colors and reduces its effect on warm skin-like hues.
        val skinProtection = if (r > g && g > b) 0.45f else 1f
        val colorGain = (1f + saturation) *
            (1f + vibrance * (1f - chroma) * skinProtection)
        val luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b
        r = luminance + (r - luminance) * colorGain
        g = luminance + (g - luminance) * colorGain
        b = luminance + (b - luminance) * colorGain
        r *= 1f + temperature * 0.18f
        b *= 1f - temperature * 0.18f
        val gain = 1f + contrast * 0.6f
        val offset = brightness * 0.25f
        fun channel(value: Float) = (((value - 0.5f) * gain + 0.5f + offset)
            .coerceIn(0f, 1f) * 255f).roundToInt()
        return (argb and -0x1000000) or (channel(r) shl 16) or
            (channel(g) shl 8) or channel(b)
    }

    companion object {
        val NEUTRAL = PhotoAdjustments()

        /** Conservative tone correction only; never guesses skin color or white balance. */
        fun automatic(samples: IntArray): PhotoAdjustments {
            val values = samples.asSequence().filter { it ushr 24 >= 230 }.map {
                (0.2126f * (it ushr 16 and 255) + 0.7152f * (it ushr 8 and 255) +
                    0.0722f * (it and 255)) / 255f
            }.filter { it in 0.04f..0.94f }.sorted().toList()
            if (values.size < 32) return NEUTRAL
            val median = values[values.size / 2]
            val spread = values[values.size * 9 / 10] - values[values.size / 10]
            return PhotoAdjustments(
                brightness = ((0.52f - median) / 0.25f).coerceIn(-0.24f, 0.24f),
                contrast = if (spread < 0.35f) 0.12f else 0f,
            )
        }
    }
}
