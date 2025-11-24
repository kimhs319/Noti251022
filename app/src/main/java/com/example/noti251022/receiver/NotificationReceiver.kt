package com.example.noti251022.receiver

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.app.Notification
import com.example.noti251022.model.MessageData
import com.example.noti251022.processor.MessageProcessor
import com.example.noti251022.sender.SenderList

class NotificationReceiver : NotificationListenerService() {

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

        val msg = MessageData(
            source = packageName,
            title = title,
            text = text
        )
        
        MessageProcessor.handleNotification(this, msg)
    }
}
