package com.example.data.remote

import com.example.data.model.AiAnalysisRequest
import com.example.data.model.AiAnalysisResponse
import com.example.data.model.AiConfigResponse
import com.example.data.model.AppRestrictionsResponse
import com.example.data.model.AuthResponse
import com.example.data.model.DashboardSummaryResponse
import com.example.data.model.DeviceResetRequest
import com.example.data.model.DeviceStatusResponse
import com.example.data.model.EmergencyReviewRequest
import com.example.data.model.EmergencyReviewResponse
import com.example.data.model.ForgotPasswordRequest
import com.example.data.model.GenericApiResponse
import com.example.data.model.LogRestrictionEventRequest
import com.example.data.model.LoginRequest
import com.example.data.model.MetaWhatsAppConnectRequest
import com.example.data.model.RefreshTokenRequest
import com.example.data.model.RegisterRequest
import com.example.data.model.ResetPasswordRequest
import com.example.data.model.SyncBatchRequest
import com.example.data.model.SyncBatchResponse
import com.example.data.model.TransactionDto
import com.example.data.model.TransactionResponse
import com.example.data.model.UpdateRestrictionsRequest
import com.example.data.model.ValidateAiKeyRequest
import com.example.data.model.WhatsAppStatusResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface FinPulseApiService {
  // 1. Authentication & Device Binding (Tasks 1 & 2)
  @POST("api/auth/register")
  suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

  @POST("api/auth/login")
  suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

  @POST("api/auth/forgot-password")
  suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<GenericApiResponse>

  @POST("api/auth/reset-password")
  suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<GenericApiResponse>

  @POST("api/auth/refresh-token")
  suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<AuthResponse>

  @GET("api/auth/device-status")
  suspend fun checkDeviceStatus(@Query("deviceId") deviceId: String): Response<DeviceStatusResponse>

  @POST("api/auth/device-reset")
  suspend fun requestDeviceReset(@Body request: DeviceResetRequest): Response<GenericApiResponse>

  // 2. Dashboard & Summary
  @GET("api/dashboard/summary")
  suspend fun getDashboardSummary(): Response<DashboardSummaryResponse>

  // 3. Transactions & Sync
  @POST("api/transactions")
  suspend fun createTransaction(@Body transaction: TransactionDto): Response<TransactionResponse>

  @PUT("api/transactions/{id}")
  suspend fun updateTransaction(
    @Path("id") id: String,
    @Body transaction: TransactionDto
  ): Response<TransactionResponse>

  @DELETE("api/transactions/{id}")
  suspend fun deleteTransaction(@Path("id") id: String): Response<GenericApiResponse>

  @POST("api/transactions/sync-batch")
  suspend fun syncBatchTransactions(@Body request: SyncBatchRequest): Response<SyncBatchResponse>

  // 4. Real AI Provider Management (Task 4)
  @GET("api/ai/config")
  suspend fun getAiConfig(): Response<AiConfigResponse>

  @POST("api/ai/validate-and-save")
  suspend fun validateAndSaveAiConfig(@Body request: ValidateAiKeyRequest): Response<AiConfigResponse>

  @DELETE("api/ai/config")
  suspend fun deleteAiConfig(): Response<GenericApiResponse>

  @POST("api/ai/analyze")
  suspend fun analyzeSpending(@Body request: AiAnalysisRequest): Response<AiAnalysisResponse>

  @POST("api/ai/emergency-review")
  suspend fun reviewEmergency(@Body request: EmergencyReviewRequest): Response<EmergencyReviewResponse>

  // 5. Real WhatsApp Integration (Task 3)
  @GET("api/integrations/whatsapp/status")
  suspend fun getWhatsAppStatus(): Response<WhatsAppStatusResponse>

  @POST("api/integrations/whatsapp/connect")
  suspend fun connectWhatsApp(@Body request: MetaWhatsAppConnectRequest): Response<WhatsAppStatusResponse>

  @DELETE("api/integrations/whatsapp/disconnect")
  suspend fun disconnectWhatsApp(): Response<GenericApiResponse>

  // 6. Real App Restrictions & Accessibility Logging (Task 5)
  @GET("api/restrictions")
  suspend fun getRestrictions(): Response<AppRestrictionsResponse>

  @POST("api/restrictions")
  suspend fun updateRestrictions(@Body request: UpdateRestrictionsRequest): Response<GenericApiResponse>

  @POST("api/restrictions/log-event")
  suspend fun logRestrictionEvent(@Body request: LogRestrictionEventRequest): Response<GenericApiResponse>

  // 7. Full Cloud User Dataset Restore (Requirement 5)
  @GET("api/user/sync-all")
  suspend fun pullCloudData(): Response<com.example.data.model.CloudSyncAllResponse>

  // 8. SBI Mini Statement & Pending Transactions (Expense Tracker Upgrade)
  @GET("api/pending-transactions")
  suspend fun getPendingTransactions(): Response<com.example.data.model.PendingTransactionsResponse>

  @POST("api/pending-transactions/confirm")
  suspend fun confirmPendingTransaction(
    @Body request: com.example.data.model.ConfirmPendingTransactionRequest
  ): Response<GenericApiResponse>

  @POST("api/pending-transactions/dismiss")
  suspend fun dismissPendingTransaction(
    @Body request: com.example.data.model.DismissPendingTransactionRequest
  ): Response<GenericApiResponse>

  @GET("api/balance/current")
  suspend fun getRealBalance(): Response<com.example.data.model.RealBalanceResponse>

  @GET("api/balance/history")
  suspend fun getBalanceHistory(
    @Query("limit") limit: Int = 20
  ): Response<com.example.data.model.BalanceHistoryResponse>

  @GET("api/automation/config")
  suspend fun getAutomationConfig(): Response<com.example.data.model.AutomationConfigResponse>

  @POST("api/automation/config")
  suspend fun updateAutomationConfig(
    @Body request: com.example.data.model.UpdateAutomationConfigRequest
  ): Response<com.example.data.model.AutomationConfigResponse>

  // Dynamic Railway Worker direct endpoints
  @GET
  suspend fun getRailwayStatus(
    @retrofit2.http.Url url: String
  ): Response<com.example.data.model.RailwayStatusResponse>

  @POST
  suspend fun requestRailwayPairCode(
    @retrofit2.http.Url url: String,
    @Body request: com.example.data.model.RailwayPairRequest
  ): Response<com.example.data.model.RailwayPairResponse>

  @POST
  suspend fun disconnectRailway(
    @retrofit2.http.Url url: String
  ): Response<GenericApiResponse>

  @POST
  suspend fun runRailwayAutomationNow(
    @retrofit2.http.Url url: String
  ): Response<GenericApiResponse>
}
