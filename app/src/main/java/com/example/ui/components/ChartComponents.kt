package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionType
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.PrimaryBlue
import java.text.NumberFormat
import java.util.Locale

data class BarChartItem(
  val label: String,
  val value: Double,
  val color: Color = ExpenseRed
)

@Composable
fun SimpleBarChart(
  items: List<BarChartItem>,
  modifier: Modifier = Modifier,
  dailyLimit: Double? = null
) {
  val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
  val maxValue = (items.maxOfOrNull { it.value } ?: 100.0).coerceAtLeast(dailyLimit ?: 100.0)

  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Spending Trend",
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )

        dailyLimit?.let { limit ->
          Text(
            text = "Limit: ${currencyFormat.format(limit)}",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      if (items.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
          contentAlignment = Alignment.Center
        ) {
          Text("No expense records for this interval", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
      } else {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.Bottom
        ) {
          items.forEach { item ->
            val ratio = if (maxValue > 0) (item.value / maxValue).toFloat().coerceIn(0.05f, 1f) else 0.05f
            val isOverLimit = dailyLimit != null && item.value > dailyLimit

            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Bottom,
              modifier = Modifier.fillMaxHeight().weight(1f)
            ) {
              if (item.value > 0) {
                Text(
                  text = "₹${item.value.toInt()}",
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (isOverLimit) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
              }

              Box(
                modifier = Modifier
                  .width(22.dp)
                  .fillMaxHeight(ratio * 0.75f)
                  .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                  .background(if (isOverLimit) ExpenseRed else item.color)
              )

              Spacer(modifier = Modifier.height(8.dp))

              Text(
                text = item.label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun CategoryDistributionCard(
  categoryTotals: List<Pair<String, Double>>,
  modifier: Modifier = Modifier
) {
  val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
  val grandTotal = categoryTotals.sumOf { it.second }.coerceAtLeast(1.0)

  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Text(
        text = "Category-Wise Distribution",
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )

      Spacer(modifier = Modifier.height(14.dp))

      if (categoryTotals.isEmpty()) {
        Text("No expenses logged yet", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
      } else {
        categoryTotals.take(6).forEach { (category, amount) ->
          val percent = ((amount / grandTotal) * 100).toInt()
          val color = getCategoryColor(category, TransactionType.EXPENSE)

          Column(modifier = Modifier.padding(vertical = 6.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = category,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Medium,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }

              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = currencyFormat.format(amount),
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "($percent%)",
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
              progress = { (amount / grandTotal).toFloat() },
              modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
              color = color,
              trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
          }
        }
      }
    }
  }
}

@Composable
fun RepeatedPurchasesCard(
  repeatedPurchases: List<Pair<String, Pair<Int, Double>>>,
  modifier: Modifier = Modifier
) {
  val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Text(
        text = "Frequent Micro-Purchases",
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = "Repeated items that cumulatively impact daily budget",
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(12.dp))

      if (repeatedPurchases.isEmpty()) {
        Text(
          text = "No recurring micro-purchases detected. Add more transactions to reveal patterns.",
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      } else {
        repeatedPurchases.take(4).forEach { (itemName, stats) ->
          val count = stats.first
          val totalSpent = stats.second

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 6.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
              .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = itemName.replaceFirstChar { it.uppercase() },
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Purchased $count times",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            Text(
              text = currencyFormat.format(totalSpent),
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              color = ExpenseRed
            )
          }
        }
      }
    }
  }
}
