package com.idphoto.printing.image

import android.graphics.Bitmap
import android.graphics.Color
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.SegmentationMask
import com.google.mlkit.vision.segmentation.Segmenter
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import java.io.Closeable
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * Gently replaces only the detected background with white and lifts shadows on
 * the detected person. The confidence mask remains soft around hair and shoulders
 * so the result does not look cut out.
 */
class BackgroundWhitener : Closeable {
    private val segmenter: Segmenter = Segmentation.getClient(
        SelfieSegmenterOptions.Builder()
            .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
            .enableRawSizeMask()
            .build(),
    )

    suspend fun whiten(bitmap: Bitmap): Bitmap {
        val mask = segmenter.processAwait(InputImage.fromBitmap(bitmap, 0))
        return withContext(Dispatchers.Default) {
            applyMask(bitmap, mask)
        }
    }

    override fun close() {
        segmenter.close()
    }

    private fun applyMask(source: Bitmap, mask: SegmentationMask): Bitmap {
        require(mask.width > 0 && mask.height > 0) { "The background mask was empty." }

        val maskBuffer = mask.buffer
        maskBuffer.rewind()
        val confidence = FloatArray(mask.width * mask.height)
        require(maskBuffer.remaining() >= confidence.size * Float.SIZE_BYTES) {
            "The background mask was incomplete."
        }
        for (index in confidence.indices) {
            confidence[index] = maskBuffer.float.coerceIn(0f, 1f)
        }

        val output = requireNotNull(source.copy(Bitmap.Config.ARGB_8888, true)) {
            "The white-background photo could not be created."
        }
        val row = IntArray(source.width)
        val maskLeft = IntArray(source.width)
        val maskRight = IntArray(source.width)
        val horizontalFraction = FloatArray(source.width)

        for (x in 0 until source.width) {
            val maskX = ((x + 0.5f) * mask.width / source.width - 0.5f)
                .coerceIn(0f, (mask.width - 1).toFloat())
            val left = floor(maskX).toInt()
            maskLeft[x] = left
            maskRight[x] = (left + 1).coerceAtMost(mask.width - 1)
            horizontalFraction[x] = maskX - left
        }

        for (y in 0 until source.height) {
            source.getPixels(row, 0, source.width, 0, y, source.width, 1)
            val maskY = ((y + 0.5f) * mask.height / source.height - 0.5f)
                .coerceIn(0f, (mask.height - 1).toFloat())
            val top = floor(maskY).toInt()
            val bottom = (top + 1).coerceAtMost(mask.height - 1)
            val verticalFraction = maskY - top
            val topOffset = top * mask.width
            val bottomOffset = bottom * mask.width

            for (x in row.indices) {
                val fractionX = horizontalFraction[x]
                val topConfidence = lerp(
                    confidence[topOffset + maskLeft[x]],
                    confidence[topOffset + maskRight[x]],
                    fractionX,
                )
                val bottomConfidence = lerp(
                    confidence[bottomOffset + maskLeft[x]],
                    confidence[bottomOffset + maskRight[x]],
                    fractionX,
                )
                val personConfidence = lerp(
                    topConfidence,
                    bottomConfidence,
                    verticalFraction,
                )
                val retention = BackgroundWhiteningMath.foregroundRetention(personConfidence)
                val pixel = row[x]
                val red = Color.red(pixel)
                val green = Color.green(pixel)
                val blue = Color.blue(pixel)
                val lighteningGain = BackgroundWhiteningMath.subjectLighteningGain(
                    red = red,
                    green = green,
                    blue = blue,
                    foregroundRetention = retention,
                )
                row[x] = Color.argb(
                    BackgroundWhiteningMath.foregroundAlpha(retention),
                    BackgroundWhiteningMath.applyGain(red, lighteningGain),
                    BackgroundWhiteningMath.applyGain(green, lighteningGain),
                    BackgroundWhiteningMath.applyGain(blue, lighteningGain),
                )
            }
            output.setPixels(row, 0, source.width, 0, y, source.width, 1)
        }
        return output
    }

    private fun lerp(start: Float, end: Float, amount: Float): Float =
        start + (end - start) * amount
}

internal object BackgroundWhiteningMath {
    private const val BACKGROUND_CONFIDENCE = 0.08f
    private const val PERSON_CONFIDENCE = 0.62f
    private const val SUBJECT_GAMMA = 0.82f
    private const val MAX_SUBJECT_GAIN = 1.28f
    private const val SUBJECT_LIGHTENING_STRENGTH = 0.90f

    /** Returns 0 for white replacement and 1 for the untouched original pixel. */
    fun foregroundRetention(personConfidence: Float): Float {
        val normalized = (
            (personConfidence.coerceIn(0f, 1f) - BACKGROUND_CONFIDENCE) /
                (PERSON_CONFIDENCE - BACKGROUND_CONFIDENCE)
            ).coerceIn(0f, 1f)
        return normalized * normalized * (3f - 2f * normalized)
    }

    fun foregroundAlpha(foregroundRetention: Float): Int =
        (255f * foregroundRetention.coerceIn(0f, 1f)).roundToInt()

    /**
     * Brightens shadows and midtones more than highlights while using one gain
     * for all RGB channels, which avoids introducing a new color cast.
     */
    fun subjectLighteningGain(
        red: Int,
        green: Int,
        blue: Int,
        foregroundRetention: Float,
    ): Float {
        val luminance = (
            0.2126f * red.coerceIn(0, 255) +
                0.7152f * green.coerceIn(0, 255) +
                0.0722f * blue.coerceIn(0, 255)
            ) / 255f
        if (luminance <= 0.001f || luminance >= 1f) return 1f

        val targetLuminance = luminance.pow(SUBJECT_GAMMA)
        val desiredGain = (targetLuminance / luminance).coerceAtMost(MAX_SUBJECT_GAIN)
        val strength = SUBJECT_LIGHTENING_STRENGTH *
            foregroundRetention.coerceIn(0f, 1f)
        return 1f + (desiredGain - 1f) * strength
    }

    fun applyGain(channel: Int, gain: Float): Int =
        (channel.coerceIn(0, 255) * gain.coerceAtLeast(1f))
            .roundToInt()
            .coerceIn(0, 255)
}

private suspend fun Segmenter.processAwait(image: InputImage): SegmentationMask =
    suspendCancellableCoroutine { continuation ->
        process(image)
            .addOnSuccessListener { mask ->
                if (continuation.isActive) continuation.resume(mask)
            }
            .addOnFailureListener { error ->
                if (continuation.isActive) continuation.resumeWithException(error)
            }
    }
