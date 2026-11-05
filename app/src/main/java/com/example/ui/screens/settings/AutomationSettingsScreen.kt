package com.example.ui.screens.settings

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.NavySurface
import com.example.viewmodel.StatementAutomationViewModel

private val INTERVAL_OPTIONS = listOf(15, 30, 60, 120, 360)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationSettingsScreen(
  viewModel: StatementAutomationViewModel,
  onNavigateBack: () -> Unit,
  onNavigateToWhatsAppLinking: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val config by viewModel.automationConfig.collectAsState()
  val realBalance by viewModel.realBalance.collectAsState()
  val isLoading by viewModel.isLoading.collectAsState()
  val statusMsg by viewModel.statusMessage.collectAsState()
  val errMsg by viewModel.errorMessage.collectAsState()

  var targetPhone by remember { mutableStateOf("919022690226") }
  var selectedInterval by remember { mutableStateOf(30) }
  var isEnabled by remember { mutableStateOf(true) }
  var railwayUrl by remember { mutableStateOf("https://your-railway-app.up.railway.app") }

  LaunchedEffect(config) {
    config?.let {
      targetPhone = it.sbiTargetPhone
      selectedInterval = it.intervalMinutes
      isEnabled = it.isEnabled
      if (!it.railwayApiUrl.isNullOrBlank()) {
        railwayUrl = it.railwayApiUrl
      }
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("SBI Mini Statement Automation", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        }
      )
    }
  ) { innerPadding ->
    Column(
      modifier = modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 20.dp, vertical = 10.dp)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // 1. Current Live Balance Card
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(38.dp)
                  .clip(CircleShape)
                  .background(IncomeGreen.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(20.dp))
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "Verified Bank Balance",
                  color = Color.White,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = "SBI A/c ${realBalance?.accountNumber ?: "xxx188"}",
                  color = Color(0xFF94A3B8),
                  fontSize = 12.sp
                )
              }
            }

            Text(
              text = "₹${String.format("%.2f", realBalance?.currentBalance ?: 0.0)}",
              color = IncomeGreen,
              fontSize = 20.sp,
              fontWeight = FontWeight.Bold
            )
          }

          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = "Last statement date: ${realBalance?.lastStatementDate ?: "Not yet fetched"}\nSource: Automated SBI WhatsApp Mini Statement",
            color = Color(0xFFCBD5E1),
            fontSize = 11.sp,
            lineHeight = 16.sp
          )
        }
      }

      // Feedback messages
      statusMsg?.let { msg ->
        Card(
          shape = RoundedCornerShape(10.dp),
          colors = CardDefaults.cardColors(containerColor = IncomeGreen.copy(alpha = 0.15f))
        ) {
          Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = msg, color = IncomeGreen, fontSize = 12.sp)
          }
        }
      }

      errMsg?.let { msg ->
        Card(
          shape = RoundedCornerShape(10.dp),
          colors = CardDefaults.cardColors(containerColor = ExpenseRed.copy(alpha = 0.15f))
        ) {
          Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = msg, color = ExpenseRed, fontSize = 12.sp)
          }
        }
      }

      // 2. Automation Active Toggle
      Card(
        shape = RoundedCornerShape(14.dp),
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
            Text("Automated Fetching", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(
              text = if (isEnabled) "Worker runs continuously on Railway" else "Automation is paused",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Switch(
            checked = isEnabled,
            onCheckedChange = { isEnabled = it },
            colors = SwitchDefaults.colors(checkedThumbColor = IncomeGreen)
          )
        }
      }

      // 3. Interval Selection
      Column {
        Text("Fetch Frequency", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          INTERVAL_OPTIONS.forEach { mins ->
            val label = if (mins >= 60) "${mins / 60}h" else "${mins}m"
            val isSel = selectedInterval == mins
            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                .clickable { selectedInterval = mins }
                .padding(vertical = 10.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = label,
                color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
              )
            }
          }
        }
      }

      // 4. Target SBI Number
      Column {
        Text("Target SBI WhatsApp Number", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
          value = targetPhone,
          onValueChange = { targetPhone = it },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("sbi_target_number_input")
        )
        Text(
          text = "Standard SBI WhatsApp Banking Bot Number: 919022690226",
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(top = 4.dp)
        )
      }

      // 5. Railway Backend URL
      Column {
        Text("Railway Backend Endpoint URL", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
          value = railwayUrl,
          onValueChange = { railwayUrl = it },
          placeholder = { Text("https://xxx.up.railway.app") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("railway_url_input")
        )
      }

      // 6. Action: Save Configuration
      Button(
        onClick = {
          viewModel.updateAutomationConfig(
            intervalMinutes = selectedInterval,
            targetPhone = targetPhone,
            isEnabled = isEnabled,
            railwayUrl = railwayUrl.ifBlank { null }
          ) {
            Toast.makeText(context, "Automation settings saved!", Toast.LENGTH_SHORT).show()
          }
        },
        enabled = !isLoading,
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("save_automation_config_btn"),
        shape = RoundedCornerShape(12.dp)
      ) {
        if (isLoading) {
          CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
        } else {
          Text("Save Automation Settings", fontWeight = FontWeight.Bold)
        }
      }

      // 7. Action: Run Immediate Cycle
      OutlinedButton(
        onClick = {
          if (railwayUrl.isBlank()) {
            Toast.makeText(context, "Please provide Railway URL first", Toast.LENGTH_SHORT).show()
            return@OutlinedButton
          }
          viewModel.triggerAutomationNow(railwayUrl.trim())
        },
        enabled = !isLoading,
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("trigger_statement_now_btn"),
        shape = RoundedCornerShape(12.dp)
      ) {
        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Fetch Mini Statement Now")
      }

      // 8. Link to WhatsApp linking screen
      OutlinedButton(
        onClick = onNavigateToWhatsAppLinking,
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp),
        shape = RoundedCornerShape(12.dp)
      ) {
        Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("WhatsApp Session & Pairing")
      }

      // Disclaimer regarding Baileys
      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
      ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
          Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "Important Architecture Disclosure: The WhatsApp automation worker runs Baileys Web multi-device protocols. This is an unofficial WhatsApp client protocol. It is not an official SBI or WhatsApp API. Ensure you review WhatsApp terms of service.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            lineHeight = 16.sp
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))
    }
  }
}
