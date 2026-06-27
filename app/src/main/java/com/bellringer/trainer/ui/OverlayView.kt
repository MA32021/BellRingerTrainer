package com.bellringer.trainer.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import com.bellringer.trainer.calibration.CalibrationData
import com.bellringer.trainer.model.HandFrame
import com.bellringer.trainer.model.Leash
import kotlin.math.cos
import kotlin.math.sin

/** Custom View drawing pan, leash rays, landmarks, and flashes over the preview. */
class OverlayView(context: Context) : View(context) {

    private var frame: HandFrame? = null
    private var calib: CalibrationData = CalibrationData()
    private var cleanFlashUntil = 0L
    private var stuckFlashUntil = 0L

    private val rayColors = intArrayOf(
        Color.parseColor("#FF5252"), Color.parseColor("#FFD740"),
        Color.parseColor("#69F0AE"), Color.parseColor("#40C4FF")
    )
    private val white = Paint().apply { color = Color.WHITE; isAntiAlias = true }
    private val rayPaint = Paint().apply { strokeWidth = 6f; isAntiAlias = true }
    private val dotPaint = Paint().apply { color = Color.CYAN; isAntiAlias = true }

    fun setFrame(f: HandFrame?) { frame = f; postInvalidate() }
    fun setCalib(c: CalibrationData) { calib = c; postInvalidate() }
    fun flashClean() { cleanFlashUntil = System.currentTimeMillis() + 250 }
    fun flashStuck() { stuckFlashUntil = System.currentTimeMillis() + 250 }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val px = calib.panX * w; val py = calib.panY * h
        val len = h * 0.45f

        // Leash rays from 10 to 2 o'clock
        Leash.entries.forEachIndexed { i, leash ->
            rayPaint.color = rayColors[i]
            val a = Math.toRadians(leash.angleFromVerticalDeg.toDouble())
            // angleFromVertical positive toward left of frame
            val ex = px - (sin(a) * len).toFloat() * signFor(leash)
            val ey = py - (cos(a) * len).toFloat()
            canvas.drawLine(px, py, ex, ey, rayPaint)
        }

        // Frying pan marker
        canvas.drawCircle(px, py, 22f, white)

        // Landmarks
        frame?.let { f ->
            dotPaint.color = Color.CYAN
            f.leftWrist?.let { canvas.drawCircle(it.x * w, it.y * h, 14f, dotPaint) }
            f.leftIndexTip?.let { canvas.drawCircle(it.x * w, it.y * h, 10f, dotPaint) }
            dotPaint.color = Color.MAGENTA
            f.rightWrist?.let { canvas.drawCircle(it.x * w, it.y * h, 14f, dotPaint) }
            f.rightMiddleMcp?.let { canvas.drawCircle(it.x * w, it.y * h, 10f, dotPaint) }
        }

        // Flash overlays
        val now = System.currentTimeMillis()
        if (now < cleanFlashUntil) drawFlash(canvas, Color.argb(90, 0, 230, 80))
        if (now < stuckFlashUntil) drawFlash(canvas, Color.argb(90, 230, 30, 30))
        if (now < cleanFlashUntil || now < stuckFlashUntil) postInvalidateOnAnimation()
    }

    private fun signFor(leash: Leash): Float =
        when (leash) { Leash.L1_10, Leash.L2_11 -> 1f; else -> -1f }

    private fun drawFlash(c: Canvas, color: Int) {
        val p = Paint().apply { this.color = color }
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), p)
    }
}