package com.bellringer.trainer.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun CalibrationScreen(vm: AppViewModel, onDone: () -> Unit) {
    var panX by remember { mutableStateOf(0.30f) }
    var panY by remember { mutableStateOf(0.65f) }
    var neutral by remember { mutableStateOf(0f) }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Calibration", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text("Assume your working stance. Tap below to set the Frying Pan anchor " +
                "(lower-left, waist level). Then set neutral right-wrist angle.")
        Spacer(Modifier.height(16.dp))

        Box(
            Modifier.fillMaxWidth().weight(1f)
                .pointerInput(Unit) {
                    detectTapGestures { o: Offset ->
                        panX = (o.x / size.width).coerceIn(0f, 1f)
                        panY = (o.y / size.height).coerceIn(0f, 1f)
                    }
                }
        ) {
            Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxSize()) {
                Text("Pan: x=${"%.2f".format(panX)}, y=${"%.2f".format(panY)}",
                    Modifier.padding(16.dp))
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Neutral right-wrist angle: ${neutral.toInt()}°")
        Slider(value = neutral, onValueChange = { neutral = it }, valueRange = -45f..45f)

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { vm.saveCalibration(panX, panY, neutral); onDone() },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save & Start Training") }
    }
}