package com.example.ui.screens.auth

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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhonelinkLock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentGold
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavySurface
import com.example.ui.theme.PrimaryBlue
import com.example.viewmodel.MainViewModel

@Composable
fun LoginScreen(
  viewModel: MainViewModel,
  onLoginSuccess: (isFirstTime: Boolean) -> Unit,
  onNavigateToRegister: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var rememberMe by remember { mutableStateOf(true) }
  var passwordVisible by remember { mutableStateOf(false) }

  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var showForgotPasswordDialog by remember { mutableStateOf(false) }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(NavyDark)
      .padding(horizontal = 24.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Spacer(modifier = Modifier.height(24.dp))

      // Top Logo & Branding
      Box(
        modifier = Modifier
          .size(76.dp)
          .clip(RoundedCornerShape(22.dp))
          .background(NavySurface),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.TrendingUp,
          contentDescription = "FinPulse Logo",
          tint = AccentGold,
          modifier = Modifier.size(42.dp)
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      Text(
        text = "FinPulse",
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
      )

      Text(
        text = "Secure Financial Intelligence & Budget Governance",
        fontSize = 12.sp,
        color = Color(0xFF94A3B8),
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(24.dp))

      // Hardware Device Security Pill
      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.PhonelinkLock,
            contentDescription = null,
            tint = AccentGold,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Hardware Device Bound: ${viewModel.deviceId.take(16)}...",
            fontSize = 11.sp,
            color = Color(0xFFCBD5E1)
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Error banner
      if (errorMessage != null) {
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = ExpenseRed.copy(alpha = 0.15f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = errorMessage!!,
            color = ExpenseRed,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(12.dp)
          )
        }
        Spacer(modifier = Modifier.height(12.dp))
      }

      // Email field
      OutlinedTextField(
        value = email,
        onValueChange = {
          email = it
          errorMessage = null
        },
        label = { Text("Email Address", color = Color(0xFF94A3B8)) },
        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = AccentGold) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = Color.White,
          unfocusedTextColor = Color.White,
          focusedBorderColor = AccentGold,
          unfocusedBorderColor = Color(0xFF334155)
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("login_email_input")
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Password field
      OutlinedTextField(
        value = password,
        onValueChange = {
          password = it
          errorMessage = null
        },
        label = { Text("Password", color = Color(0xFF94A3B8)) },
        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = AccentGold) },
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
          IconButton(onClick = { passwordVisible = !passwordVisible }) {
            Icon(
              imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
              contentDescription = null,
              tint = Color(0xFF94A3B8)
            )
          }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = Color.White,
          unfocusedTextColor = Color.White,
          focusedBorderColor = AccentGold,
          unfocusedBorderColor = Color(0xFF334155)
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("login_password_input")
      )

      // Remember Me & Forgot Password Row
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.clickable { rememberMe = !rememberMe }
        ) {
          Checkbox(
            checked = rememberMe,
            onCheckedChange = { rememberMe = it },
            colors = CheckboxDefaults.colors(checkedColor = AccentGold)
          )
          Text("Remember Me", color = Color(0xFFCBD5E1), fontSize = 12.sp)
        }

        TextButton(
          onClick = { showForgotPasswordDialog = true },
          modifier = Modifier.testTag("forgot_password_button")
        ) {
          Text("Forgot Password?", color = AccentGold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Login Button
      if (isLoading) {
        CircularProgressIndicator(color = AccentGold, modifier = Modifier.size(36.dp))
      } else {
        Button(
          onClick = {
            if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
              errorMessage = "Please enter a valid email address."
              return@Button
            }
            if (password.isBlank()) {
              errorMessage = "Please enter your password."
              return@Button
            }

            isLoading = true
            errorMessage = null
            viewModel.loginWithEmailPassword(
              email = email.trim(),
              pass = password,
              rememberMe = rememberMe,
              onSuccess = { isFirstTime ->
                isLoading = false
                onLoginSuccess(isFirstTime)
              },
              onError = { err ->
                isLoading = false
                errorMessage = err
              }
            )
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("login_submit_button"),
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
        ) {
          Text(
            text = "Sign In",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Register link
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Don't have an account?", color = Color(0xFF94A3B8), fontSize = 13.sp)
        TextButton(
          onClick = onNavigateToRegister,
          modifier = Modifier.testTag("create_account_button")
        ) {
          Text("Create Account", color = AccentGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
      }

      Spacer(modifier = Modifier.height(24.dp))
    }
  }

  // Forgot Password Dialog
  if (showForgotPasswordDialog) {
    ForgotPasswordDialog(
      initialEmail = email,
      viewModel = viewModel,
      onDismiss = { showForgotPasswordDialog = false }
    )
  }
}
