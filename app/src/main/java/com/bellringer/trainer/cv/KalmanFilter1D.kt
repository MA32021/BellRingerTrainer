package com.bellringer.trainer.cv

/**
 * Простой 1D Kalman Filter для сглаживания одной координаты.
 *
 * @param processNoise  (Q) — неопределённость модели движения.
 *                      Больше = быстрее реакция на резкие движения, но больше шума.
 *                      Для рук звонаря: 0.01–0.05
 * @param measurementNoise (R) — неопределённость измерений MediaPipe.
 *                              Больше = сильнее сглаживание, но больше лаг.
 *                              Для MediaPipe Hand Landmarker: 0.001–0.01
 */
class KalmanFilter1D(
    private val processNoise: Float = 0.02f,
    private val measurementNoise: Float = 0.005f
) {
    private var x = 0f      // оценка состояния
    private var p = 1f      // оценка ошибки
    private var initialized = false

    /**
     * Обновляет фильтр новым измерением и возвращает сглаженное значение.
     */
    fun update(measurement: Float): Float {
        if (!initialized) {
            x = measurement
            p = 1f
            initialized = true
            return x
        }

        // Predict
        val pPredicted = p + processNoise

        // Update
        val k = pPredicted / (pPredicted + measurementNoise)  // Kalman gain
        x = x + k * (measurement - x)
        p = (1f - k) * pPredicted

        return x
    }

    /**
     * Сбрасывает фильтр (например, при потере трекинга руки).
     */
    fun reset() {
        initialized = false
    }
}

