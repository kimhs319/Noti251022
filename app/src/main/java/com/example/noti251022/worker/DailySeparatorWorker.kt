package com.example.noti251022.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.noti251022.sender.SenderList
import com.example.noti251022.sender.TelegramSender
import com.example.noti251022.util.AppLogger
import java.text.SimpleDateFormat
import java.util.*

class DailySeparatorWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    private val TAG = "DailyWorker"

    override suspend fun doWork(): Result {
        return try {
            AppLogger.log("[$TAG] 일일구분선 전송 시작")
            
            val sender = SenderList.getSender("MJCard")
            if (sender == null) {
                AppLogger.error("[$TAG] MJCard 센더 없음")
                return Result.failure()
            }
            
            if (sender.token.isNullOrEmpty() || sender.chatId.isNullOrEmpty()) {
                AppLogger.error("[$TAG] MJCard 센더 정보 불완전")
                return Result.failure()
            }
            
            // 날짜 정보 추가
            val dateFormat = SimpleDateFormat("yyyy-MM-dd (E)", Locale.KOREAN)
            val today = dateFormat.format(Date())
            val message = "============================="
            //val message = "==========================\n$today"
            
            TelegramSender.sendTelegram(applicationContext, sender, message)
            AppLogger.log("[$TAG] 전송 완료: $today")
            
            Result.success()
        } catch (e: Exception) {
            AppLogger.error("[$TAG] 에러: ${e.message}")
            Result.retry()
        }
    }
}
