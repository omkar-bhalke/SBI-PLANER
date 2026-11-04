package com.example.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SyncStatus
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import java.text.NumberFormat
import java.util.Locale

fun getCategoryIcon(categoryName: String): ImageVector {
  val name = categoryName.lowercase()
  return when {
    name.contains("food") || name.contains("dine") || name.contains("meal") || name.contains("tea") || name.contains("breakfast") -> Icons.Default.Restaurant
    name.contains("transport") || name.contains("bus") || name.contains("travel") || name.contains("auto") -> Icons.Default.DirectionsBus
    name.contains("edu") || name.contains("college") || name.contains("school") || name.contains("exam") -> Icons.Default.School
    name.contains("print") || name.contains("stationery") || name.contains("xerox") -> Icons.Default.Print
    name.contains("shop") || name.contains("buy") || name.contains("store") -> Icons.Default.ShoppingBag
    name.contains("entertain") || name.contains("movie") || name.contains("game") -> Icons.Default.Movie
    name.contains("recharge") || name.contains("phone") || name.contains("bill") -> Icons.Default.PhoneAndroid
    name.contains("salary") || name.contains("allowance") -> Icons.Default.AccountBalanceWallet
    name.contains("scholarship") -> Icons.Default.CardGiftcard
    name.contains("family") -> Icons.Default.FamilyRestroom
    name.contains("invest") || name.contains("return") -> Icons.Default.TrendingUp
    name.contains("income") -> Icons.Default.AttachMoney
    name.contains("person") -> Icons.Default.Person
    else -> Icons.Default.Category
  }
}

fun getCategoryColor(categoryName: String, type: TransactionType): Color {
  if (type == TransactionType.INCOME) return IncomeGreen
  val name = categoryName.lowercase()
  return when {
    name.contains("food") -> Color(0xFFF59E0B)
    name.contains("transport") -> Color(0xFF3B82F6)
    name.contains("edu") -> Color(0xFF8B5CF6)
    name.contains("print") -> Color(0xFF6366F1)
    name.contains("shop") -> Color(0xFFEC4899)
    name.contains("entertain") -> Color(0xFF10B981)
    name.contains("recharge") -> Color(0xFF06B6D4)
    name.contains("person") -> Color(0xFF14B8A6)
    else -> Color(0xFF64748B)
  }
}

@Composable
fun TransactionItemRow(
  transaction: TransactionEntity,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
  val isIncome = transaction.type == TransactionType.INCOME
  val amountColor = if (isIncome) IncomeGreen else ExpenseRed
  val prefix = if (isIncome) "+ " else "- "
  val iconColor = getCategoryColor(transaction.categoryName, transaction.type)
  val iconVector = getCategoryIcon(transaction.categoryName)

  Card(
    modifier = modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .testTag("transaction_item_${transaction.id}"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Category Icon
      Box(
        modifier = Modifier
          .size(44.dp)
          .clip(CircleShape)
          .background(iconColor.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = iconVector,
          contentDescription = transaction.categoryName,
          tint = iconColor,
          modifier = Modifier.size(22.dp)
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      // Description & Category
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = transaction.description,
          fontSize = 14.sp,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = transaction.categoryName,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = " • ${transaction.dateString} ${transaction.timeString}",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
          )
        }
      }

      // Amount & Sync Badge
      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = prefix + currencyFormat.format(transaction.amount),
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
          color = amountColor
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = if (transaction.syncStatus == SyncStatus.SYNCED) Icons.Default.CloudDone else Icons.Default.CloudQueue,
            contentDescription = if (transaction.syncStatus == SyncStatus.SYNCED) "Synced to cloud" else "Cached locally",
            tint = if (transaction.syncStatus == SyncStatus.SYNCED) IncomeGreen else Color(0xFF94A3B8),
            modifier = Modifier.size(12.dp)
          )
          Spacer(modifier = Modifier.width(3.dp))
          Text(
            text = if (transaction.syncStatus == SyncStatus.SYNCED) "Cloud" else "Offline",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }
}
