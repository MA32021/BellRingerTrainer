package com.bellringer.trainer.calibration

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("calibration")

/** Persists the frying-pan anchor (normalized) and neutral right-wrist angle. */

class CalibrationStore(private val context: Context) {
    private object Keys {
        val PAN_X = floatPreferencesKey("pan_x")
        val PAN_Y = floatPreferencesKey("pan_y")
        val NEUTRAL = floatPreferencesKey("neutral_angle")
        val DONE = booleanPreferencesKey("calibrated")
    }

    val data: Flow<CalibrationData> = context.dataStore.data.map { p ->
        CalibrationData(
            panX = p[Keys.PAN_X] ?: 0.30f,
            panY = p[Keys.PAN_Y] ?: 0.65f,
            neutralWristAngleDeg = p[Keys.NEUTRAL] ?: 0f,
            calibrated = p[Keys.DONE] ?: false
        )
    }

    suspend fun current(): CalibrationData = data.first()

    suspend fun save(d: CalibrationData) {
        context.dataStore.edit { p ->
            p[Keys.PAN_X] = d.panX
            p[Keys.PAN_Y] = d.panY
            p[Keys.NEUTRAL] = d.neutralWristAngleDeg
            p[Keys.DONE] = true
        }
    }
}