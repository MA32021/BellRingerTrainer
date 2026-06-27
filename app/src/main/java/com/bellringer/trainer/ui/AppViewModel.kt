package com.bellringer.trainer.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bellringer.trainer.audio.BellAudioEngine
import com.bellringer.trainer.calibration.CalibrationData
import com.bellringer.trainer.calibration.CalibrationStore
import com.bellringer.trainer.cv.HandLandmarkerHelper
import com.bellringer.trainer.gesture.LeftHandFsm
import com.bellringer.trainer.gesture.RightHandFsm
import com.bellringer.trainer.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val store = CalibrationStore(app)
    private val audio = BellAudioEngine()

    private val _ui = MutableStateFlow(TrainerUiState())
    val ui: StateFlow<TrainerUiState> = _ui.asStateFlow()

    private val _calib = MutableStateFlow(CalibrationData())
    val calib: StateFlow<CalibrationData> = _calib.asStateFlow()

    // latest landmarks for overlay rendering
    val lastFrame = MutableStateFlow<HandFrame?>(null)
    val flashClean = MutableStateFlow(0L)
    val flashStuck = MutableStateFlow(0L)

    private lateinit var leftFsm: LeftHandFsm
    private lateinit var rightFsm: RightHandFsm
    private var landmarker: HandLandmarkerHelper? = null

    init {
        viewModelScope.launch {
            store.data.collect { d ->
                _calib.value = d
                _ui.value = _ui.value.copy(isCalibrated = d.calibrated)
                buildFsms(d)
            }
        }
    }

    private fun buildFsms(d: CalibrationData) {
        leftFsm = LeftHandFsm { ev -> onEvent(ev) }
        rightFsm = RightHandFsm(d.neutralWristAngleDeg) { ev -> onEvent(ev) }
    }

    fun initCv() {
        if (landmarker == null) {
            landmarker = HandLandmarkerHelper(getApplication()) { frame -> onFrame(frame) }
        }
    }

    fun onMirroredFrame(bmp: Bitmap, ts: Long) {
        landmarker?.detectAsync(bmp, ts)
    }

    private fun onFrame(frame: HandFrame) {
        lastFrame.value = frame
        val pan = Landmark(_calib.value.panX, _calib.value.panY)
        leftFsm.update(pan, frame.leftWrist, frame.timestampMs)
        rightFsm.update(frame.rightWrist, frame.rightMiddleMcp, frame.timestampMs)
        _ui.value = _ui.value.copy(
            leftState = leftFsm.state,
            rightState = rightFsm.state,
            activeLeash = leftFsm.activeLeash
        )
    }

    private fun onEvent(ev: GestureEvent) {
        when (ev) {
            is GestureEvent.Podzvon -> {
                audio.playPodzvon(ev.leash, ev.quality, ev.velocity)
                report("Podzvon ${ev.leash.clockLabel}", ev.quality)
            }
            is GestureEvent.Zazvon -> {
                audio.playZazvon(ev.up, ev.quality, ev.velocity)
                report("Zazvon ${if (ev.up) "1&4" else "2&3"}", ev.quality)
            }
        }
    }

    private fun report(label: String, q: StrikeQuality) {
        _ui.value = _ui.value.copy(
            lastEventText = "$label — ${q.name}", lastQuality = q
        )
        val now = System.currentTimeMillis()
        if (q == StrikeQuality.CLEAN) flashClean.value = now else flashStuck.value = now
    }

    fun saveCalibration(panX: Float, panY: Float, neutralAngle: Float) {
        viewModelScope.launch {
            store.save(CalibrationData(panX, panY, neutralAngle, true))
        }
    }

    override fun onCleared() { landmarker?.close() }
}