package com.bellringer.trainer.cv

import android.content.Context
import android.graphics.Bitmap
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
 */
class HandLandmarkerHelper(
    context: Context,
    private val onFrame: (HandFrame) -> Unit
) {
    companion object {
        // 21-landmark indices
        const val WRIST = 0
        const val INDEX_TIP = 8
        const val MIDDLE_MCP = 9
    }

    private val landmarker: HandLandmarker

    init {
        val base = BaseOptions.builder()
            .setModelAssetPath("hand_landmarker.task")
            .build()
        val options = HandLandmarker.HandLandmarkerOptions.builder()
            .setBaseOptions(base)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setNumHands(2)
            .setMinHandDetectionConfidence(0.5f)
            .setMinTrackingConfidence(0.5f)
            .setMinHandPresenceConfidence(0.5f)
            .setResultListener { result, _ -> handle(result) }
            .setErrorListener { e -> e.printStackTrace() }
            .build()
        landmarker = HandLandmarker.createFromOptions(context, options)
    }

    /** Pass a mirrored bitmap with monotonic timestamp (ms). */
    fun detectAsync(mirrored: Bitmap, timestampMs: Long) {
        val mpImage = BitmapImageBuilder(mirrored).build()
        landmarker.detectAsync(mpImage, timestampMs)
    }

    private fun handle(result: HandLandmarkerResult) {
        val ts = System.currentTimeMillis()
        var leftWrist: Landmark? = null
        var leftIndex: Landmark? = null
        var rightWrist: Landmark? = null
        var rightMcp: Landmark? = null

        result.handednesses().forEachIndexed { i, handed ->
            // Because the frame is mirrored to match the user, MediaPipe's
            // "Right" label corresponds to the user's right hand on screen.
            val label = handed.firstOrNull()?.categoryName() ?: return@forEachIndexed
            val lm = result.landmarks().getOrNull(i) ?: return@forEachIndexed
            fun pt(idx: Int) = lm[idx].let { Landmark(it.x(), it.y(), it.z()) }
            if (label == "Right") {
                rightWrist = pt(WRIST); rightMcp = pt(MIDDLE_MCP)
            } else {
                leftWrist = pt(WRIST); leftIndex = pt(INDEX_TIP)
            }
        }
        onFrame(
            HandFrame(
                timestampMs = ts,
                leftWrist = leftWrist, leftIndexTip = leftIndex,
                rightWrist = rightWrist, rightMiddleMcp = rightMcp
            )
        )
    }

    fun close() = landmarker.close()
}