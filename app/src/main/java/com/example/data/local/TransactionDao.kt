package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.SyncStatus
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
  @Query("SELECT * FROM transactions ORDER BY dateMillis DESC, timeString DESC")
  fun getAllTransactions(): Flow<List<TransactionEntity>>

  @Query("SELECT * FROM transactions WHERE dateString = :date ORDER BY timeString DESC")
  fun getTransactionsByDate(date: String): Flow<List<TransactionEntity>>

  @Query("SELECT * FROM transactions WHERE dateString >= :startDate AND dateString <= :endDate ORDER BY dateMillis DESC")
  fun getTransactionsBetweenDates(startDate: String, endDate: String): Flow<List<TransactionEntity>>

  @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
  suspend fun getTransactionById(id: String): TransactionEntity?

  @Query("SELECT * FROM transactions WHERE syncStatus = 'PENDING'")
  suspend fun getPendingSyncTransactions(): List<TransactionEntity>

  @Query("""
    SELECT COALESCE(SUM(amount), 0.0) 
    FROM transactions 
    WHERE type = 'EXPENSE' AND dateString = :date
  """)
  fun getTodayExpensesFlow(date: String): Flow<Double>

  @Query("""
    SELECT COALESCE(SUM(amount), 0.0) 
    FROM transactions 
    WHERE type = 'INCOME' AND dateString = :date
  """)
  fun getTodayIncomeFlow(date: String): Flow<Double>

  @Query("""
    SELECT COALESCE(SUM(amount), 0.0) 
    FROM transactions 
    WHERE type = 'EXPENSE' AND dateString >= :firstDayOfMonth
  """)
  fun getMonthlyExpensesFlow(firstDayOfMonth: String): Flow<Double>

  @Query("""
    SELECT COALESCE(SUM(amount), 0.0) 
    FROM transactions 
    WHERE type = 'INCOME' AND dateString >= :firstDayOfMonth
  """)
  fun getMonthlyIncomeFlow(firstDayOfMonth: String): Flow<Double>

  @Query("""
    SELECT COALESCE(SUM(amount), 0.0) 
    FROM transactions 
    WHERE type = 'INCOME'
  """)
  fun getTotalIncomeFlow(): Flow<Double>

  @Query("""
    SELECT COALESCE(SUM(amount), 0.0) 
    FROM transactions 
    WHERE type = 'EXPENSE'
  """)
  fun getTotalExpensesFlow(): Flow<Double>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTransaction(transaction: TransactionEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(transactions: List<TransactionEntity>)

  @Update
  suspend fun updateTransaction(transaction: TransactionEntity)

  @Query("UPDATE transactions SET syncStatus = :status, serverId = :serverId WHERE id = :id")
  suspend fun updateSyncStatus(id: String, status: SyncStatus, serverId: String? = null)

  @Delete
  suspend fun deleteTransaction(transaction: TransactionEntity)

  @Query("DELETE FROM transactions WHERE id = :id")
  suspend fun deleteById(id: String)

  @Query("DELETE FROM transactions")
  suspend fun clearAll()
}
