package com.example.noti251022.storage

import androidx.room.*

@Dao
interface CardTransactionDao {
    @Insert
    suspend fun insert(transaction: CardTransaction): Long

    @Query("SELECT * FROM card_transactions WHERE cardNumber = :cardNumber AND amount = :amount AND storeName = :storeName AND isCancelled = 0 ORDER BY timestamp DESC LIMIT 1")
    suspend fun findMatchingTransaction(cardNumber: String, amount: String, storeName: String): CardTransaction?

    @Query("UPDATE card_transactions SET isCancelled = 1 WHERE id = :id")
    suspend fun markAsCancelled(id: Long)

    @Query("SELECT * FROM card_transactions WHERE isCancelled = 0 ORDER BY timestamp DESC LIMIT 50")
    suspend fun getRecentTransactions(): List<CardTransaction>
    
    @Query("DELETE FROM card_transactions WHERE timestamp < :cutoffTime")
    suspend fun deleteOldTransactions(cutoffTime: Long)
}
