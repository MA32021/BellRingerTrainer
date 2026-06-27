package com.bellringer.trainer.model

/** Left-hand (Podzvon) FSM states. */
enum class LeftState { IDLE, ARMED, STRIKE, CLEAN, STUCK }

/** Right-hand (Zazvon) FSM states. */
enum class RightState {
    IDLE, MOVING_DOWN, MOVING_UP,
    CHORD_DOWN_CLEAN, CHORD_DOWN_STUCK,
    CHORD_UP_CLEAN, CHORD_UP_STUCK
}

/** The four Podzvon leash sectors, ordered by clock position. */
enum class Leash(val clockLabel: String, val angleFromVerticalDeg: Float) {
    L1_10("10 o'clock", 150f), // lowest timbre
    L2_11("11 o'clock", 120f),
    L3_01("1 o'clock", 60f),
    L4_02("2 o'clock", 30f)    // highest timbre
}

enum class StrikeQuality { CLEAN, STUCK }

/** Snapshot of state surfaced to the UI status panel. */
data class TrainerUiState(
    val leftState: LeftState = LeftState.IDLE,
    val rightState: RightState = RightState.IDLE,
    val activeLeash: Leash? = null,
    val lastEventText: String = "—",
    val lastQuality: StrikeQuality? = null,
    val isCalibrated: Boolean = false
)