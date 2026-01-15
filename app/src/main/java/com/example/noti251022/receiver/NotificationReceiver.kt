package com.example.noti251022.receiver

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.app.Notification
import com.example.noti251022.model.MessageData
import com.example.noti251022.processor.MessageProcessor
import com.example.noti251022.sender.SenderList
import java.util.concurrent.ConcurrentHashMap

class NotificationReceiver : NotificationListenerService() {

    // 최근 처리한 알림 캐시 (중복 방지)
    private val recentNotifications = ConcurrentHashMap<String, Long>()
    
    // 캐시 유지 시간 (5분)
    private val CACHE_DURATION_MS = 5 * 60 * 1000L

    override fun onCreate() {
        super.onCreate()
        initializeSenders()
    }

    private fun initializeSenders() {
        try {
            SenderList.loadSenderNames(this)
            SenderList.loadSenderCredentials(this)
        } catch (e: Exception) {
            android.util.Log.e("NotificationReceiver", "센더 정보 로드 실패", e)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val packageName = sbn.packageName ?: return
        val notification = sbn.notification
        val extras = notification.extras

        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getString(Notification.EXTRA_TEXT) ?: ""

        // 중복 체크용 키 생성 (패키지명 + 제목 + 내용)
        val notificationKey = "$packageName|$title|$text"
        val currentTime = System.currentTimeMillis()

        // 캐시 정리 (5분 이상 지난 항목 삭제)
        cleanupCache(currentTime)

        // 중복 체크
        val lastProcessedTime = recentNotifications[notificationKey]
        if (lastProcessedTime != null && (currentTime - lastProcessedTime) < CACHE_DURATION_MS) {
            // 5분 이내에 동일한 알림을 이미 처리함 - 무시
            android.util.Log.d("NotificationReceiver", "중복 알림 무시: $packageName")
            return
        }

        // 새로운 알림 또는 5분 이상 지난 알림 - 처리
        recentNotifications[notificationKey] = currentTime

        val msg = MessageData(
            source = packageName,
            title = title,
            text = text
        )
        
        MessageProcessor.handleNotification(this, msg)
    }

    // 오래된 캐시 항목 정리
    private fun cleanupCache(currentTime: Long) {
        val iterator = recentNotifications.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (currentTime - entry.value > CACHE_DURATION_MS) {
                iterator.remove()
            }
        }
    }
}
