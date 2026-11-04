package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AiConfigDto
import com.example.data.model.CategoryEntity
import com.example.data.model.MetaWhatsAppConnectRequest
import com.example.data.model.TransactionType
import com.example.data.model.UserSettings
import com.example.data.model.ValidateAiKeyRequest
import com.example.data.model.WhatsAppStatusResponse
import com.example.data.remote.NetworkClient
import com.example.data.repository.FinancialRepository
import com.example.data.security.DeviceSecurityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject

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

  // Real AI Configuration State (Task 4)
  private val _aiConfig = MutableStateFlow<AiConfigDto?>(null)
  val aiConfig: StateFlow<AiConfigDto?> = _aiConfig.asStateFlow()

  // Real WhatsApp Integration State (Task 3)
  private val _whatsAppStatus = MutableStateFlow<WhatsAppStatusResponse?>(null)
  val whatsAppStatus: StateFlow<WhatsAppStatusResponse?> = _whatsAppStatus.asStateFlow()

  val deviceId: String by lazy {
    DeviceSecurityManager.getOrCreateDeviceId(application)
  }

  init {
    fetchAiConfig()
    fetchWhatsAppStatus()
  }

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

  fun clearStatusMessage() {
    _statusMessage.value = null
  }

  // ==========================================
  // REAL AI CONFIGURATION (TASK 4)
  // ==========================================

  fun fetchAiConfig() {
    viewModelScope.launch {
      try {
        val response = NetworkClient.apiService.getAiConfig()
        if (response.isSuccessful && response.body()?.success == true) {
          _aiConfig.value = response.body()?.config
          val isConfigured = response.body()?.config?.isConfigured == true
          repository.preferenceManager.setAiConfigured(isConfigured)
        }
      } catch (_: Exception) {}
    }
  }

  fun validateAndSaveAiConfig(
    provider: String,
    apiKey: String,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
  ) {
    viewModelScope.launch {
      try {
        val response = NetworkClient.apiService.validateAndSaveAiConfig(
          ValidateAiKeyRequest(provider = provider, apiKey = apiKey.trim())
        )
        if (response.isSuccessful && response.body()?.success == true) {
          _aiConfig.value = response.body()?.config
          repository.preferenceManager.setAiConfigured(true)
          repository.preferenceManager.updateAiProvider(provider)
          onSuccess(response.body()?.message ?: "AI API Key validated and saved securely!")
        } else {
          val errJson = response.errorBody()?.string()
          val msg = parseErrorMessage(errJson) ?: response.body()?.message ?: "API Key validation failed."
          onError(msg)
        }
      } catch (e: Exception) {
        onError("Network error: ${e.localizedMessage ?: "Failed to connect to backend"}")
      }
    }
  }

  // ==========================================
  // REAL WHATSAPP META CLOUD API (TASK 3)
  // ==========================================

  fun fetchWhatsAppStatus() {
    viewModelScope.launch {
      try {
        val response = NetworkClient.apiService.getWhatsAppStatus()
        if (response.isSuccessful) {
          _whatsAppStatus.value = response.body()
          val isConn = response.body()?.connected == true
          val phone = response.body()?.phoneNumber
          repository.preferenceManager.updateWhatsAppStatus(isConn, phone)
        }
      } catch (_: Exception) {}
    }
  }

  fun connectMetaWhatsApp(
    phoneNumber: String,
    accessToken: String?,
    phoneNumberId: String?,
    wabaId: String?,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
  ) {
    viewModelScope.launch {
      try {
        val response = NetworkClient.apiService.connectWhatsApp(
          MetaWhatsAppConnectRequest(
            phoneNumber = phoneNumber.trim(),
            accessToken = accessToken?.trim().takeIf { !it.isNullOrBlank() },
            phoneNumberId = phoneNumberId?.trim().takeIf { !it.isNullOrBlank() },
            wabaId = wabaId?.trim().takeIf { !it.isNullOrBlank() }
          )
        )
        if (response.isSuccessful && response.body()?.connected == true) {
          _whatsAppStatus.value = response.body()
          repository.preferenceManager.updateWhatsAppStatus(true, phoneNumber.trim())
          onSuccess()
        } else {
          val errJson = response.errorBody()?.string()
          val msg = parseErrorMessage(errJson) ?: response.body()?.message ?: "Meta Cloud API connection failed."
          onError(msg)
        }
      } catch (e: Exception) {
        onError("Connection error: ${e.localizedMessage ?: "Failed to contact integration gateway"}")
      }
    }
  }

  fun disconnectMetaWhatsApp(onSuccess: () -> Unit, onError: (String) -> Unit) {
    viewModelScope.launch {
      try {
        val response = NetworkClient.apiService.disconnectWhatsApp()
        if (response.isSuccessful && response.body()?.success == true) {
          repository.preferenceManager.updateWhatsAppStatus(false, null)
          fetchWhatsAppStatus()
          onSuccess()
        } else {
          val errJson = response.errorBody()?.string()
          val msg = parseErrorMessage(errJson) ?: "Failed to disconnect WhatsApp."
          onError(msg)
        }
      } catch (e: Exception) {
        onError("Network error: ${e.localizedMessage}")
      }
    }
  }

  private fun parseErrorMessage(json: String?): String? {
    if (json.isNullOrBlank()) return null
    return try {
      val obj = JSONObject(json)
      obj.optString("message").takeIf { it.isNotBlank() }
    } catch (_: Exception) {
      null
    }
  }
}
