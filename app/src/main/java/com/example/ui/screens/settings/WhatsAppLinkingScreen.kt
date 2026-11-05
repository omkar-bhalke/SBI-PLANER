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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VpnKey
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.NavySurface
import com.example.viewmodel.SettingsViewModel
import com.example.viewmodel.StatementAutomationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsAppLinkingScreen(
  viewModel: SettingsViewModel,
  automationViewModel: StatementAutomationViewModel? = null,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val settings by viewModel.userSettings.collectAsState()
  val whatsAppStatus by viewModel.whatsAppStatus.collectAsState()

  val automationConfig by automationViewModel?.automationConfig?.collectAsState() ?: remember { mutableStateOf(null) }
  val railwayStatus by automationViewModel?.railwayStatus?.collectAsState() ?: remember { mutableStateOf(null) }
  val pairingCode by automationViewModel?.pairingCode?.collectAsState() ?: remember { mutableStateOf(null) }
  val autoLoading by automationViewModel?.isLoading?.collectAsState() ?: remember { mutableStateOf(false) }
  val autoErrMsg by automationViewModel?.errorMessage?.collectAsState() ?: remember { mutableStateOf(null) }
  val autoStatusMsg by automationViewModel?.statusMessage?.collectAsState() ?: remember { mutableStateOf(null) }

  var selectedTab by remember { mutableIntStateOf(0) } // 0: Railway Worker, 1: Meta Cloud API

  // Railway State
  var railwayUrl by remember { mutableStateOf("https://your-railway-app.up.railway.app") }
  var personalPhoneNumber by remember { mutableStateOf("") }

  LaunchedEffect(automationConfig) {
    automationConfig?.railwayApiUrl?.let {
      if (it.isNotBlank()) railwayUrl = it
    }
  }

  // Meta State
  var metaPhoneNumberText by remember { mutableStateOf(settings.whatsAppPhoneNumber ?: "+91 ") }
  var accessToken by remember { mutableStateOf("") }
  var phoneNumberId by remember { mutableStateOf("") }
  var wabaId by remember { mutableStateOf("") }
  var showAdvancedMetaConfig by remember { mutableStateOf(false) }
  var metaLoading by remember { mutableStateOf(false) }
  var metaErrorMessage by remember { mutableStateOf<String?>(null) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("WhatsApp Integration", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
        .padding(horizontal = 20.dp, vertical = 8.dp)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Tab switcher
      TabRow(
        selectedTabIndex = selectedTab,
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        contentColor = MaterialTheme.colorScheme.primary
      ) {
        Tab(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          text = { Text("Railway SBI Worker", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
        )
        Tab(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          text = { Text("Meta Cloud API", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
        )
      }

      if (selectedTab == 0) {
        // ==========================================
        // TAB 0: RAILWAY BAILEYS WORKER
        // ==========================================

        // Architecture & Disclaimer Banner
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = NavySurface)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(IncomeGreen.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.Phone, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(20.dp))
              }
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = "Railway WhatsApp Multi-Device Worker",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
              )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "Links your personal WhatsApp to the 24/7 worker on Railway. Sessions are stored on persistent volume (/data). The worker talks to SBI WhatsApp Banking (919022690226) to retrieve mini statements.",
              color = Color(0xFF94A3B8),
              fontSize = 12.sp,
              lineHeight = 17.sp
            )
          }
        }

        // Live Railway Status
        val isRailwayConnected = railwayStatus?.connected == true
        val statusString = when {
          isRailwayConnected -> "CONNECTED"
          railwayStatus?.status == "RECONNECTING" -> "RECONNECTING"
          else -> "DISCONNECTED"
        }
        val statusColor = when {
          isRailwayConnected -> IncomeGreen
          railwayStatus?.status == "RECONNECTING" -> Color(0xFFF59E0B)
          else -> Color(0xFF64748B)
        }

        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text("Railway Connection Status", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(
                  text = if (isRailwayConnected) "Active session on persistent volume" else "No active WhatsApp session on worker",
                  fontSize = 12.sp,
                  color = if (isRailwayConnected) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .background(statusColor.copy(alpha = 0.15f))
                  .padding(horizontal = 10.dp, vertical = 5.dp)
              ) {
                Text(text = statusString, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
              onClick = {
                automationViewModel?.checkRailwayStatus(railwayUrl)
              },
              modifier = Modifier.fillMaxWidth()
            ) {
              Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Refresh Worker Status")
            }
          }
        }

        // Feedback messages
        autoStatusMsg?.let { msg ->
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

        autoErrMsg?.let { msg ->
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

        // Railway Endpoint input
        Column {
          Text("Railway Worker URL", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
          Spacer(modifier = Modifier.height(4.dp))
          OutlinedTextField(
            value = railwayUrl,
            onValueChange = { railwayUrl = it },
            placeholder = { Text("https://xxx.up.railway.app") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("railway_worker_url_input")
          )
        }

        // 8-Digit Pairing Code Method
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Link With 8-Digit Pairing Code", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(
              text = "Enter your phone number with country code (e.g. 919876543210). WhatsApp will generate an 8-character pairing code.",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              lineHeight = 16.sp
            )

            OutlinedTextField(
              value = personalPhoneNumber,
              onValueChange = { personalPhoneNumber = it },
              placeholder = { Text("919876543210") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("pairing_phone_input")
            )

            Button(
              onClick = {
                if (personalPhoneNumber.trim().length < 10) {
                  Toast.makeText(context, "Please enter valid number with country code", Toast.LENGTH_SHORT).show()
                  return@Button
                }
                automationViewModel?.requestPairingCode(railwayUrl, personalPhoneNumber)
              },
              enabled = !autoLoading,
              colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth().testTag("request_pairing_code_btn")
            ) {
              if (autoLoading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
              } else {
                Text("Generate 8-Digit Pairing Code", fontWeight = FontWeight.Bold)
              }
            }

            // Display pairing code if generated
            pairingCode?.let { code ->
              Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(
                  modifier = Modifier.padding(14.dp),
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Text("Enter this code in WhatsApp:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                  Spacer(modifier = Modifier.height(6.dp))
                  Text(
                    text = code,
                    color = IncomeGreen,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                  )
                  Spacer(modifier = Modifier.height(6.dp))
                  Text(
                    text = "WhatsApp > Linked Devices > Link a Device > Link with phone number instead",
                    color = Color.White,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                  )
                }
              }
            }
          }
        }

        // Disconnect Button
        if (isRailwayConnected) {
          OutlinedButton(
            onClick = {
              automationViewModel?.disconnectRailway(railwayUrl)
            },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text("Disconnect Railway WhatsApp Session", fontWeight = FontWeight.Bold)
          }
        }

        // Baileys Terms & Risk Disclosure
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
          Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Notice: The automated worker uses the open-source Baileys multi-device protocol. This is an unofficial WhatsApp client. WhatsApp may apply rate limits or account restrictions on automated accounts. Do not use your primary personal number if you are concerned with temporary restrictions.",
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontSize = 11.sp,
              lineHeight = 16.sp
            )
          }
        }

      } else {
        // ==========================================
        // TAB 1: META GRAPH API GATEWAY (TASK 3)
        // ==========================================
        val isMetaConnected = whatsAppStatus?.connected == true || settings.isWhatsAppConnected

        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = NavySurface)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
              text = "Meta WhatsApp Business Cloud API Gateway",
              color = Color.White,
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Official Meta Cloud API webhook gateway. Requires Meta Developer Graph credentials. Fully compliant with Meta TOS.",
              color = Color(0xFF94A3B8),
              fontSize = 12.sp,
              lineHeight = 17.sp
            )
          }
        }

        // Live Connection Status
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
            Column {
              Text(text = "Meta Gateway Status", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
              Text(
                text = if (isMetaConnected) "Active with ${whatsAppStatus?.phoneNumber ?: settings.whatsAppPhoneNumber}" else "No active gateway link",
                fontSize = 12.sp,
                color = if (isMetaConnected) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background((if (isMetaConnected) IncomeGreen else Color(0xFF64748B)).copy(alpha = 0.15f))
                .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
              Text(
                text = if (isMetaConnected) "CONNECTED" else "NOT CONNECTED",
                color = if (isMetaConnected) IncomeGreen else Color(0xFF64748B),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }

        if (metaErrorMessage != null) {
          Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = ExpenseRed.copy(alpha = 0.15f))
          ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Error, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(text = metaErrorMessage!!, color = ExpenseRed, fontSize = 12.sp)
            }
          }
        }

        // Phone Input Field
        Column {
          Text("WhatsApp Business Phone Number", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
          Spacer(modifier = Modifier.height(4.dp))
          OutlinedTextField(
            value = metaPhoneNumberText,
            onValueChange = {
              metaPhoneNumberText = it
              metaErrorMessage = null
            },
            placeholder = { Text("+91 9876543210") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("meta_whatsapp_phone_input")
          )
        }

        OutlinedButton(
          onClick = { showAdvancedMetaConfig = !showAdvancedMetaConfig },
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(if (showAdvancedMetaConfig) "Hide Meta Graph Credentials" else "Custom Meta Credentials (Optional)")
        }

        if (showAdvancedMetaConfig) {
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Text("Graph API Credentials", fontWeight = FontWeight.Bold, fontSize = 13.sp)
              OutlinedTextField(
                value = accessToken,
                onValueChange = { accessToken = it },
                label = { Text("System User Access Token") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("meta_access_token_input")
              )
              OutlinedTextField(
                value = phoneNumberId,
                onValueChange = { phoneNumberId = it },
                label = { Text("Phone Number ID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("meta_phone_id_input")
              )
              OutlinedTextField(
                value = wabaId,
                onValueChange = { wabaId = it },
                label = { Text("WABA Account ID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("meta_waba_id_input")
              )
            }
          }
        }

        if (metaLoading) {
          Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = IncomeGreen)
          }
        } else if (!isMetaConnected) {
          Button(
            onClick = {
              if (metaPhoneNumberText.trim().length < 10) {
                metaErrorMessage = "Please enter a valid phone number with country code."
                return@Button
              }
              metaLoading = true
              metaErrorMessage = null
              viewModel.connectMetaWhatsApp(
                phoneNumber = metaPhoneNumberText.trim(),
                accessToken = accessToken.ifBlank { null },
                phoneNumberId = phoneNumberId.ifBlank { null },
                wabaId = wabaId.ifBlank { null },
                onSuccess = {
                  metaLoading = false
                  Toast.makeText(context, "Meta WhatsApp Cloud API linked!", Toast.LENGTH_SHORT).show()
                },
                onError = { err ->
                  metaLoading = false
                  metaErrorMessage = err
                }
              )
            },
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("link_meta_whatsapp_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Connect Meta Gateway", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
          }
        } else {
          OutlinedButton(
            onClick = {
              metaLoading = true
              metaErrorMessage = null
              viewModel.disconnectMetaWhatsApp(
                onSuccess = {
                  metaLoading = false
                  Toast.makeText(context, "WhatsApp disconnected.", Toast.LENGTH_SHORT).show()
                },
                onError = { err ->
                  metaLoading = false
                  metaErrorMessage = err
                }
              )
            },
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("disconnect_meta_whatsapp_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed)
          ) {
            Text("Disconnect Meta Gateway", fontWeight = FontWeight.Bold)
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))
    }
  }
}
