package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.SyncStatus
import com.example.data.model.TransactionType

class Converters {
  @TypeConverter
  fun fromTransactionType(type: TransactionType): String = type.name

  @TypeConverter
  fun toTransactionType(value: String): TransactionType = try {
    TransactionType.valueOf(value)
  } catch (e: Exception) {
    TransactionType.EXPENSE
  }

  @TypeConverter
  fun fromSyncStatus(status: SyncStatus): String = status.name

  @TypeConverter
  fun toSyncStatus(value: String): SyncStatus = try {
    SyncStatus.valueOf(value)
  } catch (e: Exception) {
    SyncStatus.PENDING
  }
}
