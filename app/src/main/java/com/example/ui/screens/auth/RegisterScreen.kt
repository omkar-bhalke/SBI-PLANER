package com.example.ui.screens.auth

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhonelinkLock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentGold
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavySurface
import com.example.ui.theme.PrimaryBlue
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
  viewModel: MainViewModel,
  onRegisterSuccess: (isFirstTime: Boolean) -> Unit,
  onNavigateToLogin: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  BackHandler { onNavigateToLogin() }

  var fullName by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var confirmPassword by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }
  var confirmPasswordVisible by remember { mutableStateOf(false) }

  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  // Password strength calculation
  val hasMinLength = password.length >= 8
  val hasUppercase = password.any { it.isUpperCase() }
  val hasDigit = password.any { it.isDigit() }
  val hasSpecial = password.any { !it.isLetterOrDigit() }
  val strengthScore = listOf(hasMinLength, hasUppercase, hasDigit, hasSpecial).count { it }

  val strengthColor = when (strengthScore) {
    0, 1 -> ExpenseRed
    2, 3 -> Color(0xFFF59E0B) // Amber
    else -> IncomeGreen
  }
  val strengthLabel = when (strengthScore) {
    0, 1 -> "Weak"
    2, 3 -> "Moderate"
    else -> "Strong"
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Create FinPulse Account", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        navigationIcon = {
          IconButton(onClick = onNavigateToLogin) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Login", tint = Color.White)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = NavyDark,
          titleContentColor = Color.White
        )
      )
    },
    containerColor = NavyDark
  ) { innerPadding ->
    Column(
      modifier = modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 24.dp)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      Spacer(modifier = Modifier.height(4.dp))

      // Hardware Device Binding Card (Task 2)
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(38.dp)
              .clip(CircleShape)
              .background(AccentGold.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.PhonelinkLock, contentDescription = null, tint = AccentGold, modifier = Modifier.size(20.dp))
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = "One Device = One Account Policy",
              color = Color.White,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Hardware ID: ${viewModel.deviceId.take(14)}... (Keystore Protected)",
              color = Color(0xFF94A3B8),
              fontSize = 11.sp
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
          Text(
            text = errorMessage!!,
            color = ExpenseRed,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(12.dp)
          )
        }
      }

      // 1. Full Name
      OutlinedTextField(
        value = fullName,
        onValueChange = {
          fullName = it
          errorMessage = null
        },
        label = { Text("Full Name", color = Color(0xFF94A3B8)) },
        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = AccentGold) },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = Color.White,
          unfocusedTextColor = Color.White,
          focusedBorderColor = AccentGold,
          unfocusedBorderColor = Color(0xFF334155)
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("register_fullname_input")
      )

      // 2. Email Address
      OutlinedTextField(
        value = email,
        onValueChange = {
          email = it
          errorMessage = null
        },
        label = { Text("Email Address (Gmail or personal)", color = Color(0xFF94A3B8)) },
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
          .testTag("register_email_input")
      )

      // 3. Password
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
          .testTag("register_password_input")
      )

      // Password Strength Indicator
      if (password.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Strength: $strengthLabel", color = strengthColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("${strengthScore * 25}%", color = strengthColor, fontSize = 11.sp)
          }
          LinearProgressIndicator(
            progress = { strengthScore / 4f },
            color = strengthColor,
            trackColor = Color(0xFF334155),
            modifier = Modifier
              .fillMaxWidth()
              .height(4.dp)
              .clip(RoundedCornerShape(2.dp))
          )
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StrengthCheckItem("8+ chars", hasMinLength)
            StrengthCheckItem("Uppercase", hasUppercase)
            StrengthCheckItem("Number", hasDigit)
            StrengthCheckItem("Symbol", hasSpecial)
          }
        }
      }

      // 4. Confirm Password
      OutlinedTextField(
        value = confirmPassword,
        onValueChange = {
          confirmPassword = it
          errorMessage = null
        },
        label = { Text("Confirm Password", color = Color(0xFF94A3B8)) },
        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = AccentGold) },
        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
          IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
            Icon(
              imageVector = if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
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
          .testTag("register_confirm_password_input")
      )

      Spacer(modifier = Modifier.height(10.dp))

      // Register Button
      if (isLoading) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator(color = AccentGold)
        }
      } else {
        Button(
          onClick = {
            if (fullName.trim().length < 2) {
              errorMessage = "Please enter your full name."
              return@Button
            }
            if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
              errorMessage = "Please enter a valid email address."
              return@Button
            }
            if (password.length < 8) {
              errorMessage = "Password must be at least 8 characters long."
              return@Button
            }
            if (password != confirmPassword) {
              errorMessage = "Passwords do not match."
              return@Button
            }

            isLoading = true
            errorMessage = null
            viewModel.register(
              name = fullName.trim(),
              email = email.trim(),
              pass = password,
              confirmPass = confirmPassword,
              onSuccess = { isFirstTime ->
                isLoading = false
                Toast.makeText(context, "Registration successful!", Toast.LENGTH_SHORT).show()
                onRegisterSuccess(isFirstTime)
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
            .testTag("register_submit_button"),
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = Color(0xFF0F172A))
        ) {
          Text("Register Account & Bind Device", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
      }

      // Back to Login link
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Already registered?", color = Color(0xFF94A3B8), fontSize = 13.sp)
        TextButton(onClick = onNavigateToLogin) {
          Text("Log In Here", color = AccentGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
      }

      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}

@Composable
private fun StrengthCheckItem(label: String, passed: Boolean) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Icon(
      imageVector = if (passed) Icons.Default.Check else Icons.Default.Close,
      contentDescription = null,
      tint = if (passed) IncomeGreen else Color(0xFF64748B),
      modifier = Modifier.size(12.dp)
    )
    Spacer(modifier = Modifier.width(2.dp))
    Text(
      text = label,
      fontSize = 10.sp,
      color = if (passed) IncomeGreen else Color(0xFF94A3B8)
    )
  }
}
