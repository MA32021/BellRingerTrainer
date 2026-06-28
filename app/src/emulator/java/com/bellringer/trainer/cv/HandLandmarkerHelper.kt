package com.bellringer.trainer.cv

import android.content.Context
import android.graphics.Bitmap
import android.util.Log

/**
 * Stub for emulator builds — MediaPipe is not available.
 * This class exists solely to satisfy compilation of AppViewModel.createRealLandmarker().
 * It should NEVER be instantiated on emulator (NoOpLandmarker is used instead).
 */
class HandLandmarkerHelper(
    context: Context,
    private val onFrame: (com.bellringer.trainer.model.HandFrame) -> Unit
) : LandmarkerBridge {

    init {
        Log.e("HandLandmarkerHelper", "⚠️ STUB called on emulator! This should never happen.")
    }

    override fun detectAsync(mirrored: Bitmap, timestampMs: Long) {
        // No-op
    }

    override fun close() {
        // No-op
    }

}

