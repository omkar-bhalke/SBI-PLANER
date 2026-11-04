package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.model.UserSettings
import com.example.data.repository.FinancialRepository
import com.example.ui.components.BarChartItem
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class AnalyticsPeriod {
  DAILY,
  WEEKLY,
  MONTHLY,
  YEARLY
}

data class AnalyticsUiState(
  val period: AnalyticsPeriod = AnalyticsPeriod.WEEKLY,
  val barChartItems: List<BarChartItem> = emptyList(),
  val categoryBreakdown: List<Pair<String, Double>> = emptyList(),
  val repeatedPurchases: List<Pair<String, Pair<Int, Double>>> = emptyList(),
  val totalExpense: Double = 0.0,
  val totalIncome: Double = 0.0,
  val averageDailyExpense: Double = 0.0,
  val highestSpendingDay: Pair<String, Double>? = null,
  val lowestSpendingDay: Pair<String, Double>? = null,
  val mostExpensiveCategory: Pair<String, Double>? = null,
  val limitViolationsCount: Int = 0,
  val dailyLimit: Double = 500.0,
  val totalSavings: Double = 0.0
)

class AnalyticsViewModel(application: Application) : AndroidViewModel(application) {
  val repository = FinancialRepository(application)

  private val _selectedPeriod = MutableStateFlow(AnalyticsPeriod.WEEKLY)
  val selectedPeriod: StateFlow<AnalyticsPeriod> = _selectedPeriod.asStateFlow()

  val uiState: StateFlow<AnalyticsUiState> = combine(
    repository.allTransactions,
    repository.userSettings,
    selectedPeriod
  ) { transactions, settings, period ->
    computeAnalytics(transactions, settings, period)
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = AnalyticsUiState()
  )

  fun setPeriod(period: AnalyticsPeriod) {
    _selectedPeriod.value = period
  }

  private fun computeAnalytics(
    transactions: List<TransactionEntity>,
    settings: UserSettings,
    period: AnalyticsPeriod
  ): AnalyticsUiState {
    val expenseItems = transactions.filter { it.type == TransactionType.EXPENSE }
    val incomeItems = transactions.filter { it.type == TransactionType.INCOME }

    val totalExpense = expenseItems.sumOf { it.amount }
    val totalIncome = incomeItems.sumOf { it.amount }
    val totalSavings = (totalIncome - totalExpense).coerceAtLeast(0.0)

    // Category breakdown
    val categoryTotals = expenseItems.groupBy { it.categoryName }
      .mapValues { entry -> entry.value.sumOf { it.amount } }
      .toList()
      .sortedByDescending { it.second }

    val mostExpensiveCategory = categoryTotals.firstOrNull()

    // Repeated micro-purchases detector
    val repeatedMap = mutableMapOf<String, Pair<Int, Double>>()
    expenseItems.forEach { item ->
      val key = item.description.trim().lowercase()
      val current = repeatedMap[key] ?: Pair(0, 0.0)
      repeatedMap[key] = Pair(current.first + 1, current.second + item.amount)
    }

    val repeatedList = repeatedMap.entries
      .filter { it.value.first >= 2 }
      .map { Pair(it.key, it.value) }
      .sortedByDescending { it.second.second }

    // Day-by-day spending grouping
    val dailyMap = expenseItems.groupBy { it.dateString }
      .mapValues { entry -> entry.value.sumOf { it.amount } }

    val limitViolationsCount = dailyMap.count { it.value > settings.dailySpendingLimit }
    val highestDay = dailyMap.maxByOrNull { it.value }?.toPair()
    val lowestDay = dailyMap.filter { it.value > 0 }.minByOrNull { it.value }?.toPair()

    val averageDailyExpense = if (dailyMap.isNotEmpty()) {
      dailyMap.values.sum() / dailyMap.size
    } else 0.0

    // Construct Bar Chart based on interval
    val barItems = when (period) {
      AnalyticsPeriod.DAILY, AnalyticsPeriod.WEEKLY -> {
        // Last 7 days
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayNameSdf = SimpleDateFormat("EEE", Locale.getDefault())
        val cal = Calendar.getInstance()
        val list = mutableListOf<BarChartItem>()

        for (i in 6 downTo 0) {
          val dayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
          val dateStr = sdf.format(dayCal.time)
          val dayName = dayNameSdf.format(dayCal.time)
          val amount = dailyMap[dateStr] ?: 0.0
          list.add(BarChartItem(label = dayName, value = amount, color = ExpenseRed))
        }
        list
      }
      AnalyticsPeriod.MONTHLY -> {
        // Group by weeks of month (W1, W2, W3, W4)
        val list = mutableListOf<BarChartItem>()
        for (w in 1..4) {
          val weekTotal = expenseItems.filter {
            try {
              val dayNum = it.dateString.takeLast(2).toInt()
              dayNum in ((w - 1) * 7 + 1)..(w * 7 + 3)
            } catch (e: Exception) { false }
          }.sumOf { it.amount }
          list.add(BarChartItem(label = "W$w", value = weekTotal, color = ExpenseRed))
        }
        list
      }
      AnalyticsPeriod.YEARLY -> {
        // Last 6 months
        val monthSdf = SimpleDateFormat("MMM", Locale.getDefault())
        val list = mutableListOf<BarChartItem>()
        for (m in 5 downTo 0) {
          val mCal = Calendar.getInstance().apply { add(Calendar.MONTH, -m) }
          val prefix = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(mCal.time)
          val monthName = monthSdf.format(mCal.time)
          val mTotal = expenseItems.filter { it.dateString.startsWith(prefix) }.sumOf { it.amount }
          list.add(BarChartItem(label = monthName, value = mTotal, color = ExpenseRed))
        }
        list
      }
    }

    return AnalyticsUiState(
      period = period,
      barChartItems = barItems,
      categoryBreakdown = categoryTotals,
      repeatedPurchases = repeatedList,
      totalExpense = totalExpense,
      totalIncome = totalIncome,
      averageDailyExpense = averageDailyExpense,
      highestSpendingDay = highestDay,
      lowestSpendingDay = lowestDay,
      mostExpensiveCategory = mostExpensiveCategory,
      limitViolationsCount = limitViolationsCount,
      dailyLimit = settings.dailySpendingLimit,
      totalSavings = totalSavings
    )
  }
}
