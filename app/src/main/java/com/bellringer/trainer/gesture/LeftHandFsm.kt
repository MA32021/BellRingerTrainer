package com.bellringer.trainer.gesture

import com.bellringer.trainer.cv.GestureMath
import com.bellringer.trainer.model.*

/**
 * Podzvon left-hand state machine.
 * IDLE → ARMED → STRIKE (200 ms window) → CLEAN→IDLE | STUCK→(wait return)→IDLE
 */
class LeftHandFsm(private val emit: (GestureEvent) -> Unit) {

    private val WINDOW_MS = 200L
    private val PAN_RETURN_DIST = 0.10f   // wrist near pan
    private val ARM_DIST = 0.18f          // wrist far enough from pan to "press"
    private val VELOCITY_PEAK = 0.6f      // normalized units/sec
    private val REVERSAL_DEG = 90f

    var state = LeftState.IDLE; private set
    var activeLeash: Leash? = null; private set

    private var prev: Landmark? = null
    private var prevTs = 0L
    private var peakVx = 0f
    private var peakVy = 0f
    private var peakVel = 0f
    private var strikeStartTs = 0L

    fun reset() { state = LeftState.IDLE; activeLeash = null; prev = null }

    fun update(pan: Landmark, wrist: Landmark?, ts: Long) {
        if (wrist == null) return
        val (vx, vy) = velocity(wrist, ts)
        val speed = kotlin.math.hypot(vx, vy)
        val dPan = GestureMath.distance(pan, wrist)

        when (state) {
            LeftState.IDLE -> {
                if (dPan > ARM_DIST && speed > VELOCITY_PEAK * 0.3f) {
                    activeLeash = GestureMath.classifyLeash(pan, wrist)
                    if (activeLeash != null) { state = LeftState.ARMED }
                }
            }
            LeftState.ARMED -> {
                if (speed > peakVel) { peakVel = speed; peakVx = vx; peakVy = vy }
                if (speed >= VELOCITY_PEAK) {
                    state = LeftState.STRIKE
                    strikeStartTs = ts
                    peakVel = speed; peakVx = vx; peakVy = vy
                } else if (dPan < PAN_RETURN_DIST) {
                    softReset()
                }
            }
            LeftState.STRIKE -> {
                val reversed = angleBetween(peakVx, peakVy, vx, vy) > REVERSAL_DEG
                if (dPan < PAN_RETURN_DIST || (reversed && speed > VELOCITY_PEAK * 0.4f)) {
                    emit(GestureEvent.Podzvon(activeLeash!!, StrikeQuality.CLEAN, peakVel))
                    state = LeftState.CLEAN; softReset()
                } else if (ts - strikeStartTs > WINDOW_MS) {
                    emit(GestureEvent.Podzvon(activeLeash!!, StrikeQuality.STUCK, peakVel))
                    state = LeftState.STUCK
                }
            }
            LeftState.STUCK -> {
                if (dPan < PAN_RETURN_DIST) softReset()  // lockout cleared on return
            }
            LeftState.CLEAN -> softReset()
        }
    }

    private fun softReset() {
        state = LeftState.IDLE; activeLeash = null; peakVel = 0f
    }

    private fun velocity(p: Landmark, ts: Long): Pair<Float, Float> {
        val pr = prev; val pt = prevTs
        prev = p; prevTs = ts
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