package com.bellringer.trainer.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AppNavigation(vm: AppViewModel) {
    val ui by vm.ui.collectAsStateWithLifecycle()

    // ✅ Всегда MainTabs. Стартовая вкладка зависит от isCalibrated.
    // Экран НЕ пересоздаётся при загрузке данных из DataStore.
    MainTabs(
        vm = vm,
        startTab = if (ui.isCalibrated) 0 else 1
    )
}

