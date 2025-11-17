package com.example.noti251022.sender

import android.content.Context
import com.example.noti251022.model.Sender  // model 패키지에서 가져옴
import com.example.noti251022.util.KeyStoreUtils
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

object SenderList {

    private val senders = mutableListOf<Sender>()

    /** 센더 이름 목록을 JSON 파일에서 로드 */
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

    /** KeyStore에서 센더의 토큰/챗아이디 불러와서 메모리 정보 세팅 */
    fun loadSenderCredentials(context: Context) {
        senders.forEach { sender ->
            sender.token = KeyStoreUtils.loadValue(context, "sender_${sender.name}_token")
            sender.chatId = KeyStoreUtils.loadValue(context, "sender_${sender.name}_chatid")
        }
    }

    /** 센더 정보 안전 저장 + 메모리 정보 동기화 */
    fun saveSenderCredentials(context: Context, senderName: String, token: String, chatId: String) {
        KeyStoreUtils.storeValue(context, "sender_${senderName}_token", token)
        KeyStoreUtils.storeValue(context, "sender_${senderName}_chatid", chatId)
        senders.find { it.name == senderName }?.apply {
            this.token = token
            this.chatId = chatId
        }
    }

    /** 모든 센더 반환 (불변 리스트로) */
    fun getAllSenders(): List<Sender> = senders.toList()

    /** 특정 센더 객체 반환 */
    fun getSender(name: String): Sender? = senders.find { it.name == name }

    /** 센더 토큰/챗아이디 모두 설정되어 있으면 true */
    fun isAllSendersConfigured(): Boolean =
        senders.all { !it.token.isNullOrEmpty() && !it.chatId.isNullOrEmpty() }

    /** 아직 미설정 센더 리스트 반환 */
    fun getUnconfiguredSenders(): List<Sender> =
        senders.filter { it.token.isNullOrEmpty() || it.chatId.isNullOrEmpty() }
}
