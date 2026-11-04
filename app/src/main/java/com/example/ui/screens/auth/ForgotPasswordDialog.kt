package com.example.ui.screens.auth

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.PrimaryBlue
import com.example.viewmodel.MainViewModel

@Composable
fun ForgotPasswordDialog(
  initialEmail: String = "",
  viewModel: MainViewModel,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  var step by remember { mutableStateOf(1) } // 1 = enter email, 2 = enter OTP and new password
  var email by remember { mutableStateOf(initialEmail) }
  var otp by remember { mutableStateOf("") }
  var newPassword by remember { mutableStateOf("") }
  var confirmPassword by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var statusNotice by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    shape = RoundedCornerShape(20.dp),
    title = {
      Text(
        text = if (step == 1) "Forgot Password" else "Reset Password",
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        if (errorMessage != null) {
          Text(
            text = errorMessage!!,
            color = ExpenseRed,
            fontSize = 12.sp,
            lineHeight = 16.sp
          )
        }

        if (statusNotice != null) {
          Text(
            text = statusNotice!!,
            color = PrimaryBlue,
            fontSize = 12.sp,
            lineHeight = 16.sp
          )
        }

        if (step == 1) {
          Text(
            text = "Enter your registered email address. We will verify your account and provide a 6-digit OTP to reset your password.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          OutlinedTextField(
            value = email,
            onValueChange = {
              email = it
              errorMessage = null
            },
            label = { Text("Email Address") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("forgot_pass_email_input")
          )
        } else {
          Text(
            text = "Enter the 6-digit OTP code sent for $email and choose a new secure password.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          OutlinedTextField(
            value = otp,
            onValueChange = {
              if (it.length <= 6) otp = it
              errorMessage = null
            },
            label = { Text("6-Digit OTP Code") },
            leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("forgot_pass_otp_input")
          )

          OutlinedTextField(
            value = newPassword,
            onValueChange = {
              newPassword = it
              errorMessage = null
            },
            label = { Text("New Password (min 8 chars)") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
              IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                  imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                  contentDescription = null
                )
              }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("forgot_pass_new_password_input")
          )

          OutlinedTextField(
            value = confirmPassword,
            onValueChange = {
              confirmPassword = it
              errorMessage = null
            },
            label = { Text("Confirm New Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("forgot_pass_confirm_password_input")
          )
        }
      }
    },
    confirmButton = {
      if (isLoading) {
        CircularProgressIndicator(modifier = Modifier.padding(8.dp))
      } else {
        Button(
          onClick = {
            if (step == 1) {
              if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                errorMessage = "Please enter a valid email address."
                return@Button
              }
              isLoading = true
              errorMessage = null
              viewModel.forgotPassword(
                email = email.trim(),
                onSuccess = { msg ->
                  isLoading = false
                  statusNotice = msg
                  step = 2
                },
                onError = { err ->
                  isLoading = false
                  errorMessage = err
                }
              )
            } else {
              if (otp.trim().length != 6) {
                errorMessage = "Please enter the 6-digit OTP."
                return@Button
              }
              if (newPassword.length < 8) {
                errorMessage = "Password must be at least 8 characters."
                return@Button
              }
              if (newPassword != confirmPassword) {
                errorMessage = "Passwords do not match."
                return@Button
              }
              isLoading = true
              errorMessage = null
              viewModel.resetPassword(
                email = email.trim(),
                otp = otp.trim(),
                newPass = newPassword,
                confirmPass = confirmPassword,
                onSuccess = { msg ->
                  isLoading = false
                  Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                  onDismiss()
                },
                onError = { err ->
                  isLoading = false
                  errorMessage = err
                }
              )
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
          modifier = Modifier.testTag("forgot_pass_submit_button")
        ) {
          Text(if (step == 1) "Send OTP" else "Reset Password")
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
