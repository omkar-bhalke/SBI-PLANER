package com.example.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.local.TransactionDao
import com.example.data.model.SyncBatchRequest
import com.example.data.model.SyncStatus
import com.example.data.model.TransactionDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncManager(
  private val context: Context,
  private val transactionDao: TransactionDao
) {
  fun isNetworkAvailable(): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
      ?: return false
    val activeNetwork = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
  }

  suspend fun syncPendingTransactions(): Int = withContext(Dispatchers.IO) {
    if (!isNetworkAvailable()) {
      return@withContext 0
    }

    val pending = transactionDao.getPendingSyncTransactions()
    if (pending.isEmpty()) {
      return@withContext 0
    }

    try {
      val dtoList = pending.map { entity ->
        TransactionDto(
          amount = entity.amount,
          type = entity.type.name,
          description = entity.description,
          categoryName = entity.categoryName,
          categoryId = entity.categoryId,
          transactionDate = entity.dateString,
          transactionTime = entity.timeString,
          timestamp = entity.dateMillis,
          notes = entity.notes,
          idempotencyKey = entity.idempotencyKey,
          clientLocalId = entity.id
        )
      }

      val response = NetworkClient.apiService.syncBatchTransactions(SyncBatchRequest(dtoList))
      if (response.isSuccessful && response.body()?.success == true) {
        val results = response.body()?.results.orEmpty()
        for (res in results) {
          if (res.clientLocalId != null) {
            transactionDao.updateSyncStatus(
              id = res.clientLocalId,
              status = SyncStatus.SYNCED,
              serverId = res.serverId
            )
          }
        }
        return@withContext results.size
      }
    } catch (e: Exception) {
      // Sync failed, records remain PENDING for next retry
    }
    return@withContext 0
  }
}
