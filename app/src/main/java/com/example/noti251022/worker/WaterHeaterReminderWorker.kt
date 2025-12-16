package com.example.noti251022.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.noti251022.sender.SenderList
import com.example.noti251022.sender.TelegramSender
import com.example.noti251022.util.AppLogger
import java.text.SimpleDateFormat
import java.util.*

class WaterHeaterReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    private val TAG = "WaterHeaterWorker"

    override suspend fun doWork(): Result {
        return try {
            // SharedPreferences에서 활성화 상태 확인
            val prefs = applicationContext.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
            val isEnabled = prefs.getBoolean("water_heater_enabled", true)
            
            if (!isEnabled) {
                AppLogger.log("[$TAG] 온수기 알림 비활성화됨")
                return Result.success()
            }
            
            AppLogger.log("[$TAG] 온수기 알림 전송 시작")
            
            val sender = SenderList.getSender("MGKH")
            if (sender == null) {
                AppLogger.error("[$TAG] MGKH 센더 없음")
                return Result.failure()
            }
            
            if (sender.token.isNullOrEmpty() || sender.chatId.isNullOrEmpty()) {
                AppLogger.error("[$TAG] MGKH 센더 정보 불완전")
                return Result.failure()
            }
            
            val message = "[온수기 전원 끄기]"
            
            TelegramSender.sendTelegram(applicationContext, sender, message)
            
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREAN)
            val now = dateFormat.format(Date())
            AppLogger.log("[$TAG] 전송 완료: $now")
            
            Result.success()
        } catch (e: Exception) {
            AppLogger.error("[$TAG] 에러: ${e.message}")
            Result.retry()
        }
    }
}