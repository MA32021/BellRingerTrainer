package com.bellringer.trainer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.bellringer.trainer.ui.AppNavigation
import androidx.activity.compose.rememberLauncherForActivityResult

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var granted by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                                == PackageManager.PERMISSION_GRANTED
                    )
                }
                val launcher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted = it }

                LaunchedEffect(Unit) {
                    if (!granted) launcher.launch(Manifest.permission.CAMERA)
                }

                if (granted) AppNavigation()
                else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Camera permission is required for the trainer.")
                }
            }
        }
    }
}