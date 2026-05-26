package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.ui.PanchayatApp
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.PanchayatViewModel

class MainActivity : ComponentActivity() {

  private val viewModel: PanchayatViewModel by viewModels()

  // Dynamic Microphone Recording Permission Contract
  private val requestPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { isGranted: Boolean ->
    if (isGranted) {
      Toast.makeText(this, "Microphone enabled! You can now record voice tickets.", Toast.LENGTH_SHORT).show()
    } else {
      Toast.makeText(this, "Microphone permission denied. Speak feature is limited.", Toast.LENGTH_LONG).show()
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Actively verify audio recording permissions on app launch
    checkAndRequestPermissions()

    setContent {
      MyApplicationTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          PanchayatApp(viewModel = viewModel)
        }
      }
    }
  }

  private fun checkAndRequestPermissions() {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
        != PackageManager.PERMISSION_GRANTED) {
      requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
  }
}

