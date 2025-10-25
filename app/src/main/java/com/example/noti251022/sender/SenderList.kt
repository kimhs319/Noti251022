package com.example.noti251022.sender

import android.content.Context
import com.example.noti251022.util.KeyStoreUtils
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

object SenderList {

    private val senders = mutableListOf<Sender>()

    // JSON에서 센더 이름 목록 로드
    fun loadSenderNames(context: Context) {
        senders.clear()

        val inputStream = context.resources.openRawResource(
            context.resources.getIdentifier("senders", "raw", context.packageName)
        )
        val reader = BufferedReader(InputStreamReader(inputStream))
        val jsonString = reader.use { it.readText() }

        val jsonObject = JSONObject(jsonString)
        val sendersArray = jsonObject.getJSONArray("senders")

        for (i in 0 until sendersArray.length()) {
            val senderObj = sendersArray.getJSONObject(i)
            val name = senderObj.getString("name")
            senders.add(Sender(name))
        }
    }

    // KeyStore에서 각 센더의 token과 chatId 로드
    fun loadSenderCredentials(context: Context) {
        senders.forEach { sender ->
            sender.token = KeyStoreUtils.loadValue(context, "sender_${sender.name}_token")
            sender.chatId = KeyStoreUtils.loadValue(context, "sender_${sender.name}_chatid")
        }
    }

    // 특정 센더의 token과 chatId 저장
    fun saveSenderCredentials(context: Context, senderName: String, token: String, chatId: String) {
        KeyStoreUtils.storeValue(context, "sender_${senderName}_token", token)
        KeyStoreUtils.storeValue(context, "sender_${senderName}_chatid", chatId)

        // 메모리 상의 센더 정보도 업데이트
        senders.find { it.name == senderName }?.apply {
            this.token = token
            this.chatId = chatId
        }
    }

    // 모든 센더 목록 반환
    fun getAllSenders(): List<Sender> = senders.toList()

    // 특정 이름의 센더 반환
    fun getSender(name: String): Sender? = senders.find { it.name == name }

    // 모든 센더가 설정되었는지 확인
    fun isAllSendersConfigured(): Boolean {
        return senders.all { !it.token.isNullOrEmpty() && !it.chatId.isNullOrEmpty() }
    }

    // 설정되지 않은 센더 목록 반환
    fun getUnconfiguredSenders(): List<Sender> {
        return senders.filter { it.token.isNullOrEmpty() || it.chatId.isNullOrEmpty() }
    }
}
