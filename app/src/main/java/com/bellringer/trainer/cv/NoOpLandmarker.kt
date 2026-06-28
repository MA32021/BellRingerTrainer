// cv/NoOpLandmarker.kt
package com.bellringer.trainer.cv

import android.graphics.Bitmap
import android.util.Log

class NoOpLandmarker : LandmarkerBridge {
    override fun detectAsync(mirrored: Bitmap, timestampMs: Long) {
        Log.d("NoOpLandmarker", "Skipping frame on emulator")
    }
    override fun close() {}
}

