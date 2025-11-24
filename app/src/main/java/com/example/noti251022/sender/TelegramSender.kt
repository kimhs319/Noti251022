package com.example.noti251022.sender

import android.content.Context
import androidx.work.*
import com.example.noti251022.model.Sender
import com.example.noti251022.storage.AppDatabase
import com.example.noti251022.storage.FailedMessage
import com.example.noti251022.util.AppLogger
import com.example.noti251022.worker.TelegramRetryWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

object TelegramSender {
    
    fun sendTelegram(context: Context, sender: Sender, message: String) {
        val token = sender.token
        val chatId = sender.chatId
        
        if (token.isNullOrEmpty() || chatId.isNullOrEmpty()) {
            AppLogger.error("[텔레그램] 센더 정보 없음: ${sender.name}")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val success = sendMessage(token, chatId, message)
                
                if (success) {
                    AppLogger.log("[텔레그램] 전송 성공: ${sender.name}")
                } else {
                    // 실패 시 DB에 저장하고 WorkManager로 재시도 예약
                    saveFailedMessage(context, sender.name, message)
                    scheduleRetryWorker(context, sender.name, message)
                    AppLogger.error("[텔레그램] 전송 실패, WorkManager 재시도 예약: ${sender.name}")
                }
            } catch (e: Exception) {
                saveFailedMessage(context, sender.name, message)
                scheduleRetryWorker(context, sender.name, message)
                AppLogger.error("[텔레그램] 예외 발생: ${e.message}")
            }
        }
    }

    private suspend fun sendMessage(token: String, chatId: String, message: String): Boolean {
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
            
            if (responseCode != 200) {
                val errorMsg = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown"
                AppLogger.error("[HTTP] $responseCode: $errorMsg")
            }
            
            conn.disconnect()
            
            responseCode == 200
        } catch (e: Exception) {
            AppLogger.error("[네트워크] ${e.message}")
            false
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
            AppLogger.log("[DB] 실패 메시지 저장 완료")
        } catch (e: Exception) {
            AppLogger.error("[DB] 저장 실패: ${e.message}")
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
                BackoffPolicy.LINEAR,  // 선형 증가 (10초 → 20초 → 30초...)
                10,                     // 10초부터 시작
                TimeUnit.SECONDS
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        
        WorkManager.getInstance(context).enqueue(retryRequest)
        AppLogger.log("[WorkManager] 재시도 작업 예약 (10초 backoff)")
    }
}
