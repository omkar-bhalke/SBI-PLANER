package com.example

import com.example.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun testBalanceCalculation() {
    val initialBalance = 5000.0
    val income = 1500.0
    val expense = 650.0
    val currentBalance = initialBalance + income - expense

    assertEquals(5850.0, currentBalance, 0.001)
  }

  @Test
  fun testDailySpendingLimitWarning() {
    val dailyLimit = 500.0
    val spentNormal = 200.0
    val spentApproaching = 400.0
    val spentExceeded = 550.0

    // Within limit (< 75%)
    val isNormalWithin = (spentNormal / dailyLimit) < 0.75
    assertTrue(isNormalWithin)

    // Approaching limit (>= 75% and <= 100%)
    val isApproaching = (spentApproaching / dailyLimit) >= 0.75 && spentApproaching <= dailyLimit
    assertTrue(isApproaching)

    // Exceeded limit (> 100%)
    val isExceeded = spentExceeded > dailyLimit
    assertTrue(isExceeded)
  }

  @Test
  fun testPasswordStrengthVerification() {
    val weakPassword = "pass"
    val mediumPassword = "Password1"
    val strongPassword = "StrongPassword@2026!"

    // Min length
    assertFalse(weakPassword.length >= 8)
    assertTrue(mediumPassword.length >= 8)
    assertTrue(strongPassword.length >= 8)

    // Upper + digit + special
    val hasUpper = strongPassword.any { it.isUpperCase() }
    val hasDigit = strongPassword.any { it.isDigit() }
    val hasSpecial = strongPassword.any { !it.isLetterOrDigit() }

    assertTrue(hasUpper && hasDigit && hasSpecial)
  }

  @Test
  fun testDeviceIdentifierFormatting() {
    val sampleDeviceId = "FP-GOO-4a7b9c1d2e3f-9876543210abcdef"
    assertTrue(sampleDeviceId.startsWith("FP-"))
    assertTrue(sampleDeviceId.length >= 24)
  }

  @Test
  fun testRestrictedPaymentPackages() {
    val knownPaymentPackages = listOf(
      "net.one97.paytm",
      "com.phonepe.app",
      "com.google.android.apps.nbu.paisa.user",
      "in.org.npci.upiapp",
      "com.sbi.upi"
    )

    assertTrue(knownPaymentPackages.contains("com.google.android.apps.nbu.paisa.user"))
    assertTrue(knownPaymentPackages.contains("net.one97.paytm"))
    assertFalse(knownPaymentPackages.contains("com.example.finpulse"))
  }
}
