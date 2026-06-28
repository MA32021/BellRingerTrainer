package com.bellringer.trainer.ui

import android.os.Build
import android.util.Log
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
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
fun TrainingScreen(vm: AppViewModel) {
    Log.d("DEBUG_TRAINING", "=== TrainingScreen Composed at ${System.currentTimeMillis()} ===")

    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val ui by vm.ui.collectAsStateWithLifecycle()
    val calib by vm.calib.collectAsStateWithLifecycle()
    val frame by vm.lastFrame.collectAsStateWithLifecycle()
    val cleanFlash by vm.flashClean.collectAsStateWithLifecycle()
    val stuckFlash by vm.flashStuck.collectAsStateWithLifecycle()

    val isEmulator = remember {
        Build.HARDWARE.contains("ranchu", ignoreCase = true) ||
                Build.MODEL.contains("sdk_gphone", ignoreCase = true)
    }

    val overlay = remember { OverlayView(context) }

    LaunchedEffect(Unit) {
        vm.initCv(context)
    }
    LaunchedEffect(frame, calib) {
        overlay.setCalib(calib)
        overlay.setFrame(frame)
    }
    LaunchedEffect(cleanFlash) { if (cleanFlash > 0) overlay.flashClean() }
    LaunchedEffect(stuckFlash) { if (stuckFlash > 0) overlay.flashStuck() }

    Box(Modifier.fillMaxSize()) {
        // Камера запускается ТОЛЬКО на реальных устройствах
        if (!isEmulator) {
            AndroidView(factory = { ctx ->
                val pv = PreviewView(ctx)
                CameraController(ctx) { bmp, ts -> vm.onMirroredFrame(bmp, ts) }
                    .start(owner, pv)
                pv
            }, modifier = Modifier.fillMaxSize())
        } else {
            Surface(
                color = Color(0xFF1A1A2E),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "📷 Камера недоступна на эмуляторе\nПодключите реальное устройство",
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        AndroidView(factory = { overlay }, modifier = Modifier.fillMaxSize())

        // Status panel + Exit button
        Surface(
            color = Color(0xAA000000),
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp)
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    "Left (Podzvon): ${ui.leftState}  ${ui.activeLeash?.clockLabel ?: ""}",
                    color = Color.White
                )
                Text("Right (Zazvon): ${ui.rightState}", color = Color.White)
                Text(
                    "Last: ${ui.lastEventText}",
                    color = when (ui.lastQuality?.name) {
                        "CLEAN" -> Color(0xFF69F0AE)
                        "STUCK" -> Color(0xFFFF5252)
                        else -> Color.White
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { vm.onExitClicked() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("🚪 Exit", color = Color.White)
                }
            }
        }

    }
}

