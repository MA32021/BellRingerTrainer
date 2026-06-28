package com.bellringer.trainer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SettingsScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    val calib by vm.calib.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = modifier
            .background(Color(0xFF121212))
            .padding(16.dp)
    ) {
        Text("Настройки", style = MaterialTheme.typography.headlineSmall, color = Color.White)
        Spacer(Modifier.height(8.dp))

        Text("Чувствительность жестов", color = Color(0xAAFFFFFF),
            style = MaterialTheme.typography.labelLarge)

        // ✅ Каждый слайдер имеет локальное состояние + onValueChangeFinished
        LocalSlider(
            label = "Скорость удара",
            externalValue = calib.velocityPeak,
            range = 0.1f..0.8f,
            onValueChanged = { vm.updateVelocityPeak(it) }
        )
        LocalSlider(
            label = "Дистанция замаха",
            externalValue = calib.armDist,
            range = 0.05f..0.40f,
            onValueChanged = { vm.updateArmDist(it) }
        )
        LocalSlider(
            label = "Зона возврата",
            externalValue = calib.panReturnDist,
            range = 0.03f..0.25f,
            onValueChanged = { vm.updatePanReturnDist(it) }
        )

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = Color(0x33FFFFFF))
        Spacer(Modifier.height(8.dp))

        Text("Правая рука", color = Color(0xAAFFFFFF),
            style = MaterialTheme.typography.labelLarge)

        LocalSlider(
            label = "Нейтральный угол",
            externalValue = calib.neutralWristAngleDeg,
            range = -45f..45f,
            onValueChanged = { vm.updateNeutralAngle(it) }
        )

        Spacer(Modifier.weight(1f))

        Button(
            onClick = { vm.markCalibrated() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF69F0AE))
        ) {
            Text(if (calib.calibrated) "Перейти к игре" else "Завершить настройку",
                color = Color.Black)
        }

        if (calib.calibrated) {
            Spacer(Modifier.height(6.dp))
            OutlinedButton(
                onClick = { vm.resetCalibration() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252))
            ) {
                Text("Сбросить настройки")
            }
        }

        Spacer(Modifier.height(6.dp))
        OutlinedButton(
            onClick = {
                vm.onExitClicked()
                (context as? android.app.Activity)?.finish()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray)
        ) {
            Text("Выйти")
        }
    }
}

/**
 * ✅ Слайдер с локальным состоянием.
 * Во время драга обновляется ТОЛЬКО локальное значение (мгновенно).
 * В DataStore сохраняется только при onValueChangeFinished (отпускание пальца).
 * Это предотвращает сброс драга из-за рекомпозиции от StateFlow.
 */
@Composable
private fun LocalSlider(
    label: String,
    externalValue: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChanged: (Float) -> Unit
) {
    var localValue by remember { mutableFloatStateOf(externalValue) }

    // Синхронизируем локальное значение с внешним только когда НЕ идёт драг
    LaunchedEffect(externalValue) {
        localValue = externalValue
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 1.dp)
    ) {
        Text(label, color = Color.White, style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(120.dp))

        Slider(
            value = localValue,
            onValueChange = { localValue = it },           // ✅ Мгновенное локальное обновление
            onValueChangeFinished = { onValueChanged(localValue) }, // ✅ Сохранение при отпускании
            valueRange = range,
            modifier = Modifier.weight(1f)
        )

        Text("%.2f".format(localValue), color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
    }
}

