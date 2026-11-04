package com.example.ui.screens.transactions

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.PrimaryBlue
import com.example.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
  viewModel: TransactionViewModel,
  transactionToEdit: TransactionEntity? = null,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val categories by viewModel.allCategories.collectAsState()

  val isEditing = transactionToEdit != null

  var transactionType by remember {
    mutableStateOf(transactionToEdit?.type ?: TransactionType.EXPENSE)
  }
  var amountText by remember {
    mutableStateOf(transactionToEdit?.amount?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "")
  }
  var descriptionText by remember {
    mutableStateOf(transactionToEdit?.description ?: "")
  }
  var selectedCategory by remember {
    mutableStateOf(transactionToEdit?.categoryName ?: "Food & Dining")
  }
  var notesText by remember {
    mutableStateOf(transactionToEdit?.notes ?: "")
  }

  // Pre-fill date and time
  val currentDateSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
  val currentTimeSdf = SimpleDateFormat("HH:mm", Locale.getDefault())
  var dateText by remember {
    mutableStateOf(transactionToEdit?.dateString ?: currentDateSdf.format(Date()))
  }
  var timeText by remember {
    mutableStateOf(transactionToEdit?.timeString ?: currentTimeSdf.format(Date()))
  }

  var showCategoryDropdown by remember { mutableStateOf(false) }

  // Quick suggestions based on transaction type
  val expenseSuggestions = listOf("Tea", "Breakfast", "Lunch", "Dinner", "Lassi", "Printing", "Auto Rickshaw", "Bus Pass", "Stationery", "Books", "Mobile Recharge")
  val incomeSuggestions = listOf("Monthly Allowance", "Scholarship", "Family Support", "Freelance Gig", "Stipend", "Gift")

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = if (isEditing) "Edit Transaction" else "Add Transaction",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
          )
        },
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        }
      )
    }
  ) { innerPadding ->
    Column(
      modifier = modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 20.dp, vertical = 8.dp)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Type Toggle (Expense vs Income)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant)
          .padding(4.dp)
      ) {
        // Expense Button
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(if (transactionType == TransactionType.EXPENSE) ExpenseRed else Color.Transparent)
            .clickable {
              transactionType = TransactionType.EXPENSE
              if (selectedCategory == "Salary / Allowance" || selectedCategory == "Scholarship") {
                selectedCategory = "Food & Dining"
              }
            }
            .padding(vertical = 12.dp),
          contentAlignment = Alignment.Center
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.ArrowUpward,
              contentDescription = null,
              tint = if (transactionType == TransactionType.EXPENSE) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Expense",
              fontWeight = FontWeight.Bold,
              color = if (transactionType == TransactionType.EXPENSE) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Income Button
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(if (transactionType == TransactionType.INCOME) IncomeGreen else Color.Transparent)
            .clickable {
              transactionType = TransactionType.INCOME
              selectedCategory = "Salary / Allowance"
            }
            .padding(vertical = 12.dp),
          contentAlignment = Alignment.Center
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.ArrowDownward,
              contentDescription = null,
              tint = if (transactionType == TransactionType.INCOME) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Income",
              fontWeight = FontWeight.Bold,
              color = if (transactionType == TransactionType.INCOME) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      // Amount Input Card
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Text(
            text = "Amount",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(6.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "₹",
              fontSize = 32.sp,
              fontWeight = FontWeight.Bold,
              color = if (transactionType == TransactionType.EXPENSE) ExpenseRed else IncomeGreen
            )
            Spacer(modifier = Modifier.width(10.dp))
            OutlinedTextField(
              value = amountText,
              onValueChange = { amountText = it },
              placeholder = { Text("0.00", fontSize = 28.sp) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("transaction_amount_input")
            )
          }
        }
      }

      // Description & Suggestions
      Column {
        Text(
          text = "Description / Title",
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
          value = descriptionText,
          onValueChange = { descriptionText = it },
          placeholder = { Text("e.g. Tea, Breakfast, Printing, Scholarship...") },
          shape = RoundedCornerShape(14.dp),
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("transaction_description_input")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Suggestions
        val currentSuggestions = if (transactionType == TransactionType.EXPENSE) expenseSuggestions else incomeSuggestions
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          currentSuggestions.forEach { suggestion ->
            FilterChip(
              selected = descriptionText.equals(suggestion, ignoreCase = true),
              onClick = {
                descriptionText = suggestion
                // Auto-map reasonable category
                when (suggestion) {
                  "Tea", "Breakfast", "Lunch", "Dinner", "Lassi" -> selectedCategory = "Food & Dining"
                  "Auto Rickshaw", "Bus Pass" -> selectedCategory = "Transportation"
                  "Printing", "Stationery", "Books" -> selectedCategory = "Printing & Stationery"
                  "Mobile Recharge" -> selectedCategory = "Mobile Recharge"
                  "Scholarship" -> selectedCategory = "Scholarship"
                  "Monthly Allowance", "Family Support" -> selectedCategory = "Money from Family"
                }
              },
              label = { Text(suggestion, fontSize = 11.sp) }
            )
          }
        }
      }

      // Category Picker
      Column {
        Text(
          text = "Category",
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
              .fillMaxWidth()
              .clickable { showCategoryDropdown = true }
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.Category,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                  text = selectedCategory,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Medium,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
              Text("Change", color = PrimaryBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }

          DropdownMenu(
            expanded = showCategoryDropdown,
            onDismissRequest = { showCategoryDropdown = false },
            modifier = Modifier.fillMaxWidth(0.9f)
          ) {
            categories
              .filter { it.type == transactionType }
              .forEach { cat ->
                DropdownMenuItem(
                  text = { Text(cat.name) },
                  onClick = {
                    selectedCategory = cat.name
                    showCategoryDropdown = false
                  }
                )
              }
          }
        }
      }

      // Date and Time Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(text = "Date (YYYY-MM-DD)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
          Spacer(modifier = Modifier.height(4.dp))
          OutlinedTextField(
            value = dateText,
            onValueChange = { dateText = it },
            shape = RoundedCornerShape(14.dp),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }

        Column(modifier = Modifier.weight(1f)) {
          Text(text = "Time (HH:MM)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
          Spacer(modifier = Modifier.height(4.dp))
          OutlinedTextField(
            value = timeText,
            onValueChange = { timeText = it },
            shape = RoundedCornerShape(14.dp),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }
      }

      // Optional Notes
      Column {
        Text(
          text = "Notes (Optional)",
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
          value = notesText,
          onValueChange = { notesText = it },
          placeholder = { Text("Add transaction details, location, or purpose...") },
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth()
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Save Transaction Button
      Button(
        onClick = {
          val amount = amountText.toDoubleOrNull()
          if (amount == null || amount <= 0.0) {
            Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            return@Button
          }

          if (descriptionText.isBlank()) {
            Toast.makeText(context, "Please enter a description", Toast.LENGTH_SHORT).show()
            return@Button
          }

          if (isEditing && transactionToEdit != null) {
            val updated = transactionToEdit.copy(
              type = transactionType,
              amount = amount,
              description = descriptionText.trim(),
              categoryName = selectedCategory,
              dateString = dateText.trim(),
              timeString = timeText.trim(),
              notes = notesText.trim().ifBlank { null }
            )
            viewModel.updateTransaction(updated)
            Toast.makeText(context, "Transaction updated successfully", Toast.LENGTH_SHORT).show()
          } else {
            viewModel.addTransaction(
              type = transactionType,
              amount = amount,
              description = descriptionText.trim(),
              categoryName = selectedCategory,
              notes = notesText.trim().ifBlank { null }
            )
            Toast.makeText(context, "Transaction added to financial ledger", Toast.LENGTH_SHORT).show()
          }

          onNavigateBack()
        },
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("save_transaction_button"),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (isEditing) "Save Changes" else "Add to Ledger",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }

      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}
