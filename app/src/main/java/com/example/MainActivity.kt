package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.ui.navigation.FinPulseNavHost
import com.example.ui.theme.FinPulseTheme
import com.example.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
  private val mainViewModel: MainViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val userSettings by mainViewModel.userSettings.collectAsState()
      val systemInDark = isSystemInDarkTheme()
      val isDark = when (userSettings.themeMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> systemInDark
      }

      FinPulseTheme(darkTheme = isDark) {
        FinPulseNavHost(mainViewModel = mainViewModel)
      }
    }
  }
}
