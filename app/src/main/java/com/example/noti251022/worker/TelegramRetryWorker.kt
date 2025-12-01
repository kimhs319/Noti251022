package com.example.noti251022.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.noti251022.sender.SenderList
import com.example.noti251022.storage.AppDatabase
import com.example.noti251022.util.AppLogger
import java.net.HttpURLConnection
import java.net.URL

class TelegramRetryWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val TAG = "RetryWorker"

    override suspend fun doWork(): Result {
        val senderName = inputData.getString("senderName") ?: return Result.failure()
        val message = inputData.getString("message") ?: return Result.failure()
        
        AppLogger.log("[$TAG] [재시도] 시작 (${runAttemptCount + 1}회): $senderName")
        
        return try {
            val sender = SenderList.getSender(senderName)
            if (sender?.token == null || sender.chatId == null) {
                AppLogger.error("[$TAG] [재시도] 센더 정보 없음: $senderName")
                return Result.failure()
            }
            
            val success = sendMessage(sender.token!!, sender.chatId!!, message)
            
            if (success) {
                // 성공 시 DB에서 삭제
                deleteFromDatabase(senderName, message)
                AppLogger.log("[$TAG] [재시도성공] $senderName (${runAttemptCount + 1}회 시도)")
                Result.success()
            } else {
                // 실패 시 재시도 (최대 10회)
                if (runAttemptCount < 9) {
                    AppLogger.error("[$TAG] [재시도실패] $senderName (${runAttemptCount + 1}/10)")
                    Result.retry()  // BackoffPolicy에 따라 재시도
                } else {
                    // 10회 실패 시 포기
                    deleteFromDatabase(senderName, message)
                    AppLogger.error("[$TAG] [포기] $senderName - 10회 실패")
                    Result.failure()
                }
            }
        } catch (e: Exception) {
            AppLogger.error("[$TAG] [재시도에러] ${e.message}")
            if (runAttemptCount < 9) {
                Result.retry()
            } else {
                deleteFromDatabase(senderName, message)
                Result.failure()
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
            conn.disconnect()
            
            responseCode == 200
        } catch (e: Exception) {
            AppLogger.error("[$TAG] [Worker네트워크] ${e.message}")
            false
        }
    }

    private suspend fun deleteFromDatabase(senderName: String, message: String) {
        try {
            val db = AppDatabase.getDatabase(applicationContext)
            // 가장 오래된 메시지 삭제 (같은 내용일 것으로 가정)
            val oldestMessage = db.failedMessageDao().getOldestMessage()
            if (oldestMessage != null && oldestMessage.senderName == senderName) {
                db.failedMessageDao().deleteById(oldestMessage.id)
                AppLogger.log("[$TAG] [DB] 처리 완료 메시지 삭제")
            }
        } catch (e: Exception) {
            AppLogger.error("[$TAG] [DB] 삭제 실패: ${e.message}")
        }
    }
}
