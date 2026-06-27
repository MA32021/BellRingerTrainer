package com.bellringer.trainer.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.Executors

/**
 * Binds the FRONT camera with Preview + ImageAnalysis.
 * Each analysis frame is converted to a mirrored Bitmap and handed off.
 */
class CameraController(
    private val context: Context,
    private val onMirroredFrame: (Bitmap, Long) -> Unit
) {
    private val analysisExecutor = Executors.newSingleThreadExecutor()
    private var startTs = 0L

    @SuppressLint("UnsafeOptInUsageError")
    fun start(owner: LifecycleOwner, previewView: PreviewView) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = future.get()
            startTs = System.currentTimeMillis()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
            analysis.setAnalyzer(analysisExecutor) { proxy ->
                val ts = System.currentTimeMillis() - startTs
                val bmp = proxy.toMirroredBitmap()
                proxy.close()
                if (bmp != null) onMirroredFrame(bmp, ts)
            }

            provider.unbindAll()
            provider.bindToLifecycle(
                owner, CameraSelector.DEFAULT_FRONT_CAMERA, preview, analysis
            )
        }, ContextCompat.getMainExecutor(context))
    }

    private fun ImageProxy.toMirroredBitmap(): Bitmap? {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bmp.copyPixelsFromBuffer(planes[0].buffer)
        val m = Matrix().apply {
            postRotate(imageInfo.rotationDegrees.toFloat())
            postScale(-1f, 1f) // horizontal mirror for front camera
        }
        return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
    }

    fun shutdown() = analysisExecutor.shutdown()
}