// cv/LandmarkerBridge.kt
package com.bellringer.trainer.cv

import android.graphics.Bitmap

interface LandmarkerBridge {
    fun detectAsync(mirrored: Bitmap, timestampMs: Long)
    fun close()
}
