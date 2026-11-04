package com.example.ui.screens.settings

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VpnKey
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.NavySurface
import com.example.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsAppLinkingScreen(
  viewModel: SettingsViewModel,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val settings by viewModel.userSettings.collectAsState()
  val whatsAppStatus by viewModel.whatsAppStatus.collectAsState()

  var phoneNumberText by remember {
    mutableStateOf(settings.whatsAppPhoneNumber ?: "+91 ")
  }
  var accessToken by remember { mutableStateOf("") }
  var phoneNumberId by remember { mutableStateOf("") }
  var wabaId by remember { mutableStateOf("") }
  var showAdvancedMetaConfig by remember { mutableStateOf(false) }

  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  val isConnected = whatsAppStatus?.connected == true || settings.isWhatsAppConnected
  val statusText = when {
    isConnected -> "CONNECTED"
    whatsAppStatus?.status == "CONNECTION_FAILED" -> "CONNECTION FAILED"
    whatsAppStatus?.status == "DISCONNECTED" -> "DISCONNECTED"
    else -> "NOT CONNECTED"
  }

  val statusBadgeColor = when {
    isConnected -> IncomeGreen
    whatsAppStatus?.status == "CONNECTION_FAILED" -> ExpenseRed
    else -> Color(0xFF64748B)
  }

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
        .padding(horizontal = 20.dp, vertical = 12.dp)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Official Meta API Architecture Banner (Task 3 Compliance)
      Card(
        shape = RoundedCornerShape(16.dp),
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
              text = "Meta WhatsApp Cloud API Gateway",
              color = Color.White,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold
            )
          }
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = "Notice: The official Meta WhatsApp Business Cloud API requires Graph API credentials. Meta does NOT provide arbitrary personal-account QR linking via public APIs without unauthorized third-party scrapers that violate Meta TOS. FinPulse connects to legitimate Meta Cloud API webhooks.",
            color = Color(0xFF94A3B8),
            fontSize = 12.sp,
            lineHeight = 18.sp
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
            Text(text = "Integration Status", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(
              text = if (isConnected) "Active with ${whatsAppStatus?.phoneNumber ?: settings.whatsAppPhoneNumber}" else "No active gateway link",
              fontSize = 12.sp,
              color = if (isConnected) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(statusBadgeColor.copy(alpha = 0.15f))
              .padding(horizontal = 10.dp, vertical = 5.dp)
          ) {
            Text(
              text = statusText,
              color = statusBadgeColor,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }

      if (errorMessage != null) {
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = ExpenseRed.copy(alpha = 0.15f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Error, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = errorMessage!!, color = ExpenseRed, fontSize = 12.sp)
          }
        }
      }

      // Phone Input Field
      Column {
        Text(
          text = "WhatsApp Business Phone Number",
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
          value = phoneNumberText,
          onValueChange = {
            phoneNumberText = it
            errorMessage = null
          },
          placeholder = { Text("+91 9876543210") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("whatsapp_phone_input")
        )
      }

      // Advanced Meta Graph API Configuration Toggle
      OutlinedButton(
        onClick = { showAdvancedMetaConfig = !showAdvancedMetaConfig },
        modifier = Modifier.fillMaxWidth()
      ) {
        Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(if (showAdvancedMetaConfig) "Hide Meta Graph Credentials" else "Custom Meta Cloud API Credentials (Optional)")
      }

      if (showAdvancedMetaConfig) {
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
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

      // Actions
      if (isLoading) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator(color = IncomeGreen)
        }
      } else if (!isConnected) {
        Button(
          onClick = {
            if (phoneNumberText.trim().length < 10) {
              errorMessage = "Please enter a valid phone number with country code."
              return@Button
            }
            isLoading = true
            errorMessage = null
            viewModel.connectMetaWhatsApp(
              phoneNumber = phoneNumberText.trim(),
              accessToken = accessToken.ifBlank { null },
              phoneNumberId = phoneNumberId.ifBlank { null },
              wabaId = wabaId.ifBlank { null },
              onSuccess = {
                isLoading = false
                Toast.makeText(context, "Meta WhatsApp Cloud API linked!", Toast.LENGTH_SHORT).show()
              },
              onError = { err ->
                isLoading = false
                errorMessage = err
              }
            )
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("link_whatsapp_button"),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Connect WhatsApp Gateway", fontSize = 14.sp, fontWeight = FontWeight.Bold)
          }
        }
      } else {
        OutlinedButton(
          onClick = {
            isLoading = true
            errorMessage = null
            viewModel.disconnectMetaWhatsApp(
              onSuccess = {
                isLoading = false
                Toast.makeText(context, "WhatsApp disconnected.", Toast.LENGTH_SHORT).show()
              },
              onError = { err ->
                isLoading = false
                errorMessage = err
              }
            )
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("disconnect_whatsapp_button"),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed)
        ) {
          Text("Disconnect WhatsApp Gateway", fontWeight = FontWeight.Bold)
        }
      }

      Spacer(modifier = Modifier.height(20.dp))
    }
  }
}
