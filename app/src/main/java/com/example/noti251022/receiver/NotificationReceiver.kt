package com.example.noti251022.receiver

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotificationReceiver : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        // 알림이 올라왔을 때 처리 (추후 구현)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        // 알림이 제거되었을 때 처리 (추후 구현)
    }
}
