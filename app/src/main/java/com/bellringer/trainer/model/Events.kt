package com.bellringer.trainer.model

/** Normalized landmark in [0,1] image space (already mirrored). */
data class Landmark(val x: Float, val y: Float, val z: Float = 0f)

/** Per-frame parsed hand data fed to the FSMs. */
data class HandFrame(
    val timestampMs: Long,
    // Left hand (Podzvon)
    val leftWrist: Landmark? = null,
    val leftIndexTip: Landmark? = null,
    // Right hand (Zazvon)
    val rightWrist: Landmark? = null,
    val rightMiddleMcp: Landmark? = null
)

/** Output event from an FSM, consumed by audio + UI. */
sealed interface GestureEvent {
    data class Podzvon(val leash: Leash, val quality: StrikeQuality, val velocity: Float) : GestureEvent
    data class Zazvon(val up: Boolean, val quality: StrikeQuality, val velocity: Float) : GestureEvent
}