package com.example.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CategoryDistributionCard
import com.example.ui.components.RepeatedPurchasesCard
import com.example.ui.components.SimpleBarChart
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.NavySurface
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SavingsTeal
import com.example.ui.theme.WarningOrange
import com.example.viewmodel.AnalyticsPeriod
import com.example.viewmodel.AnalyticsViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun AnalyticsScreen(
  viewModel: AnalyticsViewModel,
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsState()
  val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background
  ) { innerPadding ->
    LazyColumn(
      modifier = modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Header
      item {
        Column {
          Text(
            text = "Expense Analytics",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
          )
          Text(
            text = "Data-driven spending habits and financial patterns",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      // Period Tabs (Daily, Weekly, Monthly, Yearly)
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          AnalyticsPeriod.values().forEach { period ->
            val isSelected = uiState.period == period
            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isSelected) PrimaryBlue else Color.Transparent)
                .clickable { viewModel.setPeriod(period) }
                .padding(vertical = 8.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = period.name.lowercase().replaceFirstChar { it.uppercase() },
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }

      // 1. Spending Trend Bar Chart
      item {
        SimpleBarChart(
          items = uiState.barChartItems,
          dailyLimit = uiState.dailyLimit
        )
      }

      // 2. Spending Analytics Highlights Card
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier.fillMaxWidth().testTag("analytics_highlights_card")
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Insights,
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Key Spending Highlights",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Metric 1: Average Daily Expenses
            HighlightRow(
              title = "Average Daily Expenses",
              value = currencyFormat.format(uiState.averageDailyExpense),
              subtitle = "Across all tracked active days",
              valueColor = MaterialTheme.colorScheme.onSurface
            )

            // Metric 2: Most Expensive Category
            uiState.mostExpensiveCategory?.let { (cat, amt) ->
              HighlightRow(
                title = "Top Outflow Category",
                value = "$cat (${currencyFormat.format(amt)})",
                subtitle = "Dominates overall expenditure",
                valueColor = ExpenseRed
              )
            }

            // Metric 3: Highest Spending Day
            uiState.highestSpendingDay?.let { (day, amt) ->
              HighlightRow(
                title = "Peak Spending Day",
                value = "$day (${currencyFormat.format(amt)})",
                subtitle = "Single highest recorded daily outflow",
                valueColor = WarningOrange
              )
            }

            // Metric 4: Spending Limit Violations
            HighlightRow(
              title = "Daily Limit Violations",
              value = "${uiState.limitViolationsCount} Days",
              subtitle = "Days where spending crossed ₹${uiState.dailyLimit.toInt()}",
              valueColor = if (uiState.limitViolationsCount > 0) ExpenseRed else IncomeGreen
            )

            // Metric 5: Net Savings
            HighlightRow(
              title = "Retained Net Savings",
              value = currencyFormat.format(uiState.totalSavings),
              subtitle = "Cumulative surplus retained from income",
              valueColor = SavingsTeal
            )
          }
        }
      }

      // 3. Category Breakdown Bar/Pie
      item {
        CategoryDistributionCard(categoryTotals = uiState.categoryBreakdown)
      }

      // 4. Repeated Micro-Purchases Detector
      item {
        RepeatedPurchasesCard(repeatedPurchases = uiState.repeatedPurchases)
      }

      item {
        Spacer(modifier = Modifier.height(30.dp))
      }
    }
  }
}

@Composable
private fun HighlightRow(
  title: String,
  value: String,
  subtitle: String,
  valueColor: Color
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 8.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
      Text(text = subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = valueColor)
  }
}
