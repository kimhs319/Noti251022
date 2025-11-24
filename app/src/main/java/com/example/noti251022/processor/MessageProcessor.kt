package com.example.noti251022.processor

import android.content.Context
import com.example.noti251022.model.MessageData
import com.example.noti251022.sender.SenderList
import com.example.noti251022.sender.TelegramSender
import com.example.noti251022.util.AppLogger

object MessageProcessor {
    fun handleNotification(context: Context, msg: MessageData) {
        AppLogger.log("[수신] ${msg.source}: ${msg.title}")

        val rule = Rules.rulesMap[msg.source]
        if (rule == null) {
            AppLogger.log("[무시] 룰 없음: ${msg.source}")
            return
        }

        if (!rule.condition(msg.title, msg.text)) {
            AppLogger.log("[무시] 조건 불만족: ${msg.source}")
            return
        }

        val messageToSend = rule.buildMessage(msg.title, msg.text)
        if (messageToSend.isNullOrEmpty()) {
            AppLogger.log("[무시] 메시지 빌드 실패: ${msg.source}")
            return
        }

        val sender = SenderList.getSender(rule.sender)
        if (sender == null) {
            AppLogger.error("[에러] 센더 없음: ${rule.sender}")
            return
        }

        if (sender.token.isNullOrEmpty() || sender.chatId.isNullOrEmpty()) {
            AppLogger.error("[에러] 센더 정보 불완전: ${sender.name}")
            return
        }

        TelegramSender.sendTelegram(context, sender, messageToSend)
        AppLogger.log("[전송] ${sender.name}")
    }
}
