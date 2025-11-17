package com.example.noti251022.processor

import com.example.noti251022.model.MessageData
import com.example.noti251022.sender.SenderList
import com.example.noti251022.sender.TelegramSender
import android.util.Log

object MessageProcessor {
    fun handleNotification(msg: MessageData) {
        Log.d("MessageProcessor", "수신 메시지: $msg")

        val rule = Rules.rulesMap[msg.source]

        if (rule == null) {
            Log.d("MessageProcessor", "적합한 룰이 없어 무시함: ${msg.source}")
            return
        }

        if (!rule.condition(msg.title, msg.text)) {
            Log.d("MessageProcessor", "룰 조건에 부합하지 않음: ${msg.source}")
            return
        }

        val messageToSend = rule.buildMessage(msg.title, msg.text)
        if (messageToSend.isNullOrEmpty()) {
            Log.d("MessageProcessor", "메시지 빌드 실패 또는 null: ${msg.source}")
            return
        }

        val sender = SenderList.getSender(rule.sender)
        if (sender == null) {
            Log.d("MessageProcessor", "설정된 센더를 찾을 수 없음: ${rule.sender}")
            return
        }

        TelegramSender.sendTelegram(sender, messageToSend)

        Log.d("MessageProcessor", "텔레그램 메시지 전송 완료: 센더=${sender.name}, 메시지=$messageToSend")
    }
}
