package com.bellringer.trainer.gesture

import android.util.Log
import com.bellringer.trainer.cv.GestureMath
import com.bellringer.trainer.model.*

class LeftHandFsm(private val emit: (GestureEvent) -> Unit) {

    companion object {
        private const val TAG = "LeftHandFsm"
    }

    // ✅ Динамические параметры (обновляются из CalibrationData)
    var velocityPeak: Float = 0.25f
    var armDist: Float = 0.18f
    var panReturnDist: Float = 0.10f
    var strikeWindowMs: Long = 200L
    private val reversalDeg = 90f

    var state = LeftState.IDLE; private set
    var activeLeash: Leash? = null; private set

    private var prev: Landmark? = null
    private var prevTs = 0L
    private var peakVx = 0f
    private var peakVy = 0f
    private var peakVel = 0f
    private var strikeStartTs = 0L

    fun reset() {
        state = LeftState.IDLE
        activeLeash = null
        prev = null
    }

    fun update(pan: Landmark, wrist: Landmark?, ts: Long) {
        if (wrist == null) return
        val (vx, vy) = velocity(wrist, ts)
        val speed = kotlin.math.hypot(vx, vy)
        val dPan = GestureMath.distance(pan, wrist)

        when (state) {
            LeftState.IDLE -> {
                if (dPan > armDist && speed > velocityPeak * 0.3f) {
                    activeLeash = GestureMath.classifyLeash(pan, wrist)
                    if (activeLeash != null) {
                        state = LeftState.ARMED
                        Log.d(TAG, "→ ARMED: leash=${activeLeash!!.clockLabel}")
                    }
                }
            }
            LeftState.ARMED -> {
                if (speed > peakVel) {
                    peakVel = speed
                    peakVx = vx
                    peakVy = vy
                }
                if (speed >= velocityPeak) {
                    state = LeftState.STRIKE
                    strikeStartTs = ts
                    peakVel = speed
                    peakVx = vx
                    peakVy = vy
                    Log.d(TAG, "→ STRIKE: peakVel=${"%.3f".format(peakVel)}")
                } else if (dPan < panReturnDist) {
                    softReset()
                }
            }
            LeftState.STRIKE -> {
                val reversed = angleBetween(peakVx, peakVy, vx, vy) > reversalDeg
                if (dPan < panReturnDist || (reversed && speed > velocityPeak * 0.4f)) {
                    val ev = GestureEvent.Podzvon(activeLeash!!, StrikeQuality.CLEAN, peakVel)
                    Log.d(TAG, "🔔 Podzvon CLEAN: leash=${ev.leash.clockLabel}, vel=${"%.3f".format(ev.velocity)}")
                    emit(ev)
                    state = LeftState.CLEAN
                    softReset()
                } else if (ts - strikeStartTs > strikeWindowMs) {
                    val ev = GestureEvent.Podzvon(activeLeash!!, StrikeQuality.STUCK, peakVel)
                    Log.d(TAG, "🔔 Podzvon STUCK: leash=${ev.leash.clockLabel}, vel=${"%.3f".format(ev.velocity)}")
                    emit(ev)
                    state = LeftState.STUCK
                }
            }
            LeftState.STUCK -> {
                if (dPan < panReturnDist) softReset()
            }
            LeftState.CLEAN -> softReset()
        }
    }

    private fun softReset() {
        state = LeftState.IDLE
        activeLeash = null
        peakVel = 0f
    }

    private fun velocity(p: Landmark, ts: Long): Pair<Float, Float> {
        val pr = prev
        val pt = prevTs
        prev = p
        prevTs = ts
        if (pr == null || ts <= pt) return 0f to 0f
        val dt = (ts - pt) / 1000f
        return (p.x - pr.x) / dt to (p.y - pr.y) / dt
    }

    private fun angleBetween(ax: Float, ay: Float, bx: Float, by: Float): Float {
        val dot = ax * bx + ay * by
        val mag = kotlin.math.hypot(ax, ay) * kotlin.math.hypot(bx, by)
        if (mag < 1e-4f) return 0f
        return Math.toDegrees(kotlin.math.acos((dot / mag).coerceIn(-1f, 1f)).toDouble()).toFloat()
    }
}

