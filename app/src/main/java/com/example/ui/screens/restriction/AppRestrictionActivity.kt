package com.example.ui.screens.restriction

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainActivity
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.FinPulseTheme
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavySurface
import com.example.ui.theme.PrimaryBlue
import java.text.NumberFormat
import java.util.Locale

class AppRestrictionActivity : ComponentActivity() {

  companion object {
    const val EXTRA_PACKAGE_NAME = "extra_package_name"
    const val EXTRA_APP_NAME = "extra_app_name"
    const val EXTRA_SPENT_TODAY = "extra_spent_today"
    const val EXTRA_DAILY_LIMIT = "extra_daily_limit"
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: "Payment Application"
    val spentToday = intent.getDoubleExtra(EXTRA_SPENT_TODAY, 0.0)
    val dailyLimit = intent.getDoubleExtra(EXTRA_DAILY_LIMIT, 500.0)

    setContent {
      FinPulseTheme(darkTheme = true) {
        AppRestrictionScreen(
          appName = appName,
          spentToday = spentToday,
          dailyLimit = dailyLimit,
          onReturnToDashboard = {
            val homeIntent = Intent(this, MainActivity::class.java).apply {
              flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(homeIntent)
            finish()
          },
          onRequestEmergencyApproval = {
            val homeIntent = Intent(this, MainActivity::class.java).apply {
              flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
              putExtra("open_emergency_approval", true)
            }
            startActivity(homeIntent)
            finish()
          }
        )
      }
    }
  }
}

@Composable
fun AppRestrictionScreen(
  appName: String,
  spentToday: Double,
  dailyLimit: Double,
  onReturnToDashboard: () -> Unit,
  onRequestEmergencyApproval: () -> Unit
) {
  val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
  val amountExceeded = (spentToday - dailyLimit).coerceAtLeast(0.0)

  Scaffold(containerColor = NavyDark) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // Top Warning Badge
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 32.dp)
      ) {
        Box(
          modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(ExpenseRed.copy(alpha = 0.2f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Block,
            contentDescription = "Restricted",
            tint = ExpenseRed,
            modifier = Modifier.size(48.dp)
          )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
          text = "Daily Limit Reached",
          fontSize = 24.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "Your daily spending limit has been reached. FinPulse Spending Guardian paused launch of $appName to protect your financial discipline.",
          fontSize = 13.sp,
          color = Color(0xFF94A3B8),
          textAlign = TextAlign.Center,
          lineHeight = 19.sp
        )
      }

      // Middle Financial Breakdown Card
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        modifier = Modifier.fillMaxWidth().testTag("app_restriction_breakdown_card")
      ) {
        Column(modifier = Modifier.padding(20.dp)) {
          Text(
            text = "Today's Financial Status",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )

          Spacer(modifier = Modifier.height(16.dp))

          RestrictionRow(
            label = "Daily Spending Limit",
            value = currencyFormat.format(dailyLimit),
            valueColor = Color.White
          )

          Spacer(modifier = Modifier.height(10.dp))

          RestrictionRow(
            label = "Amount Spent Today",
            value = currencyFormat.format(spentToday),
            valueColor = ExpenseRed
          )

          Spacer(modifier = Modifier.height(10.dp))

          RestrictionRow(
            label = "Amount Exceeded",
            value = "+ ${currencyFormat.format(amountExceeded)}",
            valueColor = ExpenseRed,
            isBold = true
          )
        }
      }

      // Bottom Actions
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Button(
          onClick = onReturnToDashboard,
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("restriction_return_dashboard_button"),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Return to FinPulse Dashboard", fontWeight = FontWeight.Bold, fontSize = 14.sp)
          }
        }

        OutlinedButton(
          onClick = onRequestEmergencyApproval,
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("restriction_request_emergency_button"),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFE2B659), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Request Emergency Allowance Exception", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
          }
        }
      }
    }
  }
}

@Composable
private fun RestrictionRow(label: String, value: String, valueColor: Color, isBold: Boolean = false) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(text = label, color = Color(0xFF94A3B8), fontSize = 13.sp)
    Text(
      text = value,
      color = valueColor,
      fontSize = 14.sp,
      fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium
    )
  }
}
