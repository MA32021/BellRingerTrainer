package com.bellringer.trainer.ui

import android.os.Build
import android.util.Log
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bellringer.trainer.camera.CameraController

@Composable
fun TrainingScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val ui by vm.ui.collectAsStateWithLifecycle()
    val calib by vm.calib.collectAsStateWithLifecycle()
    val frame by vm.lastFrame.collectAsStateWithLifecycle()
    val cleanFlash by vm.flashClean.collectAsStateWithLifecycle()
    val stuckFlash by vm.flashStuck.collectAsStateWithLifecycle()

    val isEmulator = remember {
        Build.HARDWARE.contains("ranchu", ignoreCase = true) ||
                Build.MODEL.contains("sdk_gphone", ignoreCase = true)
    }

    val overlay = remember {
        OverlayView(context).apply {
            onPadPositionsChanged = { positions -> vm.savePadPositions(positions) }
        }
    }

    // ✅ Landmarker инициализируется параллельно, не блокирует камеру
    LaunchedEffect(Unit) {
        vm.initCv(context)
    }

    LaunchedEffect(calib) { overlay.setCalib(calib) }
    LaunchedEffect(frame) { overlay.setFrame(frame) }
    LaunchedEffect(cleanFlash) { if (cleanFlash > 0) overlay.flashClean() }
    LaunchedEffect(stuckFlash) { if (stuckFlash > 0) overlay.flashStuck() }

    // ✅ Подсветка площадки при звоне
    LaunchedEffect(ui.lastEventText, ui.lastQuality) {
        val text = ui.lastEventText
        Log.d("OverlayDebug", "Event: '$text', quality=${ui.lastQuality}")
        when {
            text.startsWith("Podzvon") -> {
                val index = when {
                    text.contains("10") -> 0
                    text.contains("11") -> 1
                    text.contains("1 o'clock") || text.contains(" 1 ") -> 2
                    text.contains("2 o'clock") || text.contains(" 2 ") -> 3
                    else -> -1
                }
                Log.d("OverlayDebug", "Podzvon → pad index: $index")
                if (index >= 0) overlay.highlightPad(index)
            }
            text.startsWith("Zazvon") -> {
                Log.d("OverlayDebug", "Zazvon detected → flashing all pads")
                for (i in 0..3) overlay.highlightPad(i)
            }
        }
    }

    Box(modifier = modifier) {
        // ✅ Камера: factory создаёт PreviewView, update стартует камеру синхронно
        if (!isEmulator) {
            AndroidView(
                factory = { ctx ->
                    Log.d("CameraInit", "factory: creating PreviewView")
                    PreviewView(ctx)
                },
                update = { pv ->
                    Log.d("CameraInit", "update: starting camera")
                    CameraController(context) { bmp, ts -> vm.onMirroredFrame(bmp, ts) }
                        .start(lifecycleOwner, pv)
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Surface(color = Color(0xFF1A1A2E), modifier = Modifier.fillMaxSize()) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "\uD83D\uDCF7 Камера недоступна на эмуляторе\nПодключите реальное устройство",
                        color = Color.White, textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        // ✅ Нативный оверлей с площадками, ландмарками, drag и подсветкой
        AndroidView(
            factory = { overlay },
            modifier = Modifier.fillMaxSize()
        )

        // Компактный статус в углу
        Surface(
            color = Color(0xAA000000),
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp)
        ) {
            Column(Modifier.padding(12.dp)) {
                Text("L: ${ui.leftState} ${ui.activeLeash?.clockLabel ?: ""}",
                    color = Color.White, style = MaterialTheme.typography.bodySmall)
                Text("R: ${ui.rightState}",
                    color = Color.White, style = MaterialTheme.typography.bodySmall)
                Text(ui.lastEventText,
                    color = when (ui.lastQuality?.name) {
                        "CLEAN" -> Color(0xFF69F0AE)
                        "STUCK" -> Color(0xFFFF5252)
                        else -> Color.White
                    },
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

