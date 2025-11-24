package com.example.noti251022.storage

import androidx.room.*

@Dao
interface FailedMessageDao {
    @Insert
    suspend fun insert(message: FailedMessage): Long

    @Query("SELECT * FROM failed_messages ORDER BY timestamp ASC LIMIT 1")
    suspend fun getOldestMessage(): FailedMessage?

    @Query("DELETE FROM failed_messages WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM failed_messages")
    suspend fun getPendingCount(): Int
    
    @Query("SELECT * FROM failed_messages ORDER BY timestamp DESC LIMIT 50")
    suspend fun getRecentMessages(): List<FailedMessage>
}
