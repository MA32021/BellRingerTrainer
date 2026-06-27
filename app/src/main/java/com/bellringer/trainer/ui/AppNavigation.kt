package com.bellringer.trainer.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavigation() {
    val vm: AppViewModel = viewModel()
    val nav = rememberNavController()
    val ui by vm.ui.collectAsStateWithLifecycle()

    val start = if (ui.isCalibrated) "training" else "calibration"
    NavHost(navController = nav, startDestination = start) {
        composable("calibration") {
            CalibrationScreen(vm) { nav.navigate("training") { popUpTo("calibration") { inclusive = true } } }
        }
        composable("training") { TrainingScreen(vm) }
    }
}