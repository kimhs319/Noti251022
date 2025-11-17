package com.example.noti251022.receiver

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.app.Notification
import com.example.noti251022.model.MessageData
import com.example.noti251022.processor.MessageProcessor

class NotificationReceiver : NotificationListenerService() {

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
        MessageProcessor.handleNotification(msg)
    }
}
