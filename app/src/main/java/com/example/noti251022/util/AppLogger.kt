package com.example.noti251022.util

import android.util.Log

object AppLogger {
    private const val TAG = "Noti251022"

    fun log(message: String) {
        Log.d(TAG, message)
    }

    fun error(message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(TAG, message, throwable)
        } else {
            Log.e(TAG, message)
        }
    }
}
