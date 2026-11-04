package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "finpulse_preferences")

class PreferenceManager(private val context: Context) {

  companion object {
    private val KEY_USER_ID = stringPreferencesKey("user_id")
    private val KEY_USER_EMAIL = stringPreferencesKey("user_email")
    private val KEY_USER_NAME = stringPreferencesKey("user_name")
    private val KEY_USER_PHOTO = stringPreferencesKey("user_photo")
    private val KEY_AUTH_TOKEN = stringPreferencesKey("auth_token")
    private val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    private val KEY_DEVICE_ID = stringPreferencesKey("device_id")

    private val KEY_INITIAL_BALANCE = doublePreferencesKey("initial_balance")
    private val KEY_DAILY_LIMIT = doublePreferencesKey("daily_limit")
    private val KEY_SELECTED_BANK = stringPreferencesKey("selected_bank")
    private val KEY_CURRENCY = stringPreferencesKey("currency")
    private val KEY_CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")

    private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    private val KEY_AI_PROVIDER = stringPreferencesKey("ai_provider")
    private val KEY_IS_AI_CONFIGURED = booleanPreferencesKey("is_ai_configured")
    private val KEY_BACKEND_URL = stringPreferencesKey("backend_url")

    private val KEY_WHATSAPP_CONNECTED = booleanPreferencesKey("whatsapp_connected")
    private val KEY_WHATSAPP_PHONE = stringPreferencesKey("whatsapp_phone")
    private val KEY_ACCESSIBILITY_ENABLED = booleanPreferencesKey("accessibility_enabled")
  }

  val userSettingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
    UserSettings(
      userId = prefs[KEY_USER_ID] ?: "",
      userEmail = prefs[KEY_USER_EMAIL] ?: "",
      userName = prefs[KEY_USER_NAME] ?: "",
      userPhotoUrl = prefs[KEY_USER_PHOTO],
      currency = prefs[KEY_CURRENCY] ?: "INR",
      currencySymbol = prefs[KEY_CURRENCY_SYMBOL] ?: "₹",
      initialBalance = prefs[KEY_INITIAL_BALANCE] ?: 0.0,
      dailySpendingLimit = prefs[KEY_DAILY_LIMIT] ?: 500.0,
      selectedBank = prefs[KEY_SELECTED_BANK] ?: "State Bank of India (SBI)",
      onboardingCompleted = prefs[KEY_ONBOARDING_COMPLETED] ?: false,
      themeMode = prefs[KEY_THEME_MODE] ?: "SYSTEM",
      aiProvider = prefs[KEY_AI_PROVIDER] ?: "GEMINI",
      isAiConfigured = prefs[KEY_IS_AI_CONFIGURED] ?: false,
      backendUrl = prefs[KEY_BACKEND_URL] ?: "https://radhewins.buzz",
      authToken = prefs[KEY_AUTH_TOKEN],
      refreshToken = prefs[KEY_REFRESH_TOKEN],
      deviceId = prefs[KEY_DEVICE_ID] ?: "",
      isWhatsAppConnected = prefs[KEY_WHATSAPP_CONNECTED] ?: false,
      whatsAppPhoneNumber = prefs[KEY_WHATSAPP_PHONE],
      accessibilityServiceEnabled = prefs[KEY_ACCESSIBILITY_ENABLED] ?: false
    )
  }

  suspend fun saveAuthProfile(
    id: String,
    email: String,
    name: String,
    photoUrl: String?,
    token: String?,
    refreshToken: String?,
    deviceId: String?
  ) {
    context.dataStore.edit { prefs ->
      prefs[KEY_USER_ID] = id
      prefs[KEY_USER_EMAIL] = email
      prefs[KEY_USER_NAME] = name
      photoUrl?.let { prefs[KEY_USER_PHOTO] = it } ?: prefs.remove(KEY_USER_PHOTO)
      token?.let { prefs[KEY_AUTH_TOKEN] = it } ?: prefs.remove(KEY_AUTH_TOKEN)
      refreshToken?.let { prefs[KEY_REFRESH_TOKEN] = it } ?: prefs.remove(KEY_REFRESH_TOKEN)
      deviceId?.let { prefs[KEY_DEVICE_ID] = it }
    }
  }

  suspend fun saveInitialFinancialSetup(initialBalance: Double, bankName: String, dailyLimit: Double) {
    context.dataStore.edit { prefs ->
      prefs[KEY_INITIAL_BALANCE] = initialBalance
      prefs[KEY_SELECTED_BANK] = bankName
      prefs[KEY_DAILY_LIMIT] = dailyLimit
      prefs[KEY_ONBOARDING_COMPLETED] = true
    }
  }

  suspend fun restoreCloudFinancialSetup(
    dailyLimit: Double,
    initialBalance: Double,
    bankName: String,
    onboardingDone: Boolean
  ) {
    context.dataStore.edit { prefs ->
      prefs[KEY_DAILY_LIMIT] = dailyLimit
      prefs[KEY_INITIAL_BALANCE] = initialBalance
      prefs[KEY_SELECTED_BANK] = bankName
      prefs[KEY_ONBOARDING_COMPLETED] = onboardingDone
    }
  }

  suspend fun updateBackendUrl(url: String) {
    context.dataStore.edit { prefs ->
      prefs[KEY_BACKEND_URL] = url
    }
  }

  suspend fun updateDailySpendingLimit(newLimit: Double) {
    context.dataStore.edit { prefs ->
      prefs[KEY_DAILY_LIMIT] = newLimit
    }
  }

  suspend fun updateInitialBalance(balance: Double) {
    context.dataStore.edit { prefs ->
      prefs[KEY_INITIAL_BALANCE] = balance
    }
  }

  suspend fun updateSelectedBank(bank: String) {
    context.dataStore.edit { prefs ->
      prefs[KEY_SELECTED_BANK] = bank
    }
  }

  suspend fun updateThemeMode(mode: String) {
    context.dataStore.edit { prefs ->
      prefs[KEY_THEME_MODE] = mode
    }
  }

  suspend fun updateAiProvider(provider: String) {
    context.dataStore.edit { prefs ->
      prefs[KEY_AI_PROVIDER] = provider
    }
  }

  suspend fun setAiConfigured(isConfigured: Boolean) {
    context.dataStore.edit { prefs ->
      prefs[KEY_IS_AI_CONFIGURED] = isConfigured
    }
  }

  suspend fun setAccessibilityServiceEnabled(enabled: Boolean) {
    context.dataStore.edit { prefs ->
      prefs[KEY_ACCESSIBILITY_ENABLED] = enabled
    }
  }

  suspend fun setOnboardingCompleted(completed: Boolean) {
    context.dataStore.edit { prefs ->
      prefs[KEY_ONBOARDING_COMPLETED] = completed
    }
  }

  suspend fun updateWhatsAppStatus(connected: Boolean, phoneNumber: String?) {
    context.dataStore.edit { prefs ->
      prefs[KEY_WHATSAPP_CONNECTED] = connected
      phoneNumber?.let { prefs[KEY_WHATSAPP_PHONE] = it } ?: prefs.remove(KEY_WHATSAPP_PHONE)
    }
  }

  suspend fun clearUserSession() {
    context.dataStore.edit { prefs ->
      prefs.remove(KEY_USER_ID)
      prefs.remove(KEY_USER_EMAIL)
      prefs.remove(KEY_USER_NAME)
      prefs.remove(KEY_USER_PHOTO)
      prefs.remove(KEY_AUTH_TOKEN)
      prefs.remove(KEY_REFRESH_TOKEN)
      prefs[KEY_ONBOARDING_COMPLETED] = false
    }
  }
}
