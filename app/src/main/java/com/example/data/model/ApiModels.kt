package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AuthRequest(
  val idToken: String? = null,
  val email: String? = null,
  val name: String? = null
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
  val success: Boolean,
  val token: String? = null,
  val isFirstTime: Boolean = false,
  val message: String? = null
)

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

@JsonClass(generateAdapter = true)
data class AiAnalysisRequest(
  val customQuery: String? = null
)

@JsonClass(generateAdapter = true)
data class AiAnalysisResponse(
  val success: Boolean,
  val analysis: String,
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

@JsonClass(generateAdapter = true)
data class WhatsAppConnectRequest(
  val phoneNumber: String
)

@JsonClass(generateAdapter = true)
data class GenericApiResponse(
  val success: Boolean,
  val message: String? = null
)
