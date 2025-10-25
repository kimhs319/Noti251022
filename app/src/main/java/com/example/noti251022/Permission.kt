package com.example.noti251022

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionHelper {
    fun requestNotificationListener(context: Context) {
        val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
        context.startActivity(intent)
    }

    fun requestSMSPermission(activity: Activity) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECEIVE_SMS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.RECEIVE_SMS),
                1001
            )
        }
    }
}
