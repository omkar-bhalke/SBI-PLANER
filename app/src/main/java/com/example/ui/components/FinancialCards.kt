package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ExpenseRedLight
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenLight
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavySurface
import com.example.ui.theme.SavingsTeal
import com.example.ui.theme.WarningOrange
import com.example.ui.theme.WarningOrangeLight
import java.text.NumberFormat
import java.util.Locale

@Composable
fun CurrentBalanceCard(
  totalBalance: Double,
  initialBalance: Double,
  totalIncome: Double,
  totalExpenses: Double,
  bankName: String = "State Bank of India (SBI)",
  modifier: Modifier = Modifier
) {
  val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("current_balance_card"),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = NavyDark),
    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(20.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(NavySurface),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.AccountBalance,
              contentDescription = "Bank",
              tint = Color(0xFFE2B659),
              modifier = Modifier.size(20.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = bankName,
            color = Color(0xFF94A3B8),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
          )
        }

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(NavySurface)
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(
            text = "Active Account",
            color = Color(0xFFE2E8F0),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Text(
        text = "Total Available Balance",
        color = Color(0xFF94A3B8),
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal
      )

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = currencyFormat.format(totalBalance),
        color = Color.White,
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.testTag("balance_amount_text")
      )

      Spacer(modifier = Modifier.height(18.dp))

      // Income vs Expense Mini Ribbon
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(Color(0xFF14243B))
          .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        // Income
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(28.dp)
              .clip(CircleShape)
              .background(IncomeGreen.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.ArrowDownward,
              contentDescription = "Income",
              tint = IncomeGreen,
              modifier = Modifier.size(16.dp)
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(text = "Total Income", color = Color(0xFF94A3B8), fontSize = 11.sp)
            Text(
              text = currencyFormat.format(totalIncome),
              color = IncomeGreen,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        // Expenses
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(28.dp)
              .clip(CircleShape)
              .background(ExpenseRed.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.ArrowUpward,
              contentDescription = "Expense",
              tint = ExpenseRed,
              modifier = Modifier.size(16.dp)
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(text = "Total Expenses", color = Color(0xFF94A3B8), fontSize = 11.sp)
            Text(
              text = currencyFormat.format(totalExpenses),
              color = ExpenseRed,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }
  }
}

@Composable
fun SpendingLimitCard(
  dailyLimit: Double,
  spentToday: Double,
  onConfigureClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
  val remaining = (dailyLimit - spentToday).coerceAtLeast(0.0)
  val utilizationRatio = if (dailyLimit > 0) (spentToday / dailyLimit).toFloat() else 0f
  val progress = utilizationRatio.coerceIn(0f, 1f)

  val animatedProgress by animateFloatAsState(
    targetValue = progress,
    animationSpec = tween(durationMillis = 600),
    label = "progress"
  )

  // Status indicators:
  // Green: < 75%
  // Orange: 75% - 100%
  // Red: > 100% (Exceeded)
  val isExceeded = spentToday > dailyLimit
  val isApproaching = !isExceeded && utilizationRatio >= 0.75f

  val statusColor by animateColorAsState(
    targetValue = when {
      isExceeded -> ExpenseRed
      isApproaching -> WarningOrange
      else -> IncomeGreen
    },
    label = "statusColor"
  )

  val statusBgColor = when {
    isExceeded -> ExpenseRedLight
    isApproaching -> WarningOrangeLight
    else -> IncomeGreenLight
  }

  val statusText = when {
    isExceeded -> "Limit Exceeded"
    isApproaching -> "Approaching Limit"
    else -> "Within Limit"
  }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("spending_limit_card"),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Daily Spending Limit",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Target: ${currencyFormat.format(dailyLimit)} / day",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(statusBgColor)
            .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = when {
                isExceeded -> Icons.Default.Error
                isApproaching -> Icons.Default.Warning
                else -> Icons.Default.CheckCircle
              },
              contentDescription = statusText,
              tint = statusColor,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = statusText,
              color = statusColor,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Progress bar
      LinearProgressIndicator(
        progress = { animatedProgress },
        modifier = Modifier
          .fillMaxWidth()
          .height(10.dp)
          .clip(RoundedCornerShape(5.dp)),
        color = statusColor,
        trackColor = MaterialTheme.colorScheme.surfaceVariant
      )

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column {
          Text(text = "Spent Today", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(
            text = currencyFormat.format(spentToday),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = statusColor
          )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(text = "Utilization", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(
            text = "${(utilizationRatio * 100).toInt()}%",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        Column(horizontalAlignment = Alignment.End) {
          Text(text = "Remaining", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(
            text = currencyFormat.format(remaining),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (remaining > 0) IncomeGreen else ExpenseRed
          )
        }
      }
    }
  }
}

@Composable
fun MonthlyOverviewCard(
  monthlyIncome: Double,
  monthlyExpense: Double,
  modifier: Modifier = Modifier
) {
  val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
  val netSavings = monthlyIncome - monthlyExpense
  val spendingRatio = if (monthlyIncome > 0) ((monthlyExpense / monthlyIncome) * 100).toInt() else 0

  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Monthly Financial Health",
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Savings,
            contentDescription = "Savings",
            tint = SavingsTeal,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "Net: ${currencyFormat.format(netSavings)}",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (netSavings >= 0) SavingsTeal else ExpenseRed
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(text = "Monthly Inflow", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(
            text = currencyFormat.format(monthlyIncome),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = IncomeGreen
          )
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(text = "Monthly Outflow", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(
            text = currencyFormat.format(monthlyExpense),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = ExpenseRed
          )
        }
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
          Text(text = "Spend Ratio", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(
            text = "$spendingRatio%",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
      }
    }
  }
}
