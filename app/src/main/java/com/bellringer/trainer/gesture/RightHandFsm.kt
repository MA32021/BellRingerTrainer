package com.bellringer.trainer.gesture

import com.bellringer.trainer.cv.GestureMath
import com.bellringer.trainer.model.*

/**
 * Zazvon right-hand state machine driven by wrist tilt vs neutral.
 * Downward tilt → chord(2,3); upward tilt → chord(1,4).
 */
class RightHandFsm(
    private val neutralAngleDeg: Float,
    private val emit: (GestureEvent) -> Unit
) {
    private val WINDOW_MS = 200L
    private val TILT_ENTER = 22f   // hysteresis: enter
    private val TILT_RETURN = 10f  // hysteresis: back to neutral

    var state = RightState.IDLE; private set

    private var prevAngle = neutralAngleDeg
    private var prevTs = 0L
    private var startTs = 0L
    private var peakVel = 0f

    fun reset() { state = RightState.IDLE }

    fun update(wrist: Landmark?, mcp: Landmark?, ts: Long) {
        if (wrist == null || mcp == null) return
        val angle = GestureMath.wristTilt(wrist, mcp) - neutralAngleDeg
        val vel = angularVel(angle, ts)

        when (state) {
            RightState.IDLE -> {
                if (angle > TILT_ENTER) start(RightState.MOVING_DOWN, vel)
                else if (angle < -TILT_ENTER) start(RightState.MOVING_UP, vel)
            }
            RightState.MOVING_DOWN -> handleMoving(angle, ts, down = true)
            RightState.MOVING_UP -> handleMoving(angle, ts, down = false)
            RightState.CHORD_DOWN_STUCK, RightState.CHORD_UP_STUCK -> {
                if (kotlin.math.abs(angle) < TILT_RETURN) state = RightState.IDLE
            }
            else -> state = RightState.IDLE
        }
    }

    private fun handleMoving(angle: Float, ts: Long, down: Boolean) {
        if (kotlin.math.abs(angle) < TILT_RETURN) {
            emit(GestureEvent.Zazvon(up = !down, StrikeQuality.CLEAN, peakVel))
            state = RightState.IDLE
        } else if (ts - startTs > WINDOW_MS) {
            emit(GestureEvent.Zazvon(up = !down, StrikeQuality.STUCK, peakVel))
            state = if (down) RightState.CHORD_DOWN_STUCK else RightState.CHORD_UP_STUCK
        }
    }

    private fun start(s: RightState, vel: Float) {
        state = s; startTs = System.currentTimeMillis(); peakVel = kotlin.math.abs(vel)
    }

    private fun angularVel(angle: Float, ts: Long): Float {
        val v = if (ts > prevTs) (angle - prevAngle) / ((ts - prevTs) / 1000f) else 0f
        prevAngle = angle; prevTs = ts
        peakVel = maxOf(peakVel, kotlin.math.abs(v))
        return v
    }
}