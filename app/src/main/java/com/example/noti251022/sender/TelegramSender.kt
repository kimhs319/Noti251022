package com.example.noti251022.sender

import android.content.Context
import androidx.work.*
import com.example.noti251022.model.Sender
import com.example.noti251022.storage.AppDatabase
import com.example.noti251022.storage.CardTransaction
import com.example.noti251022.storage.FailedMessage
import com.example.noti251022.util.AppLogger
import com.example.noti251022.worker.TelegramRetryWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

object TelegramSender {
    private const val TAG = "TgSender"
    
    // 일반 메시지 전송
    fun sendTelegram(context: Context, sender: Sender, message: String) {
        val token = sender.token
        val chatId = sender.chatId
        
        if (token.isNullOrEmpty() || chatId.isNullOrEmpty()) {
            AppLogger.error("[$TAG] 센더 정보 없음: ${sender.name}")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = sendMessage(token, chatId, message)
                
                if (result != null) {
                    AppLogger.log("[$TAG] 전송 성공: ${sender.name}, msgId=${result.messageId}")
                } else {
                    saveFailedMessage(context, sender.name, message)
                    scheduleRetryWorker(context, sender.name, message)
                    AppLogger.error("[$TAG] 전송 실패, 재시도 예약: ${sender.name}")
                }
            } catch (e: Exception) {
                saveFailedMessage(context, sender.name, message)
                scheduleRetryWorker(context, sender.name, message)
                AppLogger.error("[$TAG] 예외 발생: ${e.message}")
            }
        }
    }
    
    // 카드 거래 승인 전송 (DB 저장 포함)
    fun sendCardTransaction(
        context: Context, 
        sender: Sender, 
        message: String,
        cardNumber: String,
        amount: String,
        datetime: String,
        storeName: String
    ) {
        val token = sender.token
        val chatId = sender.chatId
        
        if (token.isNullOrEmpty() || chatId.isNullOrEmpty()) {
            AppLogger.error("[$TAG] 센더 정보 없음: ${sender.name}")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = sendMessage(token, chatId, message)
                
                if (result != null) {
                    // DB에 거래 저장
                    saveCardTransaction(
                        context, cardNumber, amount, datetime, storeName,
                        result.messageId, result.chatId
                    )
                    AppLogger.log("[$TAG] 카드거래 저장 완료: $cardNumber $amount $storeName")
                } else {
                    AppLogger.error("[$TAG] 카드거래 전송 실패, 재시도 예약")
                    saveFailedMessage(context, sender.name, message)
                    scheduleRetryWorker(context, sender.name, message)
                }
            } catch (e: Exception) {
                AppLogger.error("[$TAG] 카드거래 예외 발생: ${e.message}, 재시도 예약")
                saveFailedMessage(context, sender.name, message)
                scheduleRetryWorker(context, sender.name, message)
            }
        }
    }
    
    // 카드 취소 처리
    fun handleCardCancellation(
        context: Context,
        sender: Sender,
        cardNumber: String,
        amount: String,
        cancelDatetime: String,
        storeName: String
    ) {
        val token = sender.token
        val chatId = sender.chatId
        
        if (token.isNullOrEmpty() || chatId.isNullOrEmpty()) {
            AppLogger.error("[$TAG] 센더 정보 없음: ${sender.name}")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                
                // 가장 최근의 매칭되는 거래 찾기
                val originalTransaction = db.cardTransactionDao()
                    .findMatchingTransaction(cardNumber, amount, storeName)
                
                if (originalTransaction != null) {
                    // 원본 메시지에 취소선 추가
                    val cancelledMessage = applyCancellationStrike(
                        originalTransaction.cardNumber,
                        originalTransaction.amount,
                        originalTransaction.datetime,
                        originalTransaction.storeName,
                        cancelDatetime
                    )
                    
                    val success = editMessage(
                        token,
                        originalTransaction.chatId,
                        originalTransaction.messageId,
                        cancelledMessage
                    )
                    
                    if (success) {
                        db.cardTransactionDao().markAsCancelled(originalTransaction.id)
                        AppLogger.log("[$TAG] 카드취소 - 원본 메시지 수정 완료: $storeName (msgId=${originalTransaction.messageId})")
                    } else {
                        AppLogger.error("[$TAG] 카드취소 - 메시지 수정 실패, 새 메시지로 전송")
                        sendCancellationAsNewMessage(context, sender, cardNumber, amount, cancelDatetime, storeName)
                    }
                } else {
                    AppLogger.log("[$TAG] 카드취소 - 원본 없음, 새 메시지 전송: $storeName")
                    sendCancellationAsNewMessage(context, sender, cardNumber, amount, cancelDatetime, storeName)
                }
            } catch (e: Exception) {
                AppLogger.error("[$TAG] 카드취소 - 예외 발생: ${e.message}, 새 메시지로 전송")
                sendCancellationAsNewMessage(context, sender, cardNumber, amount, cancelDatetime, storeName)
            }
        }
    }

    // 취소 메시지를 새로 전송하는 헬퍼 함수
    private fun sendCancellationAsNewMessage(
        context: Context,
        sender: Sender,
        cardNumber: String,
        amount: String,
        cancelDatetime: String,
        storeName: String
    ) {
        val cancellationMessage = if (cardNumber == "7293") {
            "ㅤㅤㅤㅤ[$cardNumber] 취소\nㅤㅤㅤㅤ$amount\nㅤㅤㅤㅤ$cancelDatetime\nㅤㅤㅤㅤ$storeName"
        } else {
            "[$cardNumber] 취소\n$amount\n$cancelDatetime\n$storeName"
        }
        sendTelegram(context, sender, cancellationMessage)
    }

    // 메시지 전송 결과 (messageId 포함)
    data class SendResult(val messageId: Long, val chatId: String)

    private suspend fun sendMessage(token: String, chatId: String, message: String): SendResult? {
        return try {
            val apiUrl = "https://api.telegram.org/bot$token/sendMessage"
            val params = "chat_id=$chatId&text=${java.net.URLEncoder.encode(message, "UTF-8")}"

            val url = URL(apiUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            
            conn.outputStream.use { it.write(params.toByteArray()) }
            
            val responseCode = conn.responseCode
            val response = if (responseCode == 200) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                val errorMsg = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown"
                AppLogger.error("[$TAG] HTTP $responseCode: $errorMsg")
                conn.disconnect()
                return null
            }
            
            conn.disconnect()
            
            // 응답에서 message_id 추출
            val json = JSONObject(response)
            val messageId = json.getJSONObject("result").getLong("message_id")
            
            SendResult(messageId, chatId)
        } catch (e: Exception) {
            AppLogger.error("[$TAG] 네트워크 오류: ${e.message}")
            null
        }
    }
    
    // 메시지 수정 (취소선 처리용)
    private suspend fun editMessage(
        token: String,
        chatId: String,
        messageId: Long,
        newText: String
    ): Boolean {
        return try {
            val apiUrl = "https://api.telegram.org/bot$token/editMessageText"
            val params = "chat_id=$chatId&message_id=$messageId&text=${java.net.URLEncoder.encode(newText, "UTF-8")}"

            val url = URL(apiUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            
            conn.outputStream.use { it.write(params.toByteArray()) }
            
            val responseCode = conn.responseCode
            if (responseCode != 200) {
                val errorMsg = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown"
                AppLogger.error("[$TAG] 메시지 수정 실패 $responseCode: $errorMsg")
            }
            conn.disconnect()
            
            responseCode == 200
        } catch (e: Exception) {
            AppLogger.error("[$TAG] 메시지 수정 예외: ${e.message}")
            false
        }
    }
    
    // 취소선 적용
    private fun applyCancellationStrike(
        cardNumber: String,
        amount: String,
        originalDatetime: String,
        storeName: String,
        cancelDatetime: String
    ): String {
        // 텔레그램 취소선: 유니코드 combining strikethrough (U+0336) 사용
        fun strikethrough(text: String): String {
            return text.map { "$it\u0336" }.joinToString("")
        }
        
        return if (cardNumber == "7293") {
            "ㅤㅤㅤㅤ[$cardNumber]\nㅤㅤㅤㅤ${strikethrough(amount)}\nㅤㅤㅤㅤ${strikethrough(originalDatetime)}\nㅤㅤㅤㅤ${strikethrough(storeName)}\n\n취소: $cancelDatetime"
        } else {
            "[$cardNumber]\n${strikethrough(amount)}\n${strikethrough(originalDatetime)}\n${strikethrough(storeName)}\n\n취소: $cancelDatetime"
        }
    }

    // 카드 거래 DB 저장
    private suspend fun saveCardTransaction(
        context: Context,
        cardNumber: String,
        amount: String,
        datetime: String,
        storeName: String,
        messageId: Long,
        chatId: String
    ) {
        try {
            val db = AppDatabase.getDatabase(context)
            val transaction = CardTransaction(
                cardNumber = cardNumber,
                amount = amount,
                datetime = datetime,
                storeName = storeName,
                messageId = messageId,
                chatId = chatId
            )
            db.cardTransactionDao().insert(transaction)
            
            // 30일 이상 오래된 거래 삭제
            val cutoffTime = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
            db.cardTransactionDao().deleteOldTransactions(cutoffTime)
        } catch (e: Exception) {
            AppLogger.error("[$TAG] DB 저장 실패: ${e.message}")
        }
    }

    private suspend fun saveFailedMessage(context: Context, senderName: String, message: String) {
        try {
            val db = AppDatabase.getDatabase(context)
            val failedMessage = FailedMessage(
                senderName = senderName,
                message = message
            )
            db.failedMessageDao().insert(failedMessage)
            AppLogger.log("[$TAG] 실패 메시지 DB 저장 완료")
        } catch (e: Exception) {
            AppLogger.error("[$TAG] 실패 메시지 DB 저장 실패: ${e.message}")
        }
    }

    private fun scheduleRetryWorker(context: Context, senderName: String, message: String) {
        val inputData = workDataOf(
            "senderName" to senderName,
            "message" to message
        )
        
        val retryRequest = OneTimeWorkRequestBuilder<TelegramRetryWorker>()
            .setInputData(inputData)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                10,
                TimeUnit.SECONDS
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        
        WorkManager.getInstance(context).enqueue(retryRequest)
        AppLogger.log("[$TAG] WorkManager 재시도 예약 (10초 backoff)")
    }
}
