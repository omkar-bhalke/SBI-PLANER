package com.example.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentGold
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavySurface
import com.example.ui.theme.PrimaryBlue

data class OnboardingStep(
  val stepNumber: Int,
  val title: String,
  val subtitle: String,
  val icon: ImageVector,
  val description: String
)

@Composable
fun OnboardingScreen(
  onComplete: (initialBalance: Double, bankName: String, dailyLimit: Double) -> Unit,
  modifier: Modifier = Modifier
) {
  var currentStep by remember { mutableIntStateOf(1) }
  var initialBalanceText by remember { mutableStateOf("5000") }
  var dailyLimitText by remember { mutableStateOf("500") }
  var selectedBank by remember { mutableStateOf("State Bank of India (SBI)") }

  val supportedBanks = listOf(
    "State Bank of India (SBI)",
    "HDFC Bank",
    "ICICI Bank",
    "Punjab National Bank (PNB)",
    "Bank of Baroda",
    "Other Indian Bank"
  )

  val steps = listOf(
    OnboardingStep(
      1,
      "Welcome to FinPulse",
      "Your Personal Expense Management & AI Financial Assistant",
      Icons.Default.TrendingUp,
      "FinPulse is designed to transform how you handle daily money. Master your personal finances with strict daily spending limits, automatic micro-cost detection, and AI financial discipline."
    ),
    OnboardingStep(
      2,
      "Financial Clarity & Purpose",
      "Understand where every rupee goes",
      Icons.Default.Savings,
      "Stop wondering where your monthly allowance vanished. Log incidental expenses like tea, meals, printing, stationery, and travel in under 5 seconds to keep a pristine record."
    ),
    OnboardingStep(
      3,
      "Real-Time Dashboard",
      "Your complete financial cockpit",
      Icons.Default.Dashboard,
      "The dashboard continuously aggregates your available balance, today's spending, monthly savings, and recent transactions without ever needing manual recalculation."
    ),
    OnboardingStep(
      4,
      "Income & Expense Tracking",
      "Comprehensive two-way financial flow",
      Icons.Default.ReceiptLong,
      "Separate incoming cash (scholarships, allowances, family support) from everyday outflows (food, transport, education, shopping) with visual color badges."
    ),
    OnboardingStep(
      5,
      "Transaction Management",
      "Full control over your ledger",
      Icons.Default.CreditCard,
      "Edit, delete, search, and categorize past transactions instantly. Every change immediately updates all associated balances and historical charts."
    ),
    OnboardingStep(
      6,
      "Daily Spending Limit System",
      "Prevent overspending before it happens",
      Icons.Default.TrackChanges,
      "Set your daily allowance. FinPulse monitors every rupee spent today and triggers proactive visual warnings (Green: Safe, Orange: Approaching, Red: Exceeded)."
    ),
    OnboardingStep(
      7,
      "In-Depth Analytics",
      "Visual trends across all timeframes",
      Icons.Default.Analytics,
      "Inspect daily, weekly, monthly, and yearly bar charts. Pinpoint your most expensive spending days and visualize category allocations effortlessly."
    ),
    OnboardingStep(
      8,
      "AI Financial Assistant",
      "Data-grounded financial intelligence",
      Icons.Default.AutoAwesome,
      "Our AI analyzes your actual records to spot repeated small purchases (like 3 teas or daily snacks), calculates potential monthly savings, and provides disciplined budgeting advice."
    ),
    OnboardingStep(
      9,
      "Setup Initial Balance & Limit",
      "Enter your starting financial state",
      Icons.Default.AccountBalance,
      "Provide your opening bank or cash balance and target daily spending limit. You can adjust these anytime in Settings."
    ),
    OnboardingStep(
      10,
      "Select Your Primary Bank",
      "SBI (State Bank of India) Supported Initially",
      Icons.Default.Flag,
      "Choose your banking institution to organize your ledger and prepare for future transaction tracking."
    )
  )

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (currentStep > 1) {
          IconButton(onClick = { currentStep-- }) {
            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
          }
        } else {
          Spacer(modifier = Modifier.width(48.dp))
        }

        Text(
          text = "Step $currentStep of 10",
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )

        if (currentStep < 9) {
          Text(
            text = "Skip",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
              .clickable { currentStep = 9 }
              .padding(8.dp)
          )
        } else {
          Spacer(modifier = Modifier.width(48.dp))
        }
      }
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 24.dp, vertical = 8.dp)
        .verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      val step = steps[currentStep - 1]

      AnimatedContent(
        targetState = currentStep,
        transitionSpec = {
          if (targetState > initialState) {
            slideInHorizontally { width -> width } + fadeIn() togetherWith
            slideOutHorizontally { width -> -width } + fadeOut()
          } else {
            slideInHorizontally { width -> -width } + fadeIn() togetherWith
            slideOutHorizontally { width -> width } + fadeOut()
          }
        },
        label = "onboarding_step"
      ) { targetStepNum ->
        val current = steps[targetStepNum - 1]
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.fillMaxWidth()
        ) {
          Spacer(modifier = Modifier.height(16.dp))

          Box(
            modifier = Modifier
              .size(90.dp)
              .clip(CircleShape)
              .background(NavySurface),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = current.icon,
              contentDescription = current.title,
              tint = AccentGold,
              modifier = Modifier.size(48.dp)
            )
          }

          Spacer(modifier = Modifier.height(24.dp))

          Text(
            text = current.title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
          )

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = current.subtitle,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
          )

          Spacer(modifier = Modifier.height(16.dp))

          Text(
            text = current.description,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
          )

          Spacer(modifier = Modifier.height(24.dp))

          // Specific input forms for Steps 9 and 10
          if (targetStepNum == 9) {
            Card(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(18.dp)) {
                Text(
                  text = "Initial Account Balance (₹)",
                  fontSize = 13.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                  value = initialBalanceText,
                  onValueChange = { initialBalanceText = it },
                  placeholder = { Text("e.g. 5000") },
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                  singleLine = true,
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("initial_balance_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                  text = "Target Daily Spending Limit (₹)",
                  fontSize = 13.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                  value = dailyLimitText,
                  onValueChange = { dailyLimitText = it },
                  placeholder = { Text("e.g. 500") },
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                  singleLine = true,
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("daily_limit_input")
                )
              }
            }
          }

          if (targetStepNum == 10) {
            Column(
              modifier = Modifier.fillMaxWidth(),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Text(
                text = "Select Primary Bank Account:",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )

              supportedBanks.forEach { bank ->
                val isSelected = selectedBank == bank
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSelected) NavySurface else MaterialTheme.colorScheme.surface)
                    .border(
                      width = 1.dp,
                      color = if (isSelected) AccentGold else MaterialTheme.colorScheme.outline,
                      shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { selectedBank = bank }
                    .padding(14.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Default.AccountBalance,
                      contentDescription = bank,
                      tint = if (isSelected) AccentGold else MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                      text = bank,
                      fontSize = 14.sp,
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                      color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                  }

                  if (isSelected) {
                    Icon(
                      imageVector = Icons.Default.CheckCircle,
                      contentDescription = "Selected",
                      tint = AccentGold,
                      modifier = Modifier.size(18.dp)
                    )
                  }
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      // Bottom Action Button
      Button(
        onClick = {
          if (currentStep < 10) {
            currentStep++
          } else {
            val balance = initialBalanceText.toDoubleOrNull() ?: 0.0
            val limit = dailyLimitText.toDoubleOrNull() ?: 500.0
            onComplete(balance, selectedBank, limit)
          }
        },
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("onboarding_next_button"),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = if (currentStep == 10) "Get Started with FinPulse" else "Continue",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.width(8.dp))
          Icon(
            imageVector = if (currentStep == 10) Icons.Default.Check else Icons.Default.ArrowForward,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}
