package com.bellringer.trainer.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun MainTabs(vm: AppViewModel, startTab: Int = 0) {
    // ✅ Стабильное состояние вкладки, не зависит от пересоздания startTab
    var selectedTab by remember { mutableIntStateOf(0) }
    var initialized by remember { mutableStateOf(false) }

    // Устанавливаем стартовую вкладку ТОЛЬКО один раз
    LaunchedEffect(startTab) {
        if (!initialized) {
            selectedTab = startTab
            initialized = true
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF1A1A2E)) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Filled.PlayArrow, contentDescription = "Игра") },
                    label = { Text("Игра") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF69F0AE),
                        selectedTextColor = Color(0xFF69F0AE),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = "Настройки") },
                    label = { Text("Настройки") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF69F0AE),
                        selectedTextColor = Color(0xFF69F0AE),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> TrainingScreen(vm, Modifier.fillMaxSize())
                1 -> SettingsScreen(vm, Modifier.fillMaxSize())
            }
        }
    }
}

