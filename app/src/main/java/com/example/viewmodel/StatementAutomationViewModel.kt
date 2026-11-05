package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AutomationConfigDto
import com.example.data.model.ConfirmPendingTransactionRequest
import com.example.data.model.DismissPendingTransactionRequest
import com.example.data.model.PendingTransactionDto
import com.example.data.model.RailwayPairRequest
import com.example.data.model.RailwayStatusResponse
import com.example.data.model.RealBalanceResponse
import com.example.data.model.TransactionType
import com.example.data.model.UpdateAutomationConfigRequest
import com.example.data.remote.NetworkClient
import com.example.data.repository.FinancialRepository
import com.example.util.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StatementAutomationViewModel(application: Application) : AndroidViewModel(application) {
  val repository = FinancialRepository(application)

  private val _pendingTransactions = MutableStateFlow<List<PendingTransactionDto>>(emptyList())
  val pendingTransactions: StateFlow<List<PendingTransactionDto>> = _pendingTransactions.asStateFlow()

  private val _realBalance = MutableStateFlow<RealBalanceResponse?>(null)
  val realBalance: StateFlow<RealBalanceResponse?> = _realBalance.asStateFlow()

  private val _automationConfig = MutableStateFlow<AutomationConfigDto?>(null)
  val automationConfig: StateFlow<AutomationConfigDto?> = _automationConfig.asStateFlow()

  private val _railwayStatus = MutableStateFlow<RailwayStatusResponse?>(null)
  val railwayStatus: StateFlow<RailwayStatusResponse?> = _railwayStatus.asStateFlow()

  private val _pairingCode = MutableStateFlow<String?>(null)
  val pairingCode: StateFlow<String?> = _pairingCode.asStateFlow()

  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  private val _statusMessage = MutableStateFlow<String?>(null)
  val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

  private val _errorMessage = MutableStateFlow<String?>(null)
  val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

  init {
    loadPendingTransactions()
    loadRealBalance()
    loadAutomationConfig()
  }

  fun loadPendingTransactions() {
    viewModelScope.launch {
      try {
        val res = NetworkClient.apiService.getPendingTransactions()
        if (res.isSuccessful && res.body()?.success == true) {
          val list = res.body()?.items ?: emptyList()
          val prevCount = _pendingTransactions.value.size
          _pendingTransactions.value = list

          // If new transactions arrived and app was in background or reconnected, trigger notification
          if (list.isNotEmpty() && list.size > prevCount) {
            val latest = list.firstOrNull()
            NotificationHelper.showPendingTransactionsNotification(
              getApplication(),
              list.size,
              latest?.amount
            )
          }
        }
      } catch (_: Exception) {}
    }
  }

  fun loadRealBalance() {
    viewModelScope.launch {
      try {
        val res = NetworkClient.apiService.getRealBalance()
        if (res.isSuccessful && res.body()?.success == true) {
          _realBalance.value = res.body()
        }
      } catch (_: Exception) {}
    }
  }

  fun loadAutomationConfig() {
    viewModelScope.launch {
      try {
        val res = NetworkClient.apiService.getAutomationConfig()
        if (res.isSuccessful && res.body()?.success == true) {
          val body = res.body()!!
          _automationConfig.value = AutomationConfigDto(
            sbiTargetPhone = body.sbiTargetPhone,
            intervalMinutes = body.intervalMinutes,
            isEnabled = body.isEnabled,
            railwayApiUrl = body.railwayApiUrl,
            lastRunAt = body.lastRunAt,
            lastStatus = body.lastStatus
          )
        }
      } catch (_: Exception) {}
    }
  }

  fun confirmPendingTransaction(
    item: PendingTransactionDto,
    categoryName: String,
    purpose: String?,
    onSuccess: () -> Unit
  ) {
    viewModelScope.launch {
      _isLoading.value = true
      _errorMessage.value = null
      try {
        val res = NetworkClient.apiService.confirmPendingTransaction(
          ConfirmPendingTransactionRequest(
            pendingTransactionId = item.id,
            categoryName = categoryName.trim(),
            notes = purpose?.trim()
          )
        )
        if (res.isSuccessful && res.body()?.success == true) {
          // Add to local database ledger as well
          val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
          val parsedDate = try {
            sdf.parse(item.transactionDate) ?: Date()
          } catch (_: Exception) {
            Date()
          }

          val txType = if (item.type.uppercase() == "CREDIT") TransactionType.INCOME else TransactionType.EXPENSE
          repository.addTransaction(
            type = txType,
            amount = item.amount,
            description = item.description,
            categoryName = categoryName.trim(),
            dateMillis = parsedDate.time,
            dateString = item.transactionDate,
            timeString = "12:00",
            notes = purpose?.trim()
          )

          _statusMessage.value = "Transaction confirmed & saved to ledger."
          loadPendingTransactions()
          loadRealBalance()
          onSuccess()
        } else {
          _errorMessage.value = res.body()?.message ?: "Failed to confirm transaction."
        }
      } catch (e: Exception) {
        _errorMessage.value = e.localizedMessage ?: "Network error during confirmation"
      } finally {
        _isLoading.value = false
      }
    }
  }

  fun dismissPendingTransaction(item: PendingTransactionDto) {
    viewModelScope.launch {
      try {
        NetworkClient.apiService.dismissPendingTransaction(
          DismissPendingTransactionRequest(pendingTransactionId = item.id)
        )
        loadPendingTransactions()
      } catch (_: Exception) {}
    }
  }

  fun updateAutomationConfig(
    intervalMinutes: Int,
    targetPhone: String,
    isEnabled: Boolean,
    railwayUrl: String?,
    onSuccess: () -> Unit
  ) {
    viewModelScope.launch {
      _isLoading.value = true
      _errorMessage.value = null
      try {
        val res = NetworkClient.apiService.updateAutomationConfig(
          UpdateAutomationConfigRequest(
            intervalMinutes = intervalMinutes,
            sbiTargetPhone = targetPhone.trim(),
            isEnabled = isEnabled,
            railwayApiUrl = railwayUrl?.trim()
          )
        )
        if (res.isSuccessful && res.body()?.success == true) {
          _statusMessage.value = "Automation configuration saved."
          loadAutomationConfig()
          onSuccess()
        } else {
          _errorMessage.value = res.body()?.message ?: "Failed to update configuration."
        }
      } catch (e: Exception) {
        _errorMessage.value = e.localizedMessage ?: "Failed to reach server."
      } finally {
        _isLoading.value = false
      }
    }
  }

  fun checkRailwayStatus(railwayUrl: String) {
    viewModelScope.launch {
      _isLoading.value = true
      try {
        val cleanUrl = formatEndpoint(railwayUrl, "api/whatsapp/status")
        val res = NetworkClient.apiService.getRailwayStatus(cleanUrl)
        if (res.isSuccessful && res.body() != null) {
          _railwayStatus.value = res.body()
        }
      } catch (e: Exception) {
        _errorMessage.value = "Could not contact Railway worker: ${e.localizedMessage}"
      } finally {
        _isLoading.value = false
      }
    }
  }

  fun requestPairingCode(railwayUrl: String, phoneNumber: String) {
    viewModelScope.launch {
      _isLoading.value = true
      _errorMessage.value = null
      _pairingCode.value = null
      try {
        val cleanUrl = formatEndpoint(railwayUrl, "api/whatsapp/pair")
        val res = NetworkClient.apiService.requestRailwayPairCode(
          cleanUrl,
          RailwayPairRequest(phoneNumber = phoneNumber.trim())
        )
        if (res.isSuccessful && res.body()?.success == true) {
          _pairingCode.value = res.body()?.pairingCode
          _statusMessage.value = "Pairing code generated! Enter it into WhatsApp Linked Devices."
        } else {
          _errorMessage.value = res.body()?.error ?: "Failed to generate pairing code."
        }
      } catch (e: Exception) {
        _errorMessage.value = "Failed to contact Railway worker: ${e.localizedMessage}"
      } finally {
        _isLoading.value = false
      }
    }
  }

  fun disconnectRailway(railwayUrl: String) {
    viewModelScope.launch {
      _isLoading.value = true
      try {
        val cleanUrl = formatEndpoint(railwayUrl, "api/whatsapp/disconnect")
        NetworkClient.apiService.disconnectRailway(cleanUrl)
        checkRailwayStatus(railwayUrl)
        _statusMessage.value = "WhatsApp disconnected on Railway."
      } catch (e: Exception) {
        _errorMessage.value = e.localizedMessage
      } finally {
        _isLoading.value = false
      }
    }
  }

  fun triggerAutomationNow(railwayUrl: String) {
    viewModelScope.launch {
      _isLoading.value = true
      _statusMessage.value = "Requesting SBI Mini Statement cycle on Railway..."
      try {
        val cleanUrl = formatEndpoint(railwayUrl, "api/automation/run-now")
        val res = NetworkClient.apiService.runRailwayAutomationNow(cleanUrl)
        if (res.isSuccessful) {
          _statusMessage.value = "Cycle triggered! Statement will be parsed shortly."
          loadPendingTransactions()
          loadRealBalance()
        } else {
          _errorMessage.value = "Railway cycle trigger failed."
        }
      } catch (e: Exception) {
        _errorMessage.value = e.localizedMessage
      } finally {
        _isLoading.value = false
      }
    }
  }

  fun clearMessages() {
    _statusMessage.value = null
    _errorMessage.value = null
  }

  private fun formatEndpoint(baseUrl: String, path: String): String {
    val clean = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
    return clean + path
  }
}
