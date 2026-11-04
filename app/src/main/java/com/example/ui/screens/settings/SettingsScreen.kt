package com.example.ui.screens.settings

import android.widget.Toast
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryEntity
import com.example.data.model.TransactionType
import com.example.ui.theme.AccentGold
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.NavySurface
import com.example.ui.theme.PrimaryBlue
import com.example.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
  viewModel: SettingsViewModel,
  onNavigateToWhatsApp: () -> Unit,
  onReplayTutorial: () -> Unit,
  onLogout: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val settings by viewModel.userSettings.collectAsState()
  val categories by viewModel.categories.collectAsState()

  var showDailyLimitDialog by remember { mutableStateOf(false) }
  var showInitialBalanceDialog by remember { mutableStateOf(false) }
  var showBankSelectionDialog by remember { mutableStateOf(false) }
  var showAddCategoryDialog by remember { mutableStateOf(false) }
  var showAiProviderDialog by remember { mutableStateOf(false) }
  var showThemeDialog by remember { mutableStateOf(false) }
  var showSecurityDialog by remember { mutableStateOf(false) }
  var showBankIntegrationInfoDialog by remember { mutableStateOf(false) }
  var showPaymentRestrictionInfoDialog by remember { mutableStateOf(false) }

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
            text = "Settings & Configurations",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
          )
          Text(
            text = "Personal profile, banking, limits, and integrations",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      // 1. Profile & Google Account Card
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = NavySurface),
          modifier = Modifier.fillMaxWidth().testTag("profile_settings_card")
        ) {
          Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(AccentGold),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = (settings.userName.takeIf { it.isNotBlank() } ?: "U").take(1).uppercase(),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color(0xFF0F172A)
              )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = settings.userName.ifBlank { "FinPulse User" },
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
              Text(
                text = settings.userEmail.ifBlank { "google.auth@gmail.com" },
                fontSize = 12.sp,
                color = Color(0xFF94A3B8)
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Google Account Verified",
                fontSize = 10.sp,
                color = AccentGold,
                fontWeight = FontWeight.SemiBold
              )
            }
          }
        }
      }

      // 2. Financial Controls Section
      item {
        Text(
          text = "Financial Controls",
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )
      }

      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column {
            SettingsItemRow(
              icon = Icons.Default.TrackChanges,
              title = "Daily Spending Limit",
              subtitle = "Current: ₹${settings.dailySpendingLimit.toInt()} / day",
              onClick = { showDailyLimitDialog = true }
            )
            SettingsItemRow(
              icon = Icons.Default.AccountBalance,
              title = "Opening / Initial Balance",
              subtitle = "Current: ₹${settings.initialBalance.toInt()}",
              onClick = { showInitialBalanceDialog = true }
            )
            SettingsItemRow(
              icon = Icons.Default.AccountBalance,
              title = "Selected Primary Bank",
              subtitle = settings.selectedBank,
              onClick = { showBankSelectionDialog = true }
            )
          }
        }
      }

      // 3. Categories Management
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Categories (${categories.size})",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
          TextButton(onClick = { showAddCategoryDialog = true }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Add Custom", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            categories.take(6).forEach { cat ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Box(
                    modifier = Modifier
                      .size(8.dp)
                      .clip(CircleShape)
                      .background(MaterialTheme.colorScheme.primary)
                  )
                  Spacer(modifier = Modifier.width(10.dp))
                  Text(cat.name, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }

                if (!cat.isDefault) {
                  IconButton(
                    onClick = { viewModel.deleteCategory(cat) },
                    modifier = Modifier.size(24.dp)
                  ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ExpenseRed, modifier = Modifier.size(16.dp))
                  }
                } else {
                  Text("Default", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
              }
            }
          }
        }
      }

      // 4. Integrations & Future Modules
      item {
        Text(
          text = "Integrations & Future Modules",
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )
      }

      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column {
            SettingsItemRow(
              icon = Icons.Default.Phone,
              title = "WhatsApp Integration",
              subtitle = if (settings.isWhatsAppConnected) "Linked with ${settings.whatsAppPhoneNumber}" else "Link phone for chat tracking",
              onClick = onNavigateToWhatsApp
            )
            SettingsItemRow(
              icon = Icons.Default.AutoAwesome,
              title = "AI Engine Configuration",
              subtitle = "Active Provider: ${settings.aiProvider}",
              onClick = { showAiProviderDialog = true }
            )
            SettingsItemRow(
              icon = Icons.Default.AccountBalance,
              title = "Bank Transaction Integration",
              subtitle = "SBI Supported • Authorized SMS / Notifications architecture",
              onClick = { showBankIntegrationInfoDialog = true }
            )
            SettingsItemRow(
              icon = Icons.Default.Block,
              title = "Payment App Restrictions (Future)",
              subtitle = "Spending limit warnings & optional app restriction specs",
              onClick = { showPaymentRestrictionInfoDialog = true }
            )
          }
        }
      }

      // 5. App Preferences & Security
      item {
        Text(
          text = "Preferences & Help",
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )
      }

      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column {
            SettingsItemRow(
              icon = Icons.Default.DarkMode,
              title = "Theme Appearance",
              subtitle = settings.themeMode,
              onClick = { showThemeDialog = true }
            )
            SettingsItemRow(
              icon = Icons.Default.Help,
              title = "Interactive Tutorial & Help Center",
              subtitle = "Replay the 10-step guided onboarding walkthrough",
              onClick = onReplayTutorial
            )
            SettingsItemRow(
              icon = Icons.Default.Security,
              title = "Privacy & Zero-Trust Cloud Security",
              subtitle = "cPanel MySQL / Railway HTTPS Architecture",
              onClick = { showSecurityDialog = true }
            )
          }
        }
      }

      // Logout Button
      item {
        Button(
          onClick = onLogout,
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("logout_button"),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed.copy(alpha = 0.12f), contentColor = ExpenseRed)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Log Out from FinPulse", fontWeight = FontWeight.Bold, fontSize = 14.sp)
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(30.dp))
      }
    }
  }

  // --- Dialogs ---

  // Daily Spending Limit Dialog
  if (showDailyLimitDialog) {
    var limitInput by remember { mutableStateOf(settings.dailySpendingLimit.toInt().toString()) }
    AlertDialog(
      onDismissRequest = { showDailyLimitDialog = false },
      title = { Text("Daily Spending Limit") },
      text = {
        Column {
          Text("Set your daily expense threshold in ₹. The app will warn you when approaching or exceeding this limit.")
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedTextField(
            value = limitInput,
            onValueChange = { limitInput = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("edit_daily_limit_field")
          )
        }
      },
      confirmButton = {
        Button(onClick = {
          limitInput.toDoubleOrNull()?.let { viewModel.updateDailyLimit(it) }
          showDailyLimitDialog = false
        }) {
          Text("Save")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDailyLimitDialog = false }) { Text("Cancel") }
      }
    )
  }

  // Initial Balance Dialog
  if (showInitialBalanceDialog) {
    var balInput by remember { mutableStateOf(settings.initialBalance.toInt().toString()) }
    AlertDialog(
      onDismissRequest = { showInitialBalanceDialog = false },
      title = { Text("Initial Account Balance") },
      text = {
        Column {
          Text("Adjust your opening bank/cash balance. Total balance will automatically recalculate.")
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedTextField(
            value = balInput,
            onValueChange = { balInput = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(onClick = {
          balInput.toDoubleOrNull()?.let { viewModel.updateInitialBalance(it) }
          showInitialBalanceDialog = false
        }) {
          Text("Save")
        }
      },
      dismissButton = {
        TextButton(onClick = { showInitialBalanceDialog = false }) { Text("Cancel") }
      }
    )
  }

  // Bank Selection Dialog
  if (showBankSelectionDialog) {
    val banks = listOf("State Bank of India (SBI)", "HDFC Bank", "ICICI Bank", "Punjab National Bank (PNB)", "Bank of Baroda", "Other Indian Bank")
    AlertDialog(
      onDismissRequest = { showBankSelectionDialog = false },
      title = { Text("Select Primary Bank") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          banks.forEach { bank ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  viewModel.updateBank(bank)
                  showBankSelectionDialog = false
                }
                .padding(vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = bank,
                fontWeight = if (settings.selectedBank == bank) FontWeight.Bold else FontWeight.Normal,
                color = if (settings.selectedBank == bank) PrimaryBlue else MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }
      },
      confirmButton = {},
      dismissButton = {
        TextButton(onClick = { showBankSelectionDialog = false }) { Text("Close") }
      }
    )
  }

  // Add Category Dialog
  if (showAddCategoryDialog) {
    var catName by remember { mutableStateOf("") }
    var catType by remember { mutableStateOf(TransactionType.EXPENSE) }
    AlertDialog(
      onDismissRequest = { showAddCategoryDialog = false },
      title = { Text("Add Custom Category") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = catName,
            onValueChange = { catName = it },
            placeholder = { Text("e.g. Gym, Pet Care, Streaming") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
              onClick = { catType = TransactionType.EXPENSE },
              colors = ButtonDefaults.buttonColors(
                containerColor = if (catType == TransactionType.EXPENSE) ExpenseRed else Color.LightGray
              )
            ) { Text("Expense") }
            Button(
              onClick = { catType = TransactionType.INCOME },
              colors = ButtonDefaults.buttonColors(
                containerColor = if (catType == TransactionType.INCOME) IncomeGreen else Color.LightGray
              )
            ) { Text("Income") }
          }
        }
      },
      confirmButton = {
        Button(onClick = {
          if (catName.isNotBlank()) {
            viewModel.addCustomCategory(catName.trim(), catType, "#1E3E62", "category")
            showAddCategoryDialog = false
          }
        }) { Text("Add") }
      },
      dismissButton = {
        TextButton(onClick = { showAddCategoryDialog = false }) { Text("Cancel") }
      }
    )
  }

  // AI Provider Dialog
  if (showAiProviderDialog) {
    AlertDialog(
      onDismissRequest = { showAiProviderDialog = false },
      title = { Text("AI Provider Configuration") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Select active AI intelligence engine for spending pattern analysis:")
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                viewModel.updateAiProvider("GEMINI")
                showAiProviderDialog = false
              }
              .padding(8.dp)
          ) {
            Text("Google Gemini API (Active)", fontWeight = if (settings.aiProvider == "GEMINI") FontWeight.Bold else FontWeight.Normal)
          }
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                viewModel.updateAiProvider("OPENAI")
                showAiProviderDialog = false
              }
              .padding(8.dp)
          ) {
            Text("OpenAI GPT-4o (Backend Configuration)", fontWeight = if (settings.aiProvider == "OPENAI") FontWeight.Bold else FontWeight.Normal)
          }
        }
      },
      confirmButton = {},
      dismissButton = { TextButton(onClick = { showAiProviderDialog = false }) { Text("Close") } }
    )
  }

  // Theme Appearance Dialog
  if (showThemeDialog) {
    AlertDialog(
      onDismissRequest = { showThemeDialog = false },
      title = { Text("Theme Appearance") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf("LIGHT", "DARK", "SYSTEM").forEach { mode ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  viewModel.updateTheme(mode)
                  showThemeDialog = false
                }
                .padding(8.dp)
            ) {
              Text(mode, fontWeight = if (settings.themeMode == mode) FontWeight.Bold else FontWeight.Normal)
            }
          }
        }
      },
      confirmButton = {},
      dismissButton = { TextButton(onClick = { showThemeDialog = false }) { Text("Close") } }
    )
  }

  // Future Bank Integration Info
  if (showBankIntegrationInfoDialog) {
    AlertDialog(
      onDismissRequest = { showBankIntegrationInfoDialog = false },
      title = { Text("Bank Transaction Architecture") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Section 13 Future Bank Architecture Specifications:")
          Text("• Default: State Bank of India (SBI).")
          Text("• Non-invasive legal architecture: Authorized bank SMS pattern recognition & bank-authorized push notifications.")
          Text("• Strict Zero-Trust: Never requests banking passwords, OTPs, PINs, or credentials.")
          Text("• Interactive category prompt after detected transaction.")
        }
      },
      confirmButton = { Button(onClick = { showBankIntegrationInfoDialog = false }) { Text("Understood") } }
    )
  }

  // Future Payment Restriction Info
  if (showPaymentRestrictionInfoDialog) {
    AlertDialog(
      onDismissRequest = { showPaymentRestrictionInfoDialog = false },
      title = { Text("Payment App Restriction Architecture") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Section 14 Future Payment Restriction Module:")
          Text("• Version 1 implements real-time monitoring and visual warnings (Green, Orange, Red).")
          Text("• Future Android module uses Accessibility/UsageStats APIs with explicit user authorization.")
          Text("• Emergency spending exception allowance gateway allows academic/medical bypass.")
        }
      },
      confirmButton = { Button(onClick = { showPaymentRestrictionInfoDialog = false }) { Text("Understood") } }
    )
  }

  // Security & Cloud Architecture Info
  if (showSecurityDialog) {
    AlertDialog(
      onDismissRequest = { showSecurityDialog = false },
      title = { Text("Privacy & Security Architecture") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("• User isolation: Each user record is strictly scoped by verified Google ID.")
          Text("• Monies stored using DECIMAL(14,2) for exact mathematical precision.")
          Text("• Cloud backend: Node.js Express deployed on Railway with HTTPS encryption.")
          Text("• Database: Dedicated MySQL hosted on cPanel with indexed foreign keys.")
          Text("• Offline local caching: Encrypted Android Room with idempotent sync keys.")
        }
      },
      confirmButton = { Button(onClick = { showSecurityDialog = false }) { Text("Done") } }
    )
  }
}

@Composable
private fun SettingsItemRow(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  subtitle: String,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .padding(16.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(18.dp)
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column {
        Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        Text(text = subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
    Icon(
      imageVector = Icons.Default.ChevronRight,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(20.dp)
    )
  }
}
