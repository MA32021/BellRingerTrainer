package com.bellringer.trainer.calibration

data class CalibrationData(
    val panX: Float = 0.30f,
    val panY: Float = 0.65f,
    val neutralWristAngleDeg: Float = 0f,
    val calibrated: Boolean = false,
    val padPositions: List<Pair<Float, Float>> = listOf(
        Pair(0.15f, 0.70f),
        Pair(0.38f, 0.80f),
        Pair(0.62f, 0.80f),
        Pair(0.85f, 0.70f)
    ),
    // ✅ Гиперпараметры FSM (вынесены из хардкода)
    val velocityPeak: Float = 0.25f,
    val armDist: Float = 0.18f,
    val panReturnDist: Float = 0.10f,
    val strikeWindowMs: Long = 200L
)

