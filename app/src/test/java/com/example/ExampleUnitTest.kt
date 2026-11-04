package com.example

import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import org.junit.Assert.assertEquals
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
}
