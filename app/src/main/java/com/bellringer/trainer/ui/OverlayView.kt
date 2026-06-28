package com.bellringer.trainer.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import com.bellringer.trainer.calibration.CalibrationData
import com.bellringer.trainer.model.HandFrame
import com.bellringer.trainer.model.Landmark
import kotlin.math.hypot

class OverlayView(context: Context) : View(context) {

    companion object {
        private const val PAD_COUNT = 4
        private const val PAD_RADIUS_X = 70f
        private const val PAD_RADIUS_Y = 45f
        private const val HIT_SCALE = 1.3f
        private const val ACTIVE_DURATION_MS = 300L
        private val PAD_COLORS = intArrayOf(
            Color.parseColor("#FF6B6B"),
            Color.parseColor("#4ECDC4"),
            Color.parseColor("#45B7D1"),
            Color.parseColor("#96CEB4")
        )
        private val PAD_LABELS = arrayOf("L1", "L2", "L3", "L4")
    }

    private var padNormX = floatArrayOf(0.15f, 0.38f, 0.62f, 0.85f)
    private var padNormY = floatArrayOf(0.70f, 0.80f, 0.80f, 0.70f)

    private var activePadIndex = -1
    private var activePadUntil = 0L

    private var draggingPadIndex = -1
    private var isUserDragging = false

    private var frame: HandFrame? = null
    private var flashCleanUntil = 0L
    private var flashStuckUntil = 0L

    var onPadPositionsChanged: ((List<Pair<Float, Float>>) -> Unit)? = null

    private val paintPadFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        alpha = 180
    }
    private val paintPadStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val paintActive = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
        alpha = 200
    }
    private val paintLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 32f
        textAlign = Paint.Align.CENTER
        setShadowLayer(4f, 0f, 0f, Color.BLACK)
        isFakeBoldText = true
    }
    private val paintLandmark = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val paintLandmarkLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 24f
        textAlign = Paint.Align.CENTER
        setShadowLayer(3f, 0f, 0f, Color.BLACK)
    }

    fun setCalib(c: CalibrationData) {
        if (isUserDragging) return
        for (i in 0 until PAD_COUNT.coerceAtMost(c.padPositions.size)) {
            padNormX[i] = c.padPositions[i].first
            padNormY[i] = c.padPositions[i].second
        }
        invalidate()
    }

    fun setFrame(f: HandFrame?) {
        frame = f
        invalidate()
    }

    fun flashClean() {
        flashCleanUntil = System.currentTimeMillis() + 200
        invalidate()
    }

    fun flashStuck() {
        flashStuckUntil = System.currentTimeMillis() + 200
        invalidate()
    }

    fun highlightPad(padIndex: Int) {
        activePadIndex = padIndex.coerceIn(0, PAD_COUNT - 1)
        activePadUntil = System.currentTimeMillis() + ACTIVE_DURATION_MS
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val now = System.currentTimeMillis()

        if (now < flashCleanUntil) {
            canvas.drawColor(Color.argb(60, 105, 240, 174))
        } else if (now < flashStuckUntil) {
            canvas.drawColor(Color.argb(60, 255, 82, 82))
        }

        val isActive = now < activePadUntil

        for (i in 0 until PAD_COUNT) {
            val cx = padNormX[i] * w
            val cy = padNormY[i] * h
            val rect = RectF(cx - PAD_RADIUS_X, cy - PAD_RADIUS_Y, cx + PAD_RADIUS_X, cy + PAD_RADIUS_Y)

            val thisActive = isActive && i == activePadIndex
            val thisDragging = draggingPadIndex == i

            if (thisActive) {
                canvas.drawOval(rect, paintActive)
            }

            paintPadFill.color = PAD_COLORS[i]
            paintPadFill.alpha = if (thisDragging) 220 else 180
            canvas.drawOval(rect, paintPadFill)

            paintPadStroke.color = if (thisDragging) Color.WHITE else PAD_COLORS[i]
            paintPadStroke.strokeWidth = if (thisDragging) 6f else 4f
            canvas.drawOval(rect, paintPadStroke)

            paintLabel.color = if (thisActive) Color.BLACK else Color.WHITE
            canvas.drawText(PAD_LABELS[i], cx, cy + 12f, paintLabel)
        }

        frame?.let { f ->
            drawLandmark(canvas, f.leftWrist, Color.YELLOW, "LW")
            drawLandmark(canvas, f.rightWrist, Color.CYAN, "RW")
        }

        if (isActive) {
            postInvalidateOnAnimation()
        }
    }

    private fun drawLandmark(canvas: Canvas, lm: Landmark?, color: Int, label: String) {
        lm ?: return
        val x = lm.x * width
        val y = lm.y * height
        paintLandmark.color = color
        canvas.drawCircle(x, y, 10f, paintLandmark)
        paintLandmarkLabel.color = color
        canvas.drawText(label, x, y - 18f, paintLandmarkLabel)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val w = width.toFloat()
        val h = height.toFloat()

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                draggingPadIndex = -1
                for (i in 0 until PAD_COUNT) {
                    val cx = padNormX[i] * w
                    val cy = padNormY[i] * h
                    val dx = (event.x - cx) / (PAD_RADIUS_X * HIT_SCALE)
                    val dy = (event.y - cy) / (PAD_RADIUS_Y * HIT_SCALE)
                    if (dx * dx + dy * dy <= 1f) {
                        draggingPadIndex = i
                        break
                    }
                }
                if (draggingPadIndex >= 0) {
                    isUserDragging = true
                    invalidate()
                    return true
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (draggingPadIndex >= 0) {
                    padNormX[draggingPadIndex] = (event.x / w).coerceIn(0.05f, 0.95f)
                    padNormY[draggingPadIndex] = (event.y / h).coerceIn(0.05f, 0.95f)
                    emitPositions()
                    invalidate()
                    return true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (draggingPadIndex >= 0) {
                    draggingPadIndex = -1
                    isUserDragging = false
                    invalidate()
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    private fun emitPositions() {
        val positions = (0 until PAD_COUNT).map { Pair(padNormX[it], padNormY[it]) }
        onPadPositionsChanged?.invoke(positions)
    }
}

