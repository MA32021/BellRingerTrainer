package com.bellringer.trainer.cv

import com.bellringer.trainer.model.Landmark
import com.bellringer.trainer.model.Leash
import kotlin.math.atan2
import kotlin.math.hypot

object GestureMath {

    /** Polar angle of [point] relative to [pan], measured from vertical-up.
     *  Returns degrees, positive toward the left of the frame. */
    fun angleFromVertical(pan: Landmark, point: Landmark): Float {
        val dx = point.x - pan.x
        val dy = point.y - pan.y          // image y grows downward
        // vector pointing "up" is (0,-1). Angle between up and (dx,dy):
        val deg = Math.toDegrees(atan2(-dx.toDouble(), -dy.toDouble())).toFloat()
        // map to magnitude away from vertical (0..180)
        return kotlin.math.abs(deg)
    }

    /** Distance in normalized space. */
    fun distance(a: Landmark, b: Landmark): Float = hypot(a.x - b.x, a.y - b.y)

    /** Selects the leash whose sector angle is closest, within tolerance. */
    fun classifyLeash(pan: Landmark, hand: Landmark, toleranceDeg: Float = 20f): Leash? {
        val a = angleFromVertical(pan, hand)
        return Leash.entries.minByOrNull { kotlin.math.abs(it.angleFromVerticalDeg - a) }
            ?.takeIf { kotlin.math.abs(it.angleFromVerticalDeg - a) <= toleranceDeg }
    }

    /** Right-wrist tilt angle (deg) of the wrist→middle-MCP vector vs horizontal. */
    fun wristTilt(wrist: Landmark, middleMcp: Landmark): Float {
        val dx = middleMcp.x - wrist.x
        val dy = middleMcp.y - wrist.y
        return Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    }
}