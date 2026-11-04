package com.example.data.model

data class UserSettings(
  val userId: String = "",
  val userEmail: String = "",
  val userName: String = "",
  val userPhotoUrl: String? = null,
  val currency: String = "INR",
  val currencySymbol: String = "₹",
  val initialBalance: Double = 0.0,
  val dailySpendingLimit: Double = 500.0,
  val selectedBank: String = "State Bank of India (SBI)",
  val onboardingCompleted: Boolean = false,
  val themeMode: String = "SYSTEM", // LIGHT, DARK, SYSTEM
  val aiProvider: String = "GEMINI", // GEMINI, OPENAI
  val backendUrl: String = "https://finpulse-api.railway.app",
  val authToken: String? = null,
  val isWhatsAppConnected: Boolean = false,
  val whatsAppPhoneNumber: String? = null
)
