package com.bellringer.trainer.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bellringer.trainer.audio.BellAudioEngine
import com.bellringer.trainer.calibration.CalibrationData
import com.bellringer.trainer.calibration.CalibrationStore
import com.bellringer.trainer.cv.LandmarkerBridge
import com.bellringer.trainer.cv.NoOpLandmarker
import com.bellringer.trainer.gesture.LeftHandFsm
import com.bellringer.trainer.gesture.RightHandFsm
import com.bellringer.trainer.model.GestureEvent
import com.bellringer.trainer.model.HandFrame
import com.bellringer.trainer.model.Landmark
import com.bellringer.trainer.model.StrikeQuality
import com.bellringer.trainer.model.TrainerUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val store = CalibrationStore(app)
    private val audio = BellAudioEngine(getApplication<Application>().applicationContext)

    private val _ui = MutableStateFlow(TrainerUiState())
    val ui: StateFlow<TrainerUiState> = _ui.asStateFlow()

    private val _calib = MutableStateFlow(CalibrationData())
    val calib: StateFlow<CalibrationData> = _calib.asStateFlow()

    val lastFrame = MutableStateFlow<HandFrame?>(null)
    val flashClean = MutableStateFlow(0L)
    val flashStuck = MutableStateFlow(0L)

    private lateinit var leftFsm: LeftHandFsm
    private lateinit var rightFsm: RightHandFsm

    private var landmarker: LandmarkerBridge? = null
    private var cvInitialized = false

    var onExitRequest: (() -> Unit)? = null

    init {
        viewModelScope.launch {
            audio.initialize(listOf(
                "podzvon_l1_10.ogg",
                "podzvon_l2_11.ogg",
                "podzvon_l3_01.ogg",
                "podzvon_l4_02.ogg",
                "zazvon_down.ogg",
                "zazvon_up.ogg"
            ))

            store.data.collect { d ->
                _calib.value = d
                _ui.value = _ui.value.copy(isCalibrated = d.calibrated)
                buildFsms(d)
            }
        }
    }

    private fun buildFsms(d: CalibrationData) {
        leftFsm = LeftHandFsm { ev -> onEvent(ev) }.apply {
            velocityPeak = d.velocityPeak
            armDist = d.armDist
            panReturnDist = d.panReturnDist
            strikeWindowMs = d.strikeWindowMs
        }
        rightFsm = RightHandFsm(d.neutralWristAngleDeg) { ev -> onEvent(ev) }
    }

    /**
     * Инициализирует ТОЛЬКО landmarker (CV).
     * Камера создаётся отдельно в UI — она не зависит от CV.
     */
    fun initCv(context: Context) {
        if (cvInitialized) return
        cvInitialized = true

        Log.d("CameraInit", "initCv: initializing landmarker")

        val isEmulator = Build.HARDWARE.contains("ranchu", ignoreCase = true) ||
                Build.MODEL.contains("sdk_gphone", ignoreCase = true)

        if (isEmulator) {
            Log.w("CameraInit", "Using NoOpLandmarker on emulator")
            landmarker = NoOpLandmarker()
            return
        }

        landmarker = createRealLandmarker(context)
        Log.d("CameraInit", "Landmarker initialized")
    }

    @androidx.annotation.Keep
    private fun createRealLandmarker(context: Context): LandmarkerBridge {
        return com.bellringer.trainer.cv.HandLandmarkerHelper(context) { frame -> onFrame(frame) }
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
        Log.d("OverlayDebug", "report() called: label='$label', quality=$q")
        _ui.value = _ui.value.copy(
            lastEventText = "$label — ${q.name}", lastQuality = q
        )
        val now = System.currentTimeMillis()
        if (q == StrikeQuality.CLEAN) flashClean.value = now else flashStuck.value = now
    }

    fun savePadPositions(positions: List<Pair<Float, Float>>) {
        viewModelScope.launch { store.save(_calib.value.copy(padPositions = positions)) }
    }

    fun updateVelocityPeak(value: Float) {
        viewModelScope.launch { store.save(_calib.value.copy(velocityPeak = value)) }
    }

    fun updateArmDist(value: Float) {
        viewModelScope.launch { store.save(_calib.value.copy(armDist = value)) }
    }

    fun updatePanReturnDist(value: Float) {
        viewModelScope.launch { store.save(_calib.value.copy(panReturnDist = value)) }
    }

    fun updateStrikeWindowMs(value: Long) {
        viewModelScope.launch { store.save(_calib.value.copy(strikeWindowMs = value)) }
    }

    fun updateNeutralAngle(value: Float) {
        viewModelScope.launch { store.save(_calib.value.copy(neutralWristAngleDeg = value)) }
    }

    fun markCalibrated() {
        viewModelScope.launch { store.save(_calib.value.copy(calibrated = true)) }
    }

    fun resetCalibration() {
        viewModelScope.launch { store.save(CalibrationData()) }
    }

    fun saveCalibration(panX: Float, panY: Float, neutralAngle: Float) {
        viewModelScope.launch { store.save(CalibrationData(panX, panY, neutralAngle, true)) }
    }

    fun onExitClicked() {
        landmarker?.close()
        onExitRequest?.invoke()
    }

    override fun onCleared() {
        landmarker?.close()
        audio.release()
    }
}

