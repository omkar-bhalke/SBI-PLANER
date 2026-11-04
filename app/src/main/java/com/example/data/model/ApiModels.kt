package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ==========================================
// AUTHENTICATION & DEVICE BINDING MODELS
// ==========================================

@JsonClass(generateAdapter = true)
data class RegisterRequest(
  val name: String,
  val email: String,
  val password: String,
  val confirmPassword: String,
  val deviceId: String,
  val deviceFingerprint: String? = null,
  val deviceModel: String? = null,
  val osVersion: String? = null
)

@JsonClass(generateAdapter = true)
data class LoginRequest(
  val email: String,
  val password: String,
  val rememberMe: Boolean = true,
  val deviceId: String? = null,
  val deviceFingerprint: String? = null
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
  val success: Boolean,
  val token: String? = null,
  val refreshToken: String? = null,
  val isFirstTime: Boolean = false,
  val message: String? = null,
  val code: String? = null,
  val user: UserDto? = null
)

@JsonClass(generateAdapter = true)
data class UserDto(
  val id: String,
  val email: String,
  val name: String,
  val profileImage: String? = null
)

@JsonClass(generateAdapter = true)
data class ForgotPasswordRequest(
  val email: String
)

@JsonClass(generateAdapter = true)
data class ResetPasswordRequest(
  val email: String,
  val otp: String,
  val newPassword: String,
  val confirmPassword: String
)

@JsonClass(generateAdapter = true)
data class RefreshTokenRequest(
  val refreshToken: String
)

@JsonClass(generateAdapter = true)
data class DeviceStatusResponse(
  val success: Boolean,
  val isBound: Boolean = false,
  val status: String? = null,
  val boundToMaskedEmail: String? = null,
  val message: String? = null
)

@JsonClass(generateAdapter = true)
data class DeviceResetRequest(
  val email: String,
  val password: String,
  val deviceId: String,
  val reason: String? = null
)

// ==========================================
// TRANSACTION & SYNC MODELS
// ==========================================

@JsonClass(generateAdapter = true)
data class TransactionDto(
  val id: String? = null,
  val amount: Double,
  val type: String,
  val description: String,
  val categoryName: String,
  val categoryId: String? = null,
  val transactionDate: String,
  val transactionTime: String,
  val timestamp: Long? = null,
  val notes: String? = null,
  val idempotencyKey: String? = null,
  val clientLocalId: String? = null
)

@JsonClass(generateAdapter = true)
data class TransactionResponse(
  val success: Boolean,
  val transaction: TransactionDto? = null,
  val message: String? = null
)

@JsonClass(generateAdapter = true)
data class SyncBatchRequest(
  val pendingTransactions: List<TransactionDto>
)

@JsonClass(generateAdapter = true)
data class SyncBatchResult(
  val clientLocalId: String?,
  val serverId: String?,
  val status: String
)

@JsonClass(generateAdapter = true)
data class SyncBatchResponse(
  val success: Boolean,
  val results: List<SyncBatchResult>? = null
)

// ==========================================
// DASHBOARD & SUMMARY MODELS
// ==========================================

@JsonClass(generateAdapter = true)
data class DashboardSummaryResponse(
  val success: Boolean,
  val data: DashboardSummaryData? = null
)

@JsonClass(generateAdapter = true)
data class DashboardSummaryData(
  val balance: BalanceData? = null,
  val today: TodayData? = null,
  val monthly: MonthlyData? = null,
  val recentTransactions: List<TransactionDto>? = null
)

@JsonClass(generateAdapter = true)
data class BalanceData(
  val currentBalance: Double = 0.0,
  val initialBalance: Double = 0.0,
  val totalIncome: Double = 0.0,
  val totalExpenses: Double = 0.0,
  val bankName: String = "SBI"
)

@JsonClass(generateAdapter = true)
data class TodayData(
  val income: Double = 0.0,
  val expense: Double = 0.0,
  val dailyLimit: Double = 500.0,
  val remainingAllowance: Double = 500.0,
  val utilizationPercentage: Double = 0.0,
  val isExceeded: Boolean = false
)

@JsonClass(generateAdapter = true)
data class MonthlyData(
  val income: Double = 0.0,
  val expense: Double = 0.0,
  val savings: Double = 0.0,
  val spendingPercentage: Double = 0.0
)

// ==========================================
// REAL AI MODELS (TASK 4)
// ==========================================

@JsonClass(generateAdapter = true)
data class ValidateAiKeyRequest(
  val provider: String, // GEMINI, OPENAI
  val apiKey: String
)

@JsonClass(generateAdapter = true)
data class AiConfigResponse(
  val success: Boolean,
  val config: AiConfigDto? = null,
  val message: String? = null
)

@JsonClass(generateAdapter = true)
data class AiConfigDto(
  val provider: String = "GEMINI",
  val isConfigured: Boolean = false,
  val keyStatus: String = "UNCHECKED",
  val lastValidatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class AiAnalysisRequest(
  val customQuery: String? = null
)

@JsonClass(generateAdapter = true)
data class AiAnalysisResponse(
  val success: Boolean,
  val provider: String? = null,
  val analysis: String,
  val requiresConfiguration: Boolean = false,
  val message: String? = null
)

@JsonClass(generateAdapter = true)
data class EmergencyReviewRequest(
  val requestedAmount: Double,
  val reason: String,
  val screenshotUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class EmergencyReviewResponse(
  val success: Boolean,
  val recommendation: String,
  val confidenceScore: Double = 0.0,
  val suggestedApproval: Boolean = false,
  val requiresUserConfirmation: Boolean = true
)

// ==========================================
// REAL WHATSAPP MODELS (TASK 3)
// ==========================================

@JsonClass(generateAdapter = true)
data class MetaWhatsAppConnectRequest(
  val phoneNumber: String,
  val accessToken: String? = null,
  val phoneNumberId: String? = null,
  val wabaId: String? = null
)

@JsonClass(generateAdapter = true)
data class WhatsAppStatusResponse(
  val success: Boolean,
  val status: String = "NOT_CONNECTED", // NOT_CONNECTED, CONNECTING, CONNECTED, CONNECTION_FAILED, DISCONNECTED
  val phoneNumber: String? = null,
  val verifiedName: String? = null,
  val lastSyncedAt: String? = null,
  val errorMessage: String? = null,
  val message: String? = null
) {
  val isConnected: Boolean get() = status == "CONNECTED"
  val connected: Boolean get() = status == "CONNECTED"
}

// ==========================================
// REAL APP RESTRICTIONS MODELS (TASK 5)
// ==========================================

@JsonClass(generateAdapter = true)
data class AppRestrictionItem(
  val packageName: String,
  val appName: String,
  val isEnabled: Boolean = true
)

@JsonClass(generateAdapter = true)
data class AppRestrictionsResponse(
  val success: Boolean,
  val restrictions: List<AppRestrictionItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class UpdateRestrictionsRequest(
  val restrictions: List<AppRestrictionItem>
)

@JsonClass(generateAdapter = true)
data class LogRestrictionEventRequest(
  val packageName: String,
  val appName: String,
  val dailyLimit: Double,
  val amountSpent: Double,
  val amountExceeded: Double,
  val actionTaken: String = "BLOCKED"
)

@JsonClass(generateAdapter = true)
data class GenericApiResponse(
  val success: Boolean,
  val message: String? = null,
  val code: String? = null
)

// ==========================================
// CLOUD RESTORE & SYNC ALL MODELS (REQUIREMENT 5)
// ==========================================

@JsonClass(generateAdapter = true)
data class CloudSyncAllResponse(
  val success: Boolean,
  val message: String? = null,
  val transactions: List<TransactionDto>? = null,
  val categories: List<CategoryDto>? = null,
  val settings: CloudSettingsDto? = null,
  val account: CloudAccountDto? = null,
  val whatsApp: CloudWhatsAppDto? = null
)

@JsonClass(generateAdapter = true)
data class CategoryDto(
  val id: String,
  val name: String,
  val type: String,
  val iconName: String? = "category",
  val colorHex: String? = "#1E3E62",
  val isDefault: Boolean = false
)

@JsonClass(generateAdapter = true)
data class CloudSettingsDto(
  val dailySpendingLimit: Double? = 500.0,
  val selectedBank: String? = "State Bank of India (SBI)",
  val currency: String? = "INR",
  val currencySymbol: String? = "₹",
  val themeMode: String? = "SYSTEM",
  val aiProvider: String? = "GEMINI",
  val onboardingCompleted: Boolean? = false
)

@JsonClass(generateAdapter = true)
data class CloudAccountDto(
  val initialBalance: Double = 0.0,
  val currentBalance: Double = 0.0,
  val bankName: String = "State Bank of India (SBI)"
)

@JsonClass(generateAdapter = true)
data class CloudWhatsAppDto(
  val connected: Boolean = false,
  val phoneNumber: String? = null,
  val status: String? = "NOT_CONNECTED"
)

