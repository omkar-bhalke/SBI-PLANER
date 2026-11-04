package com.example.data.repository

import android.content.Context
import com.example.data.ai.FinancialAiAssistant
import com.example.data.local.AppDatabase
import com.example.data.local.PreferenceManager
import com.example.data.model.CategoryEntity
import com.example.data.model.SyncStatus
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.model.UserSettings
import com.example.data.remote.NetworkClient
import com.example.data.remote.SyncManager
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FinancialRepository(context: Context) {
  private val database = AppDatabase.getDatabase(context)
  private val transactionDao = database.transactionDao()
  private val categoryDao = database.categoryDao()
  val preferenceManager = PreferenceManager(context)
  val syncManager = SyncManager(context, transactionDao)
  val aiAssistant = FinancialAiAssistant()

  val userSettings: Flow<UserSettings> = preferenceManager.userSettingsFlow
  val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
  val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

  fun getTodayDateString(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date())
  }

  fun getFirstDayOfMonthString(): String {
    val sdf = SimpleDateFormat("yyyy-MM-01", Locale.getDefault())
    return sdf.format(Date())
  }

  fun getTodayExpenses(): Flow<Double> = transactionDao.getTodayExpensesFlow(getTodayDateString())
  fun getTodayIncome(): Flow<Double> = transactionDao.getTodayIncomeFlow(getTodayDateString())
  fun getMonthlyExpenses(): Flow<Double> = transactionDao.getMonthlyExpensesFlow(getFirstDayOfMonthString())
  fun getMonthlyIncome(): Flow<Double> = transactionDao.getMonthlyIncomeFlow(getFirstDayOfMonthString())
  fun getTotalIncome(): Flow<Double> = transactionDao.getTotalIncomeFlow()
  fun getTotalExpenses(): Flow<Double> = transactionDao.getTotalExpensesFlow()

  suspend fun addTransaction(
    type: TransactionType,
    amount: Double,
    description: String,
    categoryName: String,
    categoryId: String? = null,
    dateMillis: Long = System.currentTimeMillis(),
    dateString: String,
    timeString: String,
    notes: String? = null
  ) {
    val transaction = TransactionEntity(
      type = type,
      amount = amount,
      description = description,
      categoryName = categoryName,
      categoryId = categoryId,
      dateMillis = dateMillis,
      dateString = dateString,
      timeString = timeString,
      notes = notes,
      syncStatus = SyncStatus.PENDING
    )
    transactionDao.insertTransaction(transaction)

    // Trigger sync in background if network is active
    if (syncManager.isNetworkAvailable()) {
      syncManager.syncPendingTransactions()
    }
  }

  suspend fun updateTransaction(transaction: TransactionEntity) {
    val updated = transaction.copy(syncStatus = SyncStatus.PENDING)
    transactionDao.updateTransaction(updated)
    if (syncManager.isNetworkAvailable()) {
      syncManager.syncPendingTransactions()
    }
  }

  suspend fun deleteTransaction(id: String) {
    transactionDao.deleteById(id)
    try {
      if (syncManager.isNetworkAvailable()) {
        NetworkClient.apiService.deleteTransaction(id)
      }
    } catch (_: Exception) {}
  }

  suspend fun addCategory(name: String, type: TransactionType, colorHex: String, iconName: String) {
    val category = CategoryEntity(
      id = "custom_${System.currentTimeMillis()}",
      name = name,
      type = type,
      iconName = iconName,
      colorHex = colorHex,
      isDefault = false
    )
    categoryDao.insertCategory(category)
  }

  suspend fun deleteCategory(category: CategoryEntity) {
    if (!category.isDefault) {
      categoryDao.deleteCategory(category)
    }
  }
}
