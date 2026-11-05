package com.example.ui.screens.transactions

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PendingTransactionDto
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.NavySurface
import com.example.viewmodel.StatementAutomationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingTransactionsScreen(
  viewModel: StatementAutomationViewModel,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val pendingList by viewModel.pendingTransactions.collectAsState()
  val realBalance by viewModel.realBalance.collectAsState()
  val isLoading by viewModel.isLoading.collectAsState()
  val errorMessage by viewModel.errorMessage.collectAsState()

  var selectedItemForDialog by remember { mutableStateOf<PendingTransactionDto?>(null) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("SBI Pending Transactions", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          IconButton(onClick = { viewModel.loadPendingTransactions() }) {
            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
          }
        }
      )
    }
  ) { innerPadding ->
    Column(
      modifier = modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
      // Header Info Card
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "SBI Mini Statement Ingestion",
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp
            )
            realBalance?.let { rb ->
              Text(
                text = "₹${"%.2f".format(rb.currentBalance)}",
                color = IncomeGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
              )
            }
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "These transactions were automatically retrieved from your SBI WhatsApp Statement (919022690226). Select a category to confirm them into your personal ledger.",
            color = Color(0xFF94A3B8),
            fontSize = 12.sp,
            lineHeight = 17.sp
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      if (pendingList.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              Icons.Default.CheckCircle,
              contentDescription = null,
              tint = IncomeGreen,
              modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "All Caught Up!",
              fontWeight = FontWeight.Bold,
              fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "No pending statement transactions awaiting confirmation. When Railway fetches a new SBI mini statement, new entries will appear here.",
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontSize = 13.sp,
              lineHeight = 18.sp
            )
          }
        }
      } else {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "${pendingList.size} Transactions Pending",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
          )
          Text(
            text = "Tap to categorize",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxSize()
        ) {
          items(pendingList, key = { it.id }) { item ->
            val isCredit = item.type.uppercase() == "CREDIT"
            val badgeColor = if (isCredit) IncomeGreen else ExpenseRed

            Card(
              shape = RoundedCornerShape(14.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              modifier = Modifier
                .fillMaxWidth()
                .clickable { selectedItemForDialog = item }
                .testTag("pending_item_${item.id}")
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                  Box(
                    modifier = Modifier
                      .size(42.dp)
                      .clip(CircleShape)
                      .background(badgeColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = if (isCredit) "CR" else "DR",
                      color = badgeColor,
                      fontSize = 13.sp,
                      fontWeight = FontWeight.Bold
                    )
                  }
                  Spacer(modifier = Modifier.width(12.dp))
                  Column {
                    Text(
                      text = item.description,
                      fontWeight = FontWeight.SemiBold,
                      fontSize = 14.sp,
                      maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = "${item.transactionDate} • Suggested: ${item.suggestedCategory}",
                      fontSize = 12.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }

                Column(horizontalAlignment = Alignment.End) {
                  Text(
                    text = (if (isCredit) "+ ₹" else "- ₹") + String.format("%.2f", item.amount),
                    color = badgeColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                  )
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = "Review",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  // Dialog when an item is tapped
  selectedItemForDialog?.let { item ->
    PendingTransactionConfirmDialog(
      transaction = item,
      onDismiss = { selectedItemForDialog = null },
      onConfirm = { category, purpose ->
        viewModel.confirmPendingTransaction(item, category, purpose) {
          Toast.makeText(context, "Transaction confirmed!", Toast.LENGTH_SHORT).show()
          selectedItemForDialog = null
        }
      },
      onReject = {
        viewModel.dismissPendingTransaction(item)
        Toast.makeText(context, "Transaction dismissed.", Toast.LENGTH_SHORT).show()
        selectedItemForDialog = null
      },
      isLoading = isLoading,
      errorMessage = errorMessage
    )
  }
}
