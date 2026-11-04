package com.example.ui.screens.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.components.TransactionItemRow
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.PrimaryBlue
import com.example.viewmodel.DateFilterPeriod
import com.example.viewmodel.SortOption
import com.example.viewmodel.TransactionViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun TransactionListScreen(
  viewModel: TransactionViewModel,
  onAddTransactionClick: () -> Unit,
  onEditTransactionClick: (TransactionEntity) -> Unit,
  modifier: Modifier = Modifier
) {
  val transactions by viewModel.filteredTransactions.collectAsState()
  val searchQuery by viewModel.searchQuery.collectAsState()
  val typeFilter by viewModel.typeFilter.collectAsState()
  val categoryFilter by viewModel.categoryFilter.collectAsState()
  val datePeriod by viewModel.datePeriod.collectAsState()
  val sortOption by viewModel.sortOption.collectAsState()
  val allCategories by viewModel.allCategories.collectAsState()

  var showSortMenu by remember { mutableStateOf(false) }
  var selectedTransactionForDetails by remember { mutableStateOf<TransactionEntity?>(null) }
  var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }

  val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    floatingActionButton = {
      FloatingActionButton(
        onClick = onAddTransactionClick,
        containerColor = PrimaryBlue,
        contentColor = Color.White,
        shape = CircleShape,
        modifier = Modifier.testTag("add_transaction_screen_fab")
      ) {
        Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
      }
    }
  ) { innerPadding ->
    Column(
      modifier = modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      // Header and Sort
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Financial History",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
          )
          Text(
            text = "${transactions.size} records matching filters",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Box {
          IconButton(onClick = { showSortMenu = true }) {
            Icon(imageVector = Icons.Default.Sort, contentDescription = "Sort", tint = MaterialTheme.colorScheme.primary)
          }

          DropdownMenu(
            expanded = showSortMenu,
            onDismissRequest = { showSortMenu = false }
          ) {
            DropdownMenuItem(
              text = { Text("Date (Newest First)") },
              onClick = {
                viewModel.setSortOption(SortOption.DATE_DESC)
                showSortMenu = false
              }
            )
            DropdownMenuItem(
              text = { Text("Date (Oldest First)") },
              onClick = {
                viewModel.setSortOption(SortOption.DATE_ASC)
                showSortMenu = false
              }
            )
            DropdownMenuItem(
              text = { Text("Amount (High to Low)") },
              onClick = {
                viewModel.setSortOption(SortOption.AMOUNT_DESC)
                showSortMenu = false
              }
            )
            DropdownMenuItem(
              text = { Text("Amount (Low to High)") },
              onClick = {
                viewModel.setSortOption(SortOption.AMOUNT_ASC)
                showSortMenu = false
              }
            )
          }
        }
      }

      // Search Field
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { viewModel.setSearchQuery(it) },
        placeholder = { Text("Search by description, notes, or category...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { viewModel.setSearchQuery("") }) {
              Icon(Icons.Default.Clear, contentDescription = "Clear")
            }
          }
        },
        shape = RoundedCornerShape(14.dp),
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 4.dp)
          .testTag("transaction_search_input")
      )

      // Type Filter Tabs (All, Expense, Income)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        FilterChip(
          selected = typeFilter == null,
          onClick = { viewModel.setTypeFilter(null) },
          label = { Text("All") },
          modifier = Modifier.testTag("filter_type_all")
        )
        FilterChip(
          selected = typeFilter == TransactionType.EXPENSE,
          onClick = { viewModel.setTypeFilter(TransactionType.EXPENSE) },
          label = { Text("Expenses Only") },
          colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ExpenseRed.copy(alpha = 0.2f)),
          modifier = Modifier.testTag("filter_type_expense")
        )
        FilterChip(
          selected = typeFilter == TransactionType.INCOME,
          onClick = { viewModel.setTypeFilter(TransactionType.INCOME) },
          label = { Text("Income Only") },
          colors = FilterChipDefaults.filterChipColors(selectedContainerColor = IncomeGreen.copy(alpha = 0.2f)),
          modifier = Modifier.testTag("filter_type_income")
        )
      }

      // Time Period Scrollable Chips
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        DateFilterPeriod.values().forEach { period ->
          val label = when (period) {
            DateFilterPeriod.ALL -> "All Time"
            DateFilterPeriod.TODAY -> "Today"
            DateFilterPeriod.YESTERDAY -> "Yesterday"
            DateFilterPeriod.THIS_WEEK -> "This Week"
            DateFilterPeriod.LAST_WEEK -> "Last Week"
            DateFilterPeriod.THIS_MONTH -> "This Month"
            DateFilterPeriod.LAST_MONTH -> "Last Month"
            DateFilterPeriod.THIS_YEAR -> "This Year"
          }
          FilterChip(
            selected = datePeriod == period,
            onClick = { viewModel.setDatePeriod(period) },
            label = { Text(label, fontSize = 12.sp) }
          )
        }
      }

      // Category Chips
      if (allCategories.isNotEmpty()) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          FilterChip(
            selected = categoryFilter == null,
            onClick = { viewModel.setCategoryFilter(null) },
            label = { Text("All Categories", fontSize = 11.sp) }
          )
          allCategories.forEach { cat ->
            FilterChip(
              selected = categoryFilter == cat.name,
              onClick = { viewModel.setCategoryFilter(if (categoryFilter == cat.name) null else cat.name) },
              label = { Text(cat.name, fontSize = 11.sp) }
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      // Transactions List
      if (transactions.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "No Transactions Found",
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "No records match the selected filter combination.",
              fontSize = 13.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(transactions, key = { it.id }) { item ->
            TransactionItemRow(
              transaction = item,
              onClick = { selectedTransactionForDetails = item }
            )
          }
          item {
            Spacer(modifier = Modifier.height(64.dp))
          }
        }
      }
    }
  }

  // Transaction Details / Action Dialog
  selectedTransactionForDetails?.let { tx ->
    AlertDialog(
      onDismissRequest = { selectedTransactionForDetails = null },
      title = {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(text = tx.description, fontWeight = FontWeight.Bold, fontSize = 18.sp)
          Text(
            text = (if (tx.type == TransactionType.INCOME) "+ " else "- ") + currencyFormat.format(tx.amount),
            color = if (tx.type == TransactionType.INCOME) IncomeGreen else ExpenseRed,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp
          )
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(text = "Category: ${tx.categoryName}", fontSize = 14.sp)
          Text(text = "Date: ${tx.dateString} at ${tx.timeString}", fontSize = 14.sp)
          if (!tx.notes.isNullOrBlank()) {
            Text(text = "Notes: ${tx.notes}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          Text(
            text = "Storage: Local Room Cache (${tx.syncStatus.name.lowercase()})",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      },
      confirmButton = {
        Row {
          IconButton(onClick = {
            val toEdit = tx
            selectedTransactionForDetails = null
            onEditTransactionClick(toEdit)
          }) {
            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
          }
          IconButton(onClick = {
            transactionToDelete = tx
            selectedTransactionForDetails = null
          }) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ExpenseRed)
          }
        }
      },
      dismissButton = {
        TextButton(onClick = { selectedTransactionForDetails = null }) {
          Text("Close")
        }
      }
    )
  }

  // Delete Confirmation Dialog
  transactionToDelete?.let { tx ->
    AlertDialog(
      onDismissRequest = { transactionToDelete = null },
      title = { Text("Delete Transaction?") },
      text = {
        Text("Are you sure you want to permanently delete \"${tx.description}\" (${currencyFormat.format(tx.amount)})? This will recalculate all affected financial balances.")
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.deleteTransaction(tx.id)
            transactionToDelete = null
          },
          colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = ExpenseRed)
        ) {
          Text("Delete", color = Color.White)
        }
      },
      dismissButton = {
        TextButton(onClick = { transactionToDelete = null }) {
          Text("Cancel")
        }
      }
    )
  }
}
