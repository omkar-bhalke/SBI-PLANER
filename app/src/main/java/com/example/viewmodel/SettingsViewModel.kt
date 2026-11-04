package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CategoryEntity
import com.example.data.model.TransactionType
import com.example.data.model.UserSettings
import com.example.data.remote.NetworkClient
import com.example.data.repository.FinancialRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
  val repository = FinancialRepository(application)

  val userSettings: StateFlow<UserSettings> = repository.userSettings.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = UserSettings()
  )

  val categories: StateFlow<List<CategoryEntity>> = repository.allCategories.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  private val _statusMessage = MutableStateFlow<String?>(null)
  val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

  fun updateDailyLimit(limit: Double) {
    viewModelScope.launch {
      repository.preferenceManager.updateDailySpendingLimit(limit)
      _statusMessage.value = "Daily limit updated to ₹${limit.toInt()}"
    }
  }

  fun updateInitialBalance(balance: Double) {
    viewModelScope.launch {
      repository.preferenceManager.updateInitialBalance(balance)
      _statusMessage.value = "Initial balance updated to ₹${balance.toInt()}"
    }
  }

  fun updateBank(bank: String) {
    viewModelScope.launch {
      repository.preferenceManager.updateSelectedBank(bank)
      _statusMessage.value = "Selected bank updated to $bank"
    }
  }

  fun updateTheme(themeMode: String) {
    viewModelScope.launch {
      repository.preferenceManager.updateThemeMode(themeMode)
    }
  }

  fun updateAiProvider(provider: String) {
    viewModelScope.launch {
      repository.preferenceManager.updateAiProvider(provider)
      _statusMessage.value = "AI Provider set to $provider"
    }
  }

  fun addCustomCategory(name: String, type: TransactionType, colorHex: String, iconName: String) {
    viewModelScope.launch {
      repository.addCategory(name, type, colorHex, iconName)
      _statusMessage.value = "Category \"$name\" added"
    }
  }

  fun deleteCategory(category: CategoryEntity) {
    viewModelScope.launch {
      repository.deleteCategory(category)
      _statusMessage.value = "Category \"${category.name}\" deleted"
    }
  }

  fun connectWhatsApp(phoneNumber: String) {
    viewModelScope.launch {
      repository.preferenceManager.updateWhatsAppStatus(true, phoneNumber)
      _statusMessage.value = "WhatsApp linked with $phoneNumber"
    }
  }

  fun disconnectWhatsApp() {
    viewModelScope.launch {
      repository.preferenceManager.updateWhatsAppStatus(false, null)
      _statusMessage.value = "WhatsApp disconnected"
    }
  }

  fun clearStatusMessage() {
    _statusMessage.value = null
  }
}
