package com.example.ui.screens.settings

import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.ui.screens.restriction.AppRestrictionManager
import com.example.ui.theme.AccentGold
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.NavySurface
import com.example.ui.theme.PrimaryBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRestrictionSettingsScreen(
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  var isFeatureActive by remember { mutableStateOf(AppRestrictionManager.isFeatureEnabled(context)) }
  var isAccessibilityGranted by remember { mutableStateOf(AppRestrictionManager.isAccessibilityServiceEnabled(context)) }

  // App selection map
  val appRestrictedMap = remember {
    mutableStateMapOf<String, Boolean>().apply {
      AppRestrictionManager.KNOWN_PAYMENT_PACKAGES.forEach { item ->
        put(item.packageName, AppRestrictionManager.isPackageRestricted(context, item.packageName))
      }
    }
  }

  // Auto-refresh permission status when user returns from Android Settings
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        isAccessibilityGranted = AppRestrictionManager.isAccessibilityServiceEnabled(context)
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Payment App Restrictions", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        }
      )
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 20.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // 1. Feature Master Toggle Card
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Spending Limit Guardian",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Monitor payment apps and display warning when daily limit is exceeded",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Switch(
              checked = isFeatureActive,
              onCheckedChange = {
                isFeatureActive = it
                AppRestrictionManager.setFeatureEnabled(context, it)
              },
              modifier = Modifier.testTag("app_restriction_master_switch")
            )
          }
        }
      }

      // 2. Android Accessibility Permission Card
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(
            containerColor = if (isAccessibilityGranted) IncomeGreen.copy(alpha = 0.1f) else NavySurface
          ),
          modifier = Modifier.fillMaxWidth().testTag("accessibility_permission_card")
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(if (isAccessibilityGranted) IncomeGreen.copy(alpha = 0.2f) else AccentGold.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = if (isAccessibilityGranted) Icons.Default.CheckCircle else Icons.Default.Security,
                  contentDescription = null,
                  tint = if (isAccessibilityGranted) IncomeGreen else AccentGold,
                  modifier = Modifier.size(20.dp)
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = if (isAccessibilityGranted) "Accessibility Service Enabled" else "Accessibility Permission Required",
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (isAccessibilityGranted) IncomeGreen else Color.White
                )
                Text(
                  text = if (isAccessibilityGranted) "Active: Detecting restricted payment app launches locally" else "Android requires explicit manual consent in Accessibility Settings",
                  fontSize = 11.sp,
                  color = if (isAccessibilityGranted) MaterialTheme.colorScheme.onSurface else Color(0xFF94A3B8)
                )
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
              text = "Why is this needed? To intercept payment apps when you exceed your daily allowance, Android's AccessibilityService API observes window events locally. FinPulse never reads text, keystrokes, passwords, or personal contents.",
              fontSize = 12.sp,
              color = if (isAccessibilityGranted) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFFE2E8F0),
              lineHeight = 17.sp
            )

            if (!isAccessibilityGranted) {
              Spacer(modifier = Modifier.height(14.dp))
              Button(
                onClick = { AppRestrictionManager.openAccessibilitySettings(context) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                modifier = Modifier.fillMaxWidth().testTag("enable_accessibility_button")
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.OpenInNew, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(8.dp))
                  Text("Enable in Android Settings", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
              }
            }
          }
        }
      }

      // 3. Monitored Payment Apps List
      item {
        Text(
          text = "Select Payment Applications to Guard",
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )
      }

      items(AppRestrictionManager.KNOWN_PAYMENT_PACKAGES) { item ->
        val isChecked = appRestrictedMap[item.packageName] ?: false
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                val newVal = !isChecked
                appRestrictedMap[item.packageName] = newVal
                AppRestrictionManager.setPackageRestricted(context, item.packageName, newVal)
              }
              .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = item.appName,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = item.packageName,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            Checkbox(
              checked = isChecked,
              onCheckedChange = {
                appRestrictedMap[item.packageName] = it
                AppRestrictionManager.setPackageRestricted(context, item.packageName, it)
              }
            )
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }
}
