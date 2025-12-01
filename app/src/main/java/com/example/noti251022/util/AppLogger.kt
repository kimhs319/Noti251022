package com.example.noti251022.util

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

object AppLogger {
    private const val TAG = "Noti251022"
    private const val MAX_LOGS = 100
    
    // 메모리에 로그 저장
    private val logHistory = mutableListOf<LogEntry>()
    private val listeners = mutableSetOf<LogListener>()
    
    data class LogEntry(
        val timestamp: String,
        val level: String,
        val message: String
    )
    
    interface LogListener {
        fun onNewLog(entry: LogEntry)
    }
    
    fun log(message: String) {
        Log.d(TAG, message)
        addLog("d", message)
    }

    fun error(message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(TAG, message, throwable)
            addLog("ERROR", "$message: ${throwable.message}")
        } else {
            Log.e(TAG, message)
            addLog("ERROR", message)
        }
    }
    
    private fun addLog(level: String, message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val entry = LogEntry(timestamp, level, message)
        
        synchronized(logHistory) {
            logHistory.add(entry)
            if (logHistory.size > MAX_LOGS) {
                logHistory.removeAt(0)
            }
        }
        
        // 리스너들에게 알림
        listeners.forEach { it.onNewLog(entry) }
    }
    
    fun registerListener(listener: LogListener) {
        listeners.add(listener)
    }
    
    fun unregisterListener(listener: LogListener) {
        listeners.remove(listener)
    }
    
    fun getAllLogs(): List<LogEntry> {
        return synchronized(logHistory) {
            logHistory.toList()
        }
    }
    
    fun clearLogs() {
        synchronized(logHistory) {
            logHistory.clear()
        }
    }
}
