package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CategoryEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.repository.FinancialRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class DateFilterPeriod {
  ALL,
  TODAY,
  YESTERDAY,
  THIS_WEEK,
  LAST_WEEK,
  THIS_MONTH,
  LAST_MONTH,
  THIS_YEAR
}

enum class SortOption {
  DATE_DESC,
  DATE_ASC,
  AMOUNT_DESC,
  AMOUNT_ASC
}

class TransactionViewModel(application: Application) : AndroidViewModel(application) {
  val repository = FinancialRepository(application)

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _typeFilter = MutableStateFlow<TransactionType?>(null)
  val typeFilter: StateFlow<TransactionType?> = _typeFilter.asStateFlow()

  private val _categoryFilter = MutableStateFlow<String?>(null)
  val categoryFilter: StateFlow<String?> = _categoryFilter.asStateFlow()

  private val _datePeriod = MutableStateFlow(DateFilterPeriod.ALL)
  val datePeriod: StateFlow<DateFilterPeriod> = _datePeriod.asStateFlow()

  private val _sortOption = MutableStateFlow(SortOption.DATE_DESC)
  val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

  val allCategories: StateFlow<List<CategoryEntity>> = repository.allCategories.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
    repository.allTransactions,
    searchQuery,
    typeFilter,
    categoryFilter,
    datePeriod,
    sortOption
  ) { args: Array<Any?> ->
    @Suppress("UNCHECKED_CAST")
    val list = args[0] as List<TransactionEntity>
    val query = args[1] as String
    val type = args[2] as? TransactionType
    val category = args[3] as? String
    val period = args[4] as DateFilterPeriod
    val sort = args[5] as SortOption

    var result = list

    // Type filter
    if (type != null) {
      result = result.filter { it.type == type }
    }

    // Category filter
    if (!category.isNullOrBlank()) {
      result = result.filter { it.categoryName == category }
    }

    // Search query
    if (query.isNotBlank()) {
      val trimmed = query.trim().lowercase()
      result = result.filter {
        it.description.lowercase().contains(trimmed) ||
        (it.notes?.lowercase()?.contains(trimmed) == true) ||
        it.categoryName.lowercase().contains(trimmed)
      }
    }

    // Date period filter
    result = filterByPeriod(result, period)

    // Sort
    result = when (sort) {
      SortOption.DATE_DESC -> result.sortedWith(compareByDescending<TransactionEntity> { it.dateMillis }.thenByDescending { it.timeString })
      SortOption.DATE_ASC -> result.sortedWith(compareBy<TransactionEntity> { it.dateMillis }.thenBy { it.timeString })
      SortOption.AMOUNT_DESC -> result.sortedByDescending { it.amount }
      SortOption.AMOUNT_ASC -> result.sortedBy { it.amount }
    }

    result
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  fun setSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun setTypeFilter(type: TransactionType?) {
    _typeFilter.value = type
  }

  fun setCategoryFilter(category: String?) {
    _categoryFilter.value = category
  }

  fun setDatePeriod(period: DateFilterPeriod) {
    _datePeriod.value = period
  }

  fun setSortOption(sort: SortOption) {
    _sortOption.value = sort
  }

  fun addTransaction(
    type: TransactionType,
    amount: Double,
    description: String,
    categoryName: String,
    notes: String?,
    customDate: Date? = null,
    customTime: String? = null
  ) {
    viewModelScope.launch {
      val dateToUse = customDate ?: Date()
      val dateSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
      val timeSdf = SimpleDateFormat("HH:mm", Locale.getDefault())

      val dateStr = dateSdf.format(dateToUse)
      val timeStr = customTime ?: timeSdf.format(dateToUse)

      repository.addTransaction(
        type = type,
        amount = amount,
        description = description.trim(),
        categoryName = categoryName.trim(),
        dateMillis = dateToUse.time,
        dateString = dateStr,
        timeString = timeStr,
        notes = notes?.trim()
      )
    }
  }

  fun updateTransaction(transaction: TransactionEntity) {
    viewModelScope.launch {
      repository.updateTransaction(transaction)
    }
  }

  fun deleteTransaction(id: String) {
    viewModelScope.launch {
      repository.deleteTransaction(id)
    }
  }

  fun addCustomCategory(name: String, type: TransactionType, colorHex: String, iconName: String) {
    viewModelScope.launch {
      repository.addCategory(name, type, colorHex, iconName)
    }
  }

  private fun filterByPeriod(list: List<TransactionEntity>, period: DateFilterPeriod): List<TransactionEntity> {
    if (period == DateFilterPeriod.ALL) return list

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val cal = Calendar.getInstance()

    return when (period) {
      DateFilterPeriod.TODAY -> {
        val todayStr = sdf.format(cal.time)
        list.filter { it.dateString == todayStr }
      }
      DateFilterPeriod.YESTERDAY -> {
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = sdf.format(cal.time)
        list.filter { it.dateString == yesterdayStr }
      }
      DateFilterPeriod.THIS_WEEK -> {
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        val startWeek = cal.timeInMillis
        list.filter { it.dateMillis >= startWeek }
      }
      DateFilterPeriod.LAST_WEEK -> {
        cal.add(Calendar.WEEK_OF_YEAR, -1)
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        val startLastWeek = cal.timeInMillis
        cal.add(Calendar.DAY_OF_WEEK, 6)
        val endLastWeek = cal.timeInMillis
        list.filter { it.dateMillis in startLastWeek..endLastWeek }
      }
      DateFilterPeriod.THIS_MONTH -> {
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val startMonth = cal.timeInMillis
        list.filter { it.dateMillis >= startMonth }
      }
      DateFilterPeriod.LAST_MONTH -> {
        cal.add(Calendar.MONTH, -1)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val startLastMonth = cal.timeInMillis
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        val endLastMonth = cal.timeInMillis
        list.filter { it.dateMillis in startLastMonth..endLastMonth }
      }
      DateFilterPeriod.THIS_YEAR -> {
        cal.set(Calendar.DAY_OF_YEAR, 1)
        val startYear = cal.timeInMillis
        list.filter { it.dateMillis >= startYear }
      }
      DateFilterPeriod.ALL -> list
    }
  }
}
