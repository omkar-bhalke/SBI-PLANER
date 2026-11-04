package com.example.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.local.CategoryDao
import com.example.data.local.PreferenceManager
import com.example.data.local.TransactionDao
import com.example.data.model.CategoryEntity
import com.example.data.model.SyncBatchRequest
import com.example.data.model.SyncStatus
import com.example.data.model.TransactionDto
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
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

  /**
   * Fulfills Requirement 5: Complete data recovery from cPanel MySQL after app reinstall
   */
  suspend fun restoreAllDataFromCloud(
    categoryDao: CategoryDao,
    preferenceManager: PreferenceManager
  ): Int = withContext(Dispatchers.IO) {
    if (!isNetworkAvailable()) {
      return@withContext 0
    }

    try {
      val response = NetworkClient.apiService.pullCloudData()
      if (response.isSuccessful && response.body()?.success == true) {
        val body = response.body()!!

        // 1. Restore Financial Settings & Account Balances
        val settings = body.settings
        val account = body.account
        if (settings != null && account != null) {
          preferenceManager.restoreCloudFinancialSetup(
            dailyLimit = settings.dailySpendingLimit ?: 500.0,
            initialBalance = account.initialBalance,
            bankName = account.bankName,
            onboardingDone = settings.onboardingCompleted ?: true
          )
        }

        // 2. Restore WhatsApp Configuration
        body.whatsApp?.let { wa ->
          preferenceManager.updateWhatsAppStatus(wa.connected, wa.phoneNumber)
        }

        // 3. Restore Categories
        body.categories?.let { catDtos ->
          val catEntities = catDtos.map { dto ->
            CategoryEntity(
              id = dto.id,
              name = dto.name,
              type = if (dto.type.equals("INCOME", ignoreCase = true)) TransactionType.INCOME else TransactionType.EXPENSE,
              iconName = dto.iconName ?: "category",
              colorHex = dto.colorHex ?: "#1E3E62",
              isDefault = dto.isDefault
            )
          }
          if (catEntities.isNotEmpty()) {
            categoryDao.insertAllCategories(catEntities)
          }
        }

        // 4. Restore Transactions
        val txDtos = body.transactions.orEmpty()
        if (txDtos.isNotEmpty()) {
          val txEntities = txDtos.map { dto ->
            TransactionEntity(
              id = dto.clientLocalId ?: dto.id ?: "tx_${System.currentTimeMillis()}",
              serverId = dto.id,
              amount = dto.amount,
              type = if (dto.type.equals("INCOME", ignoreCase = true)) TransactionType.INCOME else TransactionType.EXPENSE,
              description = dto.description,
              categoryName = dto.categoryName,
              categoryId = dto.categoryId,
              dateMillis = dto.timestamp ?: System.currentTimeMillis(),
              dateString = dto.transactionDate,
              timeString = dto.transactionTime,
              notes = dto.notes,
              syncStatus = SyncStatus.SYNCED,
              idempotencyKey = dto.idempotencyKey ?: ("idem_" + dto.id)
            )
          }
          transactionDao.insertAll(txEntities)
          return@withContext txEntities.size
        }
      }
    } catch (_: Exception) {}
    return@withContext 0
  }
}
