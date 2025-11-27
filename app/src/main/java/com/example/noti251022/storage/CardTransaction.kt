package com.example.noti251022.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "card_transactions")
data class CardTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cardNumber: String,          // "6585" or "7221"
    val amount: String,               // "31,999원"
    val datetime: String,             // "11/21 15:44"
    val storeName: String,            // "쿠팡"
    val messageId: Long,              // 텔레그램 메시지 ID
    val chatId: String,               // 텔레그램 채팅 ID
    val timestamp: Long = System.currentTimeMillis(),
    val isCancelled: Boolean = false  // 취소 여부
)
