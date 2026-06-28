package com.bellringer.trainer.ui

import android.content.res.Configuration
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

@Composable
fun CalibrationScreen(vm: AppViewModel, onDone: () -> Unit) {
    var panX by remember { mutableStateOf(0.30f) }
    var panY by remember { mutableStateOf(0.65f) }
    var neutral by remember { mutableStateOf(0f) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        // ✅ Landscape: зона тапа слева, панель управления справа
        Row(Modifier.fillMaxSize()) {
            // Зона тапа занимает всё доступное пространство слева
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(Unit) {
                        detectTapGestures { o: Offset ->
                            panX = (o.x / size.width).coerceIn(0f, 1f)
                            panY = (o.y / size.height).coerceIn(0f, 1f)
                        }
                    }
            ) {
                Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxSize()) {
                    Text(
                        "Pan: x=${"%.2f".format(panX)}, y=${"%.2f".format(panY)}",
                        Modifier.padding(16.dp)
                    )
                }
            }

            // Панель управления прижата к правому краю
            Surface(
                color = Color(0xAA000000),
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 300.dp)
            ) {
                Column(
                    Modifier
                        .padding(16.dp)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Calibration", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tap left area to set Pan anchor. Adjust wrist angle below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                    Spacer(Modifier.height(12.dp))

                    Text("Angle: ${neutral.toInt()}°", style = MaterialTheme.typography.bodySmall, color = Color.White)
                    Slider(
                        value = neutral,
                        onValueChange = { neutral = it },
                        valueRange = -45f..45f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color(0xFF69F0AE)
                        )
                    )

                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { vm.saveCalibration(panX, panY, neutral); onDone() },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Save & Start", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    } else {
        // ✅ Portrait: оригинальная вертикальная раскладка
        Column(Modifier.fillMaxSize().padding(24.dp)) {
            Text("Calibration", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            Text(
                "Assume your working stance. Tap below to set the Frying Pan anchor " +
                        "(lower-left, waist level). Then set neutral right-wrist angle."
            )
            Spacer(Modifier.height(16.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .pointerInput(Unit) {
                        detectTapGestures { o: Offset ->
                            panX = (o.x / size.width).coerceIn(0f, 1f)
                            panY = (o.y / size.height).coerceIn(0f, 1f)
                        }
                    }
            ) {
                Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxSize()) {
                    Text(
                        "Pan: x=${"%.2f".format(panX)}, y=${"%.2f".format(panY)}",
                        Modifier.padding(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Neutral right-wrist angle: ${neutral.toInt()}°")
            Slider(value = neutral, onValueChange = { neutral = it }, valueRange = -45f..45f)

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { vm.saveCalibration(panX, panY, neutral); onDone() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save & Start Training")
            }
        }
    }
}
