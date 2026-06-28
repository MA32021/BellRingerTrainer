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
import com.bellringer.trainer.model.*
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

    // ✅ Только интерфейс — никаких прямых ссылок на HandLandmarkerHelper
    private var landmarker: LandmarkerBridge? = null

    init {
        viewModelScope.launch {
            // ✅ Загрузка звуков ДО начала работы
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
        leftFsm = LeftHandFsm { ev -> onEvent(ev) }
        rightFsm = RightHandFsm(d.neutralWristAngleDeg) { ev -> onEvent(ev) }
    }

    // ✅ Единственная версия initCv — с полной изоляцией MediaPipe
    fun initCv(context: Context) {
        val isEmulator = Build.HARDWARE.contains("ranchu", ignoreCase = true) ||
                Build.MODEL.contains("sdk_gphone", ignoreCase = true)

        if (isEmulator) {
            Log.w("CameraInit", "Using NoOpLandmarker on emulator")
            landmarker = NoOpLandmarker()
            return
        }

        // На реальном устройстве создаём настоящий хелпер через отдельную функцию
        landmarker = createRealLandmarker(context)
    }

    @androidx.annotation.Keep
    private fun createRealLandmarker(context: Context): LandmarkerBridge {
        // Используем reflection, чтобы избежать статической ссылки на класс
        // Это гарантирует, что ART не загрузит HandLandmarkerHelper при верификации AppViewModel
        return try {
            val clazz = Class.forName("com.bellringer.trainer.cv.HandLandmarkerHelper")
            val constructor = clazz.getConstructor(
                Context::class.java,
                kotlin.jvm.functions.Function1::class.java
            )
            val callback: (HandFrame) -> Unit = { frame: HandFrame -> onFrame(frame) }
            constructor.newInstance(context, callback) as LandmarkerBridge
        } catch (e: Exception) {
            Log.e("AppViewModel", "Failed to create HandLandmarkerHelper via reflection", e)
            NoOpLandmarker()
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

    override fun onCleared() {
        landmarker?.close()
    }

    var onExitRequest: (() -> Unit)? = null

    fun onExitClicked() {
        // Закрываем ресурсы перед выходом
        landmarker?.close()
        onExitRequest?.invoke()
    }

}

