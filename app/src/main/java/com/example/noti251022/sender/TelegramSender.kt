package com.example.noti251022.sender

import com.example.noti251022.model.Sender
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

object TelegramSender {
    // Sender 객체 기반으로 보내는 방식 (권장)
    fun sendTelegram(sender: Sender, message: String) {
        val token = sender.token ?: return
        val chatId = sender.chatId ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val apiUrl = "https://api.telegram.org/bot$token/sendMessage"
                val params = "chat_id=$chatId&text=${java.net.URLEncoder.encode(message, "UTF-8")}"

                val url = URL(apiUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.outputStream.use { it.write(params.toByteArray()) }
                conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
