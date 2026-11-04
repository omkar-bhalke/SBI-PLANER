package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.DeviceResetRequest
import com.example.data.model.ForgotPasswordRequest
import com.example.data.model.LoginRequest
import com.example.data.model.RegisterRequest
import com.example.data.model.ResetPasswordRequest
import com.example.data.model.UserSettings
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

class MainViewModel(application: Application) : AndroidViewModel(application) {
  val repository = FinancialRepository(application)

  val userSettings: StateFlow<UserSettings> = repository.userSettings.stateIn(
    scope = viewModelScope,
    started = SharingStarted.Eagerly,
    initialValue = UserSettings()
  )

  private val _syncMessage = MutableStateFlow<String?>(null)
  val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

  private val _authError = MutableStateFlow<String?>(null)
  val authError: StateFlow<String?> = _authError.asStateFlow()

  val deviceId: String by lazy {
    DeviceSecurityManager.getOrCreateDeviceId(application)
  }

  init {
    // Establish hardware/keystore device identity into network client
    NetworkClient.setDeviceId(deviceId)

    viewModelScope.launch {
      userSettings.collect { settings ->
        NetworkClient.setAuthToken(settings.authToken)
        NetworkClient.setRefreshToken(settings.refreshToken)
        NetworkClient.setBaseUrl(settings.backendUrl)
        if (settings.deviceId.isNotBlank()) {
          NetworkClient.setDeviceId(settings.deviceId)
        } else {
          NetworkClient.setDeviceId(deviceId)
        }
      }
    }

    // Trigger initial sync and cloud data restore if online
    viewModelScope.launch {
      val syncedCount = repository.syncManager.syncPendingTransactions()
      if (syncedCount > 0) {
        _syncMessage.value = "Synced $syncedCount pending records with cPanel cloud backend"
      }
      try {
        if (!userSettings.value.authToken.isNullOrBlank()) {
          val restored = repository.restoreFromCloud()
          if (restored > 0) {
            _syncMessage.value = "Synchronized $restored records from cPanel database"
          }
        }
      } catch (_: Exception) {}
    }
  }

  fun clearSyncMessage() {
    _syncMessage.value = null
  }

  fun clearAuthError() {
    _authError.value = null
  }

  fun register(
    name: String,
    email: String,
    pass: String,
    confirmPass: String,
    onSuccess: (isFirstTime: Boolean) -> Unit,
    onError: (String) -> Unit
  ) {
    viewModelScope.launch {
      try {
        val app = getApplication<Application>()
        val devFingerprint = DeviceSecurityManager.getDeviceFingerprint(app)
        val devModel = DeviceSecurityManager.getDeviceModel()
        val osVer = DeviceSecurityManager.getOsVersion()

        val request = RegisterRequest(
          name = name.trim(),
          email = email.trim(),
          password = pass,
          confirmPassword = confirmPass,
          deviceId = deviceId,
          deviceFingerprint = devFingerprint,
          deviceModel = devModel,
          osVersion = osVer
        )

        val response = NetworkClient.apiService.register(request)
        if (response.isSuccessful && response.body()?.success == true) {
          val body = response.body()!!
          val user = body.user
          repository.preferenceManager.saveAuthProfile(
            id = user?.id ?: "usr_${System.currentTimeMillis()}",
            email = user?.email ?: email.trim(),
            name = user?.name ?: name.trim(),
            photoUrl = user?.profileImage,
            token = body.token,
            refreshToken = body.refreshToken,
            deviceId = deviceId
          )
          NetworkClient.setAuthToken(body.token)
          NetworkClient.setRefreshToken(body.refreshToken)
          NetworkClient.setDeviceId(deviceId)
          onSuccess(body.isFirstTime)
        } else {
          val errJson = response.errorBody()?.string()
          val msg = parseErrorMessage(errJson) ?: response.body()?.message ?: "Registration failed."
          _authError.value = msg
          onError(msg)
        }
      } catch (e: Exception) {
        val msg = "Network error: ${e.localizedMessage ?: "Unable to contact authentication server"}"
        _authError.value = msg
        onError(msg)
      }
    }
  }

  fun loginWithEmailPassword(
    email: String,
    pass: String,
    rememberMe: Boolean,
    onSuccess: (isFirstTime: Boolean) -> Unit,
    onError: (String) -> Unit
  ) {
    viewModelScope.launch {
      try {
        val app = getApplication<Application>()
        val devFingerprint = DeviceSecurityManager.getDeviceFingerprint(app)

        val request = LoginRequest(
          email = email.trim(),
          password = pass,
          rememberMe = rememberMe,
          deviceId = deviceId,
          deviceFingerprint = devFingerprint
        )

        val response = NetworkClient.apiService.login(request)
        if (response.isSuccessful && response.body()?.success == true) {
          val body = response.body()!!
          val user = body.user
          repository.preferenceManager.saveAuthProfile(
            id = user?.id ?: "usr_${System.currentTimeMillis()}",
            email = user?.email ?: email.trim(),
            name = user?.name ?: "FinPulse User",
            photoUrl = user?.profileImage,
            token = body.token,
            refreshToken = body.refreshToken,
            deviceId = deviceId
          )
          NetworkClient.setAuthToken(body.token)
          NetworkClient.setRefreshToken(body.refreshToken)
          NetworkClient.setDeviceId(deviceId)

          // Restore previously saved data from cPanel cloud database (Requirement 5)
          try {
            val restoredCount = repository.restoreFromCloud()
            if (restoredCount > 0) {
              _syncMessage.value = "Restored $restoredCount records from cPanel database."
            }
          } catch (_: Exception) {}

          onSuccess(body.isFirstTime)
        } else {
          val errJson = response.errorBody()?.string()
          val msg = parseErrorMessage(errJson) ?: response.body()?.message ?: "Login failed. Check your credentials."
          _authError.value = msg
          onError(msg)
        }
      } catch (e: Exception) {
        val msg = "Connection error: ${e.localizedMessage ?: "Failed to reach server"}"
        _authError.value = msg
        onError(msg)
      }
    }
  }

  fun forgotPassword(email: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
    viewModelScope.launch {
      try {
        val response = NetworkClient.apiService.forgotPassword(ForgotPasswordRequest(email = email.trim()))
        if (response.isSuccessful && response.body()?.success == true) {
          onSuccess(response.body()?.message ?: "Password reset instructions sent.")
        } else {
          val errJson = response.errorBody()?.string()
          val msg = parseErrorMessage(errJson) ?: "Failed to process forgot password request."
          onError(msg)
        }
      } catch (e: Exception) {
        onError("Network error: ${e.localizedMessage}")
      }
    }
  }

  fun resetPassword(
    email: String,
    otp: String,
    newPass: String,
    confirmPass: String,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
  ) {
    viewModelScope.launch {
      try {
        val response = NetworkClient.apiService.resetPassword(
          ResetPasswordRequest(
            email = email.trim(),
            otp = otp.trim(),
            newPassword = newPass,
            confirmPassword = confirmPass
          )
        )
        if (response.isSuccessful && response.body()?.success == true) {
          onSuccess(response.body()?.message ?: "Password reset successfully.")
        } else {
          val errJson = response.errorBody()?.string()
          val msg = parseErrorMessage(errJson) ?: "Failed to reset password."
          onError(msg)
        }
      } catch (e: Exception) {
        onError("Network error: ${e.localizedMessage}")
      }
    }
  }

  fun resetDevice(
    email: String,
    pass: String,
    reason: String?,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
  ) {
    viewModelScope.launch {
      try {
        val response = NetworkClient.apiService.requestDeviceReset(
          DeviceResetRequest(
            email = email.trim(),
            password = pass,
            deviceId = deviceId,
            reason = reason
          )
        )
        if (response.isSuccessful && response.body()?.success == true) {
          onSuccess()
        } else {
          val errJson = response.errorBody()?.string()
          val msg = parseErrorMessage(errJson) ?: "Device reset unbinding failed."
          onError(msg)
        }
      } catch (e: Exception) {
        onError("Network error: ${e.localizedMessage}")
      }
    }
  }

  fun authenticateUser(id: String, email: String, name: String, photoUrl: String?, token: String?) {
    viewModelScope.launch {
      repository.preferenceManager.saveAuthProfile(
        id = id,
        email = email,
        name = name,
        photoUrl = photoUrl,
        token = token,
        refreshToken = null,
        deviceId = deviceId
      )
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
      NetworkClient.setRefreshToken(null)
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
