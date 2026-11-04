package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class TransactionType {
  INCOME,
  EXPENSE
}

enum class SyncStatus {
  SYNCED,
  PENDING,
  FAILED
}

@Entity(tableName = "transactions")
data class TransactionEntity(
  @PrimaryKey
  val id: String = UUID.randomUUID().toString(),
  val serverId: String? = null,
  val type: TransactionType,
  val amount: Double,
  val description: String,
  val categoryId: String? = null,
  val categoryName: String,
  val dateMillis: Long = System.currentTimeMillis(),
  val dateString: String, // YYYY-MM-DD
  val timeString: String, // HH:MM
  val notes: String? = null,
  val syncStatus: SyncStatus = SyncStatus.PENDING,
  val createdAt: Long = System.currentTimeMillis(),
  val idempotencyKey: String = UUID.randomUUID().toString()
)
