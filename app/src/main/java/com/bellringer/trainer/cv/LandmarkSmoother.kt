package com.bellringer.trainer.cv

import com.bellringer.trainer.model.HandFrame
import com.bellringer.trainer.model.Landmark

/**
 * Сглаживает ключевые ландмарки обеих рук через независимые 1D Kalman Filters.
 * Обрабатывает только те точки, которые используются в FSM:
 *   - Left: wrist(0), indexTip(8)
 *   - Right: wrist(0), middleMcp(9)
 */
class LandmarkSmoother(
    processNoise: Float = 0.02f,
    measurementNoise: Float = 0.005f
) {
    // Левая рука: wrist(0), indexTip(8) × 3 координаты
    private val leftWristFilters = List(3) { KalmanFilter1D(processNoise, measurementNoise) }
    private val leftIndexFilters = List(3) { KalmanFilter1D(processNoise, measurementNoise) }

    // Правая рука: wrist(0), middleMcp(9) × 3 координаты
    private val rightWristFilters = List(3) { KalmanFilter1D(processNoise, measurementNoise) }
    private val rightMcpFilters = List(3) { KalmanFilter1D(processNoise, measurementNoise) }

    fun smooth(frame: HandFrame): HandFrame {
        return frame.copy(
            leftWrist = frame.leftWrist?.let { smooth(it, leftWristFilters) },
            leftIndexTip = frame.leftIndexTip?.let { smooth(it, leftIndexFilters) },
            rightWrist = frame.rightWrist?.let { smooth(it, rightWristFilters) },
            rightMiddleMcp = frame.rightMiddleMcp?.let { smooth(it, rightMcpFilters) }
        )
    }

    private fun smooth(lm: Landmark, filters: List<KalmanFilter1D>): Landmark {
        return Landmark(
            x = filters[0].update(lm.x),
            y = filters[1].update(lm.y),
            z = filters[2].update(lm.z)
        )
    }

    /**
     * Вызывайте, когда рука потеряна (landmark == null),
     * чтобы фильтр не продолжал сглаживать с последнего значения.
     */
    fun resetHand(isLeft: Boolean) {
        if (isLeft) {
            leftWristFilters.forEach { it.reset() }
            leftIndexFilters.forEach { it.reset() }
        } else {
            rightWristFilters.forEach { it.reset() }
            rightMcpFilters.forEach { it.reset() }
        }
    }
}
