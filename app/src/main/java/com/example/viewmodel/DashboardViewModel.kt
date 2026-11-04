package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.TransactionEntity
import com.example.data.model.UserSettings
import com.example.data.repository.FinancialRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
  val totalBalance: Double = 0.0,
  val initialBalance: Double = 0.0,
  val totalIncome: Double = 0.0,
  val totalExpenses: Double = 0.0,
  val todayIncome: Double = 0.0,
  val todayExpenses: Double = 0.0,
  val monthlyIncome: Double = 0.0,
  val monthlyExpenses: Double = 0.0,
  val dailyLimit: Double = 500.0,
  val selectedBank: String = "State Bank of India (SBI)",
  val recentTransactions: List<TransactionEntity> = emptyList(),
  val isLoading: Boolean = false,
  val isRefreshing: Boolean = false
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
  val repository = FinancialRepository(application)

  private val _isRefreshing = MutableStateFlow(false)
  val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

  val uiState: StateFlow<DashboardUiState> = combine(
    repository.userSettings,
    repository.getTotalIncome(),
    repository.getTotalExpenses(),
    repository.getTodayIncome(),
    repository.getTodayExpenses(),
    repository.getMonthlyIncome(),
    repository.getMonthlyExpenses(),
    repository.allTransactions
  ) { args: Array<Any> ->
    val settings = args[0] as UserSettings
    val totalIncome = args[1] as Double
    val totalExpenses = args[2] as Double
    val todayIncome = args[3] as Double
    val todayExpenses = args[4] as Double
    val monthlyIncome = args[5] as Double
    val monthlyExpenses = args[6] as Double
    @Suppress("UNCHECKED_CAST")
    val allTransactions = args[7] as List<TransactionEntity>

    val totalBalance = settings.initialBalance + totalIncome - totalExpenses

    DashboardUiState(
      totalBalance = totalBalance,
      initialBalance = settings.initialBalance,
      totalIncome = totalIncome,
      totalExpenses = totalExpenses,
      todayIncome = todayIncome,
      todayExpenses = todayExpenses,
      monthlyIncome = monthlyIncome,
      monthlyExpenses = monthlyExpenses,
      dailyLimit = settings.dailySpendingLimit,
      selectedBank = settings.selectedBank,
      recentTransactions = allTransactions.take(8),
      isLoading = false
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = DashboardUiState(isLoading = true)
  )

  fun refreshData() {
    viewModelScope.launch {
      _isRefreshing.value = true
      repository.syncManager.syncPendingTransactions()
      _isRefreshing.value = false
    }
  }

  fun updateDailyLimit(newLimit: Double) {
    viewModelScope.launch {
      repository.preferenceManager.updateDailySpendingLimit(newLimit)
    }
  }
}
