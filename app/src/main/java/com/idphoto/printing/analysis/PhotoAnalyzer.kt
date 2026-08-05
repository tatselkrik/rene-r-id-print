package com.idphoto.printing.analysis

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import com.idphoto.printing.core.CropPlan
import com.idphoto.printing.core.CropPlanner
import com.idphoto.printing.core.FloatRect
import com.idphoto.printing.core.ImageSize
import java.io.Closeable
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs
import kotlinx.coroutines.suspendCancellableCoroutine

enum class CheckStatus {
    PASS,
    FAIL,
}

data class ReviewCheck(
    val label: String,
    val status: CheckStatus,
    val detail: String,
)

data class PhotoReview(
    val imageSize: ImageSize,
    val cropPlan: CropPlan?,
    val checks: List<ReviewCheck>,
    val readyToPrint: Boolean,
    val failureMessage: String? = null,
)

class PhotoAnalyzer(context: Context) : Closeable {
    private val appContext = context.applicationContext
    private val detector: FaceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build(),
    )

    suspend fun analyze(uri: Uri, cameraSquare: FloatRect): PhotoReview {
        return try {
            val image = InputImage.fromFilePath(appContext, uri)
            val faces = detector.processAwait(image)
            buildReview(image, faces, cameraSquare)
        } catch (error: Exception) {
            PhotoReview(
                imageSize = ImageSize(0, 0),
                cropPlan = null,
                checks = emptyList(),
                readyToPrint = false,
                failureMessage = error.message ?: "The photo could not be analyzed.",
            )
        }
    }

    private fun buildReview(
        image: InputImage,
        faces: List<Face>,
        cameraSquare: FloatRect,
    ): PhotoReview {
        val imageSize = ImageSize(image.width, image.height)
        val singleFace = faces.singleOrNull()
        val leftEye = singleFace?.getLandmark(FaceLandmark.LEFT_EYE)?.position
        val rightEye = singleFace?.getLandmark(FaceLandmark.RIGHT_EYE)?.position
        val cropPlan = CropPlanner.plan(imageSize, cameraSquare)

        val exactlyOneFace = faces.size == 1
        val eyesFound = leftEye != null && rightEye != null
        val poseAcceptable = singleFace != null &&
            abs(singleFace.headEulerAngleX) <= 12f &&
            abs(singleFace.headEulerAngleY) <= 12f &&
            abs(singleFace.headEulerAngleZ) <= 8f
        val eyesOpen = singleFace?.let(::eyesAppearOpen) ?: false
        val framingReliable = cropPlan?.framingReliable == true
        val resolutionSufficient = cropPlan?.resolutionSufficient == true

        val checks = listOf(
            ReviewCheck(
                label = "One person",
                status = passFail(exactlyOneFace),
                detail = when (faces.size) {
                    0 -> "No face found. Retake with the face inside the guide."
                    1 -> "Exactly one face detected."
                    else -> "${faces.size} faces found. Only one person is allowed."
                },
            ),
            ReviewCheck(
                label = "Eyes detected",
                status = passFail(eyesFound),
                detail = if (eyesFound) {
                    "Both eye landmarks are clearly visible."
                } else {
                    "Both eyes must be clearly visible."
                },
            ),
            ReviewCheck(
                label = "Eyes open",
                status = passFail(eyesOpen),
                detail = if (eyesOpen) {
                    "No obvious closed-eye result."
                } else {
                    "One or both eyes may be closed or obscured."
                },
            ),
            ReviewCheck(
                label = "Head position",
                status = passFail(poseAcceptable),
                detail = if (poseAcceptable) {
                    "Head angle is within the accepted limit."
                } else {
                    "Face the camera directly and keep the head level."
                },
            ),
            ReviewCheck(
                label = "Framing",
                status = passFail(framingReliable),
                detail = if (framingReliable) {
                    "The 2×2 crop matches the camera square. The 35×45 crop trims only left and right."
                } else {
                    "The saved photo did not contain the complete camera square. Please retake."
                },
            ),
            ReviewCheck(
                label = "Print resolution",
                status = passFail(resolutionSufficient),
                detail = if (resolutionSufficient) {
                    "Both guide-based crops have at least 300 dpi source pixels."
                } else {
                    "The usable crop is too small for the 2×2 print."
                },
            ),
            ReviewCheck(
                label = "Blur",
                status = CheckStatus.FAIL,
                detail = "Sharpness is checked from the full-resolution crop.",
            ),
        )

        return PhotoReview(
            imageSize = imageSize,
            cropPlan = cropPlan,
            checks = checks,
            readyToPrint = exactlyOneFace && eyesFound && eyesOpen && poseAcceptable &&
                framingReliable && resolutionSufficient,
        )
    }

    private fun eyesAppearOpen(face: Face): Boolean {
        val left = face.leftEyeOpenProbability
        val right = face.rightEyeOpenProbability
        return left != null && right != null && left >= 0.5f && right >= 0.5f
    }

    override fun close() {
        detector.close()
    }

    private fun passFail(value: Boolean): CheckStatus =
        if (value) CheckStatus.PASS else CheckStatus.FAIL
}

private suspend fun FaceDetector.processAwait(image: InputImage): List<Face> =
    suspendCancellableCoroutine { continuation ->
        process(image)
            .addOnSuccessListener { faces ->
                if (continuation.isActive) continuation.resume(faces)
            }
            .addOnFailureListener { error ->
                if (continuation.isActive) continuation.resumeWithException(error)
            }
    }
