package com.example.ui.screens.restriction

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.example.data.local.AppDatabase
import com.example.data.local.PreferenceManager
import com.example.data.model.AppRestrictionItem
import com.example.data.model.LogRestrictionEventRequest
import com.example.data.model.TransactionType
import com.example.data.remote.NetworkClient
import com.example.service.FinPulseAccessibilityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppRestrictionManager {
  private const val PREFS_NAME = "finpulse_app_restrictions"
  private const val KEY_RESTRICTION_ENABLED = "restriction_feature_enabled"

  // Standard popular Indian payment packages
  val KNOWN_PAYMENT_PACKAGES = listOf(
    AppRestrictionItem("com.google.android.apps.nbu.paisa.user", "Google Pay", true),
    AppRestrictionItem("com.phonepe.app", "PhonePe", true),
    AppRestrictionItem("net.one97.paytm", "Paytm", true),
    AppRestrictionItem("in.org.npci.upiapp", "BHIM UPI", true),
    AppRestrictionItem("com.dreamplug.androidapp", "CRED", true),
    AppRestrictionItem("in.amazon.mShop.android.shopping", "Amazon Pay", false),
    AppRestrictionItem("com.whatsapp", "WhatsApp Payments", false),
    AppRestrictionItem("com.sbi.lotusintouch", "YONO SBI", true),
    AppRestrictionItem("com.snapwork.hdfc", "HDFC MobileBanking", true),
    AppRestrictionItem("com.csam.icici.bank.imobile", "iMobile Pay", true)
  )

  fun isFeatureEnabled(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_RESTRICTION_ENABLED, true)
  }

  fun setFeatureEnabled(context: Context, enabled: Boolean) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putBoolean(KEY_RESTRICTION_ENABLED, enabled).apply()
  }

  fun isPackageRestricted(context: Context, packageName: String): Boolean {
    if (!isFeatureEnabled(context)) return false
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean("pkg_$packageName", true)
  }

  fun setPackageRestricted(context: Context, packageName: String, restricted: Boolean) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putBoolean("pkg_$packageName", restricted).apply()
  }

  /**
   * Checks whether the FinPulse Accessibility Service is currently active in Android settings.
   */
  fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
    val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
    val expectedComponentName = "${context.packageName}/${FinPulseAccessibilityService::class.java.canonicalName}"
    return enabledServices.any { serviceInfo ->
      serviceInfo.id.equals(expectedComponentName, ignoreCase = true) ||
      serviceInfo.resolveInfo?.serviceInfo?.name.equals(FinPulseAccessibilityService::class.java.canonicalName, ignoreCase = true)
    }
  }

  /**
   * Intent helper to direct user to Android Accessibility Settings screen
   */
  fun openAccessibilitySettings(context: Context) {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
  }

  /**
   * Checks if today's expenses exceed the configured daily spending limit.
   */
  suspend fun checkSpendingLimitExceeded(context: Context): Triple<Boolean, Double, Double> = withContext(Dispatchers.IO) {
    try {
      val db = AppDatabase.getDatabase(context)
      val prefs = PreferenceManager(context)
      val settings = prefs.userSettingsFlow.first()
      val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

      val todayExpenses = db.transactionDao().getTodayExpensesFlow(todayDate).first()
      val dailyLimit = settings.dailySpendingLimit

      val isExceeded = todayExpenses > dailyLimit
      Triple(isExceeded, todayExpenses, dailyLimit)
    } catch (e: Exception) {
      Triple(false, 0.0, 500.0)
    }
  }

  /**
   * Logs interception event locally & to cloud backend
   */
  suspend fun logEvent(
    context: Context,
    packageName: String,
    appName: String,
    dailyLimit: Double,
    amountSpent: Double,
    actionTaken: String = "BLOCKED"
  ) = withContext(Dispatchers.IO) {
    try {
      val exceeded = (amountSpent - dailyLimit).coerceAtLeast(0.0)
      NetworkClient.apiService.logRestrictionEvent(
        LogRestrictionEventRequest(
          packageName = packageName,
          appName = appName,
          dailyLimit = dailyLimit,
          amountSpent = amountSpent,
          amountExceeded = exceeded,
          actionTaken = actionTaken
        )
      )
    } catch (_: Exception) {}
  }
}
