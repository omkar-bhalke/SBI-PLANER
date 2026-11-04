package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.UserSettings
import com.example.data.remote.NetworkClient
import com.example.data.repository.FinancialRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
  val repository = FinancialRepository(application)

  val userSettings: StateFlow<UserSettings> = repository.userSettings.stateIn(
    scope = viewModelScope,
    started = SharingStarted.Eagerly,
    initialValue = UserSettings()
  )

  private val _syncMessage = MutableStateFlow<String?>(null)
  val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

  init {
    viewModelScope.launch {
      userSettings.collect { settings ->
        NetworkClient.setAuthToken(settings.authToken)
        NetworkClient.setBaseUrl(settings.backendUrl)
      }
    }

    // Trigger initial sync if online
    viewModelScope.launch {
      val syncedCount = repository.syncManager.syncPendingTransactions()
      if (syncedCount > 0) {
        _syncMessage.value = "Synced $syncedCount pending records with cloud backend"
      }
    }
  }

  fun clearSyncMessage() {
    _syncMessage.value = null
  }

  fun authenticateUser(id: String, email: String, name: String, photoUrl: String?, token: String?) {
    viewModelScope.launch {
      repository.preferenceManager.saveAuthProfile(id, email, name, photoUrl, token)
      NetworkClient.setAuthToken(token)
    }
  }

  fun completeOnboarding(initialBalance: Double, bankName: String, dailyLimit: Double) {
    viewModelScope.launch {
      repository.preferenceManager.saveInitialFinancialSetup(initialBalance, bankName, dailyLimit)
    }
  }

  fun logout() {
    viewModelScope.launch {
      repository.preferenceManager.clearUserSession()
      NetworkClient.setAuthToken(null)
    }
  }
}
