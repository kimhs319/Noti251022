package com.example.noti251022.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "failed_messages")
data class FailedMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val senderName: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)
