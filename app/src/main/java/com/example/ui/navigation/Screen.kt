package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
  object Login : Screen("login", "Login")
  object Register : Screen("register", "Register")
  object Onboarding : Screen("onboarding", "Onboarding")

  // Bottom Navigation tabs
  object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
  object Transactions : Screen("transactions", "History", Icons.Default.ReceiptLong)
  object Analytics : Screen("analytics", "Analytics", Icons.Default.Analytics)
  object AiAssistant : Screen("ai_assistant", "AI Advisor", Icons.Default.AutoAwesome)
  object Settings : Screen("settings", "Settings", Icons.Default.Settings)

  // Secondary sub-screens
  object AddTransaction : Screen("add_transaction", "Add Transaction")
  object EditTransaction : Screen("edit_transaction", "Edit Transaction")
  object WhatsAppLinking : Screen("whatsapp_linking", "WhatsApp Linking")
  object AppRestrictions : Screen("app_restrictions", "App Restrictions")
  object EmergencyApproval : Screen("emergency_approval", "Emergency Request")
  object PendingTransactions : Screen("pending_transactions", "SBI Statement Entries")
  object AutomationSettings : Screen("automation_settings", "Statement Automation")

  companion object {
    val bottomNavItems = listOf(
      Dashboard,
      Transactions,
      Analytics,
      AiAssistant,
      Settings
    )
  }
}
