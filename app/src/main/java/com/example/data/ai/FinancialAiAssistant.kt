package com.example.data.ai

import com.example.data.model.AiAnalysisRequest
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.remote.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

class FinancialAiAssistant {

  suspend fun generateAnalysis(
    transactions: List<TransactionEntity>,
    customPrompt: String? = null,
    dailyLimit: Double = 500.0
  ): String = withContext(Dispatchers.IO) {
    // Attempt remote backend AI endpoint first if connected
    try {
      val response = NetworkClient.apiService.analyzeSpending(AiAnalysisRequest(customQuery = customPrompt))
      if (response.isSuccessful && response.body()?.success == true) {
        val analysis = response.body()?.analysis
        if (!analysis.isNullOrBlank()) {
          return@withContext analysis
        }
      }
    } catch (_: Exception) {
      // Fallback to on-device analytical synthesis grounded in actual transactions
    }

    // On-device AI Analysis Engine grounded in actual ledger records
    if (transactions.isEmpty()) {
      return@withContext "You don't have any recorded transactions yet. Once you log your daily expenses (like tea, meals, printing, or transportation), I will analyze your spending habits, identify repeated purchases, and help you save money!"
    }

    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    val expenseItems = transactions.filter { it.type == TransactionType.EXPENSE }
    val totalExpense = expenseItems.sumOf { it.amount }
    val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }

    // Category aggregation
    val categoryTotals = expenseItems.groupBy { it.categoryName }
      .mapValues { entry -> entry.value.sumOf { it.amount } }
      .toList()
      .sortedByDescending { it.second }

    val topCategory = categoryTotals.firstOrNull() ?: Pair("None", 0.0)
    val topCatPercent = if (totalExpense > 0) ((topCategory.second / totalExpense) * 100).toInt() else 0

    // Repeated purchases detector (case-insensitive description matching)
    val repeatedMap = mutableMapOf<String, MutableList<TransactionEntity>>()
    expenseItems.forEach { item ->
      val key = item.description.trim().lowercase()
      repeatedMap.getOrPut(key) { mutableListOf() }.add(item)
    }

    val repeatedPurchases = repeatedMap.values
      .filter { it.size >= 2 }
      .sortedByDescending { list -> list.sumOf { it.amount } }

    val potentialSavings = (totalExpense * 0.15).toInt()

    val sb = StringBuilder()
    sb.append("📊 **AI Financial Intelligence Report**\n\n")

    if (!customPrompt.isNullOrBlank()) {
      sb.append("Regarding your inquiry: *\"$customPrompt\"*\n\n")
    }

    sb.append("• **Total Spending Ledger**: ${currencyFormat.format(totalExpense)} tracked across ${expenseItems.size} expense entries.\n")
    if (totalIncome > 0) {
      val netSavings = totalIncome - totalExpense
      sb.append("• **Net Cashflow**: Income of ${currencyFormat.format(totalIncome)} with net balance difference of ${currencyFormat.format(netSavings)}.\n")
    }

    sb.append("• **Primary Outflow Driver**: **${topCategory.first}** accounts for ${currencyFormat.format(topCategory.second)} ($topCatPercent% of all expenses).\n")

    if (repeatedPurchases.isNotEmpty()) {
      val topRepeated = repeatedPurchases.first()
      val repeatedName = topRepeated.first().description
      val repeatedCount = topRepeated.size
      val repeatedTotal = topRepeated.sumOf { it.amount }
      sb.append("• **Repeated Micro-Expenses Detected**: You purchased **\"$repeatedName\"** $repeatedCount times, totaling ${currencyFormat.format(repeatedTotal)}.\n")
    }

    sb.append("\n💡 **Actionable Spending Advice**:\n")
    if (topCatPercent > 40) {
      sb.append("1. Over 40% of your budget is absorbed by **${topCategory.first}**. Consider establishing a dedicated sub-budget for this category.\n")
    }
    if (repeatedPurchases.isNotEmpty()) {
      sb.append("2. Trimming daily micro-purchases (like frequent snacks, coffee, or incidental printing) can comfortably preserve **${currencyFormat.format(potentialSavings)}** this month.\n")
    }
    sb.append("3. Stay mindful of your daily spending limit (${currencyFormat.format(dailyLimit)}/day) to prevent end-of-month budget strain.")

    return@withContext sb.toString()
  }
}
