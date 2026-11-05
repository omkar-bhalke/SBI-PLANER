package com.example.ui.screens.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.data.model.PendingTransactionDto
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.NavySurface

private val DEFAULT_CATEGORIES = listOf(
  "Food & Dining",
  "Groceries",
  "Transportation",
  "Utilities & Bills",
  "Entertainment",
  "Healthcare",
  "Shopping",
  "Bank Transfer",
  "Salary & Income",
  "Investments"
)

@Composable
fun PendingTransactionConfirmDialog(
  transaction: PendingTransactionDto,
  onDismiss: () -> Unit,
  onConfirm: (category: String, purpose: String?) -> Unit,
  onReject: () -> Unit,
  isLoading: Boolean = false,
  errorMessage: String? = null
) {
  var selectedCategory by remember {
    mutableStateOf(
      if (transaction.suggestedCategory.isNotBlank()) transaction.suggestedCategory else "Bank Transfer"
    )
  }
  var purposeText by remember { mutableStateOf("") }

  val isCredit = transaction.type.uppercase() == "CREDIT"
  val badgeColor = if (isCredit) IncomeGreen else ExpenseRed

  AlertDialog(
    onDismissRequest = onDismiss,
    shape = RoundedCornerShape(20.dp),
    containerColor = MaterialTheme.colorScheme.surface,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(badgeColor.copy(alpha = 0.15f)),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = if (isCredit) "+" else "-",
            color = badgeColor,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = if (isCredit) "Credit Entry Found" else "Debit Entry Found",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "SBI A/c ${transaction.accountNumber} • ${transaction.transactionDate}",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Amount and statement summary card
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = NavySurface)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(
              text = (if (isCredit) "+ ₹" else "- ₹") + String.format("%.2f", transaction.amount),
              color = badgeColor,
              fontSize = 24.sp,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Statement Raw: ${transaction.description}",
              color = Color(0xFFCBD5E1),
              fontSize = 12.sp
            )
          }
        }

        // Category Selection
        Text(
          text = "Select Category:",
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold
        )

        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          items(DEFAULT_CATEGORIES) { cat ->
            val isSelected = cat == selectedCategory
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(
                  if (isSelected) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.surfaceVariant
                )
                .clickable { selectedCategory = cat }
                .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
              Text(
                text = cat,
                fontSize = 12.sp,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
              )
            }
          }
        }

        // Purpose / Notes
        OutlinedTextField(
          value = purposeText,
          onValueChange = { purposeText = it },
          label = { Text("Purpose / Personal Note") },
          placeholder = { Text("e.g. Dinner with friends, Petrol, etc.") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("pending_tx_purpose_input")
        )

        if (errorMessage != null) {
          Text(text = errorMessage, color = ExpenseRed, fontSize = 12.sp)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(selectedCategory, purposeText.ifBlank { null }) },
        enabled = !isLoading,
        colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.testTag("confirm_pending_tx_btn")
      ) {
        if (isLoading) {
          CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
        } else {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Confirm & Add")
          }
        }
      }
    },
    dismissButton = {
      OutlinedButton(
        onClick = onReject,
        enabled = !isLoading,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.testTag("reject_pending_tx_btn")
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Dismiss")
        }
      }
    }
  )
}
