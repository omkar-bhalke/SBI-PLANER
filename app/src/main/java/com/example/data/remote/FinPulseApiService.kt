package com.example.data.remote

import com.example.data.model.AiAnalysisRequest
import com.example.data.model.AiAnalysisResponse
import com.example.data.model.AuthRequest
import com.example.data.model.AuthResponse
import com.example.data.model.DashboardSummaryResponse
import com.example.data.model.EmergencyReviewRequest
import com.example.data.model.EmergencyReviewResponse
import com.example.data.model.GenericApiResponse
import com.example.data.model.SyncBatchRequest
import com.example.data.model.SyncBatchResponse
import com.example.data.model.TransactionDto
import com.example.data.model.TransactionResponse
import com.example.data.model.WhatsAppConnectRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface FinPulseApiService {
  @POST("api/auth/google")
  suspend fun loginWithGoogle(@Body request: AuthRequest): Response<AuthResponse>

  @GET("api/dashboard/summary")
  suspend fun getDashboardSummary(): Response<DashboardSummaryResponse>

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

  @POST("api/ai/analyze")
  suspend fun analyzeSpending(@Body request: AiAnalysisRequest): Response<AiAnalysisResponse>

  @POST("api/ai/emergency-review")
  suspend fun reviewEmergency(@Body request: EmergencyReviewRequest): Response<EmergencyReviewResponse>

  @POST("api/integrations/whatsapp/connect")
  suspend fun connectWhatsApp(@Body request: WhatsAppConnectRequest): Response<GenericApiResponse>

  @DELETE("api/integrations/whatsapp/disconnect")
  suspend fun disconnectWhatsApp(): Response<GenericApiResponse>
}
