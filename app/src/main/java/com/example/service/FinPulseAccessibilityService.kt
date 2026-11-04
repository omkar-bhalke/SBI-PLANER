package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.ui.screens.restriction.AppRestrictionActivity
import com.example.ui.screens.restriction.AppRestrictionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class FinPulseAccessibilityService : AccessibilityService() {
  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
  private var lastTriggeredPackage: String? = null
  private var lastTriggeredTimestamp: Long = 0L

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
      return
    }

    val packageName = event.packageName?.toString() ?: return

    // Never intercept own app, system UI, launcher, phone/dialer, or settings
    if (packageName == applicationContext.packageName ||
        packageName.startsWith("com.android.systemui") ||
        packageName.startsWith("com.android.settings") ||
        packageName.contains("dialer") ||
        packageName.contains("emergency")
    ) {
      return
    }

    // Check if this package is restricted by the user
    if (!AppRestrictionManager.isPackageRestricted(applicationContext, packageName)) {
      return
    }

    // Debounce triggers (prevent loop within 3 seconds)
    val now = System.currentTimeMillis()
    if (packageName == lastTriggeredPackage && (now - lastTriggeredTimestamp) < 3000L) {
      return
    }

    lastTriggeredPackage = packageName
    lastTriggeredTimestamp = now

    // Check if daily spending limit is exceeded
    serviceScope.launch {
      val (isExceeded, spentToday, dailyLimit) = AppRestrictionManager.checkSpendingLimitExceeded(applicationContext)

      if (isExceeded) {
        // Log event
        AppRestrictionManager.logEvent(
          context = applicationContext,
          packageName = packageName,
          appName = getAppNameForPackage(packageName),
          dailyLimit = dailyLimit,
          amountSpent = spentToday,
          actionTaken = "BLOCKED"
        )

        // Launch restriction alert screen
        val intent = Intent(applicationContext, AppRestrictionActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
          putExtra(AppRestrictionActivity.EXTRA_PACKAGE_NAME, packageName)
          putExtra(AppRestrictionActivity.EXTRA_APP_NAME, getAppNameForPackage(packageName))
          putExtra(AppRestrictionActivity.EXTRA_SPENT_TODAY, spentToday)
          putExtra(AppRestrictionActivity.EXTRA_DAILY_LIMIT, dailyLimit)
        }
        startActivity(intent)
      }
    }
  }

  private fun getAppNameForPackage(packageName: String): String {
    return try {
      val pm = packageManager
      val info = pm.getApplicationInfo(packageName, 0)
      pm.getApplicationLabel(info).toString()
    } catch (e: Exception) {
      packageName
    }
  }

  override fun onInterrupt() {
    Log.d("FinPulseService", "Accessibility service interrupted.")
  }

  override fun onDestroy() {
    super.onDestroy()
    serviceScope.cancel()
  }
}
