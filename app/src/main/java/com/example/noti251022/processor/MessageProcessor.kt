package com.example.noti251022.processor

import android.content.Context
import com.example.noti251022.model.MessageData
import com.example.noti251022.sender.SenderList
import com.example.noti251022.sender.TelegramSender
import com.example.noti251022.util.AppLogger

object MessageProcessor {
    private const val TAG = "MsgProc"
    
    fun handleNotification(context: Context, msg: MessageData) {
        val rule = Rules.rulesMap[msg.source]
        if (rule == null) {
            return
        }

        if (!rule.condition(msg.title, msg.text)) {
            return
        }

        val messageToSend = rule.buildMessage(msg.title, msg.text)
        if (messageToSend.isNullOrEmpty()) {
            AppLogger.log("[$TAG] 무시 - 메시지 빌드 실패: ${msg.source}")
            return
        }

        val actualSenderName = when {
            messageToSend.startsWith("SMILEPAY|") -> "MJCard"
            messageToSend.startsWith("KAKAO_RESERVE|") -> "MGKH"
            else -> rule.sender
        }

        val sender = SenderList.getSender(actualSenderName)
        if (sender == null) {
            AppLogger.error("[$TAG] 에러 - 센더 없음: $actualSenderName")
            return
        }

        if (sender.token.isNullOrEmpty() || sender.chatId.isNullOrEmpty()) {
            AppLogger.error("[$TAG] 에러 - 센더 정보 불완전: ${sender.name}")
            return
        }

        if (msg.source == "com.shcard.smartpay" && 
            (messageToSend.startsWith("APPROVE|") || messageToSend.startsWith("CANCEL|"))) {
            handleCardTransaction(context, sender, messageToSend)
        }
        else if (messageToSend.startsWith("SMILEPAY|")) {
            val actualMessage = messageToSend.removePrefix("SMILEPAY|")
            TelegramSender.sendTelegram(context, sender, actualMessage)
            AppLogger.log("[$TAG] 스마일페이 전송: ${sender.name}")
        }
        else if (messageToSend.startsWith("KAKAO_RESERVE|")) {
            val actualMessage = messageToSend.removePrefix("KAKAO_RESERVE|")
            TelegramSender.sendTelegram(context, sender, actualMessage)
            AppLogger.log("[$TAG] 카카오예약 전송: ${sender.name}")
        }
        else {
            TelegramSender.sendTelegram(context, sender, messageToSend)
            AppLogger.log("[$TAG] 전송: ${sender.name}")
        }
    }
    
    private fun handleCardTransaction(
        context: Context, 
        sender: com.example.noti251022.model.Sender, 
        data: String
    ) {
        val parts = data.split("|")
        if (parts.size < 3) {
            AppLogger.error("[$TAG] 카드처리 - 잘못된 데이터 형식: $data")
            return
        }
        
        val type = parts[0]
        val cardNumber = parts[1]
        
        when (type) {
            "APPROVE" -> {
                if (parts.size < 6) {
                    AppLogger.error("[$TAG] 카드승인 - 데이터 부족: $data")
                    return
                }
                val message = parts[2]
                val amount = parts[3]
                val datetime = parts[4]
                val storeName = parts[5]
                
                TelegramSender.sendCardTransaction(
                    context, sender, message,
                    cardNumber, amount, datetime, storeName
                )
                AppLogger.log("[$TAG] 카드승인: $cardNumber $amount $storeName")
            }
            "CANCEL" -> {
                if (parts.size < 5) {
                    AppLogger.error("[$TAG] 카드취소 - 데이터 부족: $data")
                    return
                }
                val amount = parts[2]
                val datetime = parts[3]
                val storeName = parts[4]
                
                TelegramSender.handleCardCancellation(
                    context, sender,
                    cardNumber, amount, datetime, storeName
                )
                AppLogger.log("[$TAG] 카드취소: $cardNumber $amount $storeName")
            }
            else -> {
                AppLogger.error("[$TAG] 카드처리 - 알 수 없는 타입: $type")
            }
        }
    }
}
