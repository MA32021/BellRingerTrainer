package com.bellringer.trainer.cv

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.bellringer.trainer.model.HandFrame
import com.bellringer.trainer.model.Landmark

/**
 * Wraps MediaPipe Hand Landmarker in LIVE_STREAM mode.
 * Input bitmaps are expected ALREADY MIRRORED (front camera).
 * Applies Kalman smoothing to key landmarks before emitting frames.
 */
class HandLandmarkerHelper(
    context: Context,
    private val onFrame: (HandFrame) -> Unit
) : LandmarkerBridge {

    companion object {
        const val WRIST = 0
        const val INDEX_TIP = 8
        const val MIDDLE_MCP = 9
    }

    private var landmarker: HandLandmarker? = null

    // ✅ Фильтр Калмана для сглаживания координат
    private val smoother = LandmarkSmoother(
        processNoise = 0.02f,
        measurementNoise = 0.005f
    )

    init {
        landmarker = try {
            val base = BaseOptions.builder()
                .setModelAssetPath("models/hand_landmarker.task")
                .build()
            val options = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(base)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumHands(2)
                .setMinHandDetectionConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setMinHandPresenceConfidence(0.5f)
                .setResultListener { result, _ -> handle(result) }
                .setErrorListener { e -> Log.e("HandLandmarker", "Error: ${e.message}") }
                .build()
            HandLandmarker.createFromOptions(context, options)
        } catch (e: Exception) {
            Log.e("HandLandmarker", "Failed to create landmarker: ${e.message}", e)
            null
        }
    }

    override fun detectAsync(mirrored: Bitmap, timestampMs: Long) {
        val lm = landmarker ?: run {
            Log.w("HandLandmarker", "Landmarker not initialized, skipping frame")
            return
        }
        val mpImage = BitmapImageBuilder(mirrored).build()
        lm.detectAsync(mpImage, timestampMs)
    }

    private fun handle(result: HandLandmarkerResult) {
        val ts = System.currentTimeMillis()
        var leftWrist: Landmark? = null
        var leftIndex: Landmark? = null
        var rightWrist: Landmark? = null
        var rightMcp: Landmark? = null

        result.handednesses().forEachIndexed { i, handed ->
            val label = handed.firstOrNull()?.categoryName() ?: return@forEachIndexed
            val lm = result.landmarks().getOrNull(i) ?: return@forEachIndexed
            fun pt(idx: Int) = lm[idx].let { Landmark(it.x(), it.y(), it.z()) }
            if (label == "Right") {
                rightWrist = pt(WRIST); rightMcp = pt(MIDDLE_MCP)
            } else {
                leftWrist = pt(WRIST); leftIndex = pt(INDEX_TIP)
            }
        }

        // ✅ Сброс фильтров при потере трекинга руки
        if (leftWrist == null) smoother.resetHand(isLeft = true)
        if (rightWrist == null) smoother.resetHand(isLeft = false)

        // ✅ Сглаживание через фильтр Калмана ПЕРЕД отправкой в FSM
        val rawFrame = HandFrame(
            timestampMs = ts,
            leftWrist = leftWrist,
            leftIndexTip = leftIndex,
            rightWrist = rightWrist,
            rightMiddleMcp = rightMcp
        )
        val smoothedFrame = smoother.smooth(rawFrame)

        onFrame(smoothedFrame)
    }

    override fun close() {
        landmarker?.close()
    }
}
