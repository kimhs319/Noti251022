package com.example.noti251022.processor

import android.content.Context
import com.example.noti251022.model.MessageData
import com.example.noti251022.sender.SenderList
import com.example.noti251022.sender.TelegramSender
import com.example.noti251022.util.AppLogger

object MessageProcessor {
    fun handleNotification(context: Context, msg: MessageData) {
        //AppLogger.log("[수신] ${msg.source}: ${msg.title}")

        val rule = Rules.rulesMap[msg.source]
        if (rule == null) {
            //AppLogger.log("[무시] 룰 없음: ${msg.source}")
            return
        }

        if (!rule.condition(msg.title, msg.text)) {
            //AppLogger.log("[무시] 조건 불만족: ${msg.source}")
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

        // 신한카드 승인/취소 처리
        if (msg.source == "com.shcard.smartpay" && 
            (messageToSend.startsWith("APPROVE|") || messageToSend.startsWith("CANCEL|"))) {
            handleCardTransaction(context, sender, messageToSend)
        } else {
            // 일반 메시지 전송
            TelegramSender.sendTelegram(context, sender, messageToSend)
            AppLogger.log("[전송] ${sender.name}")
        }
    }
    
    private fun handleCardTransaction(
        context: Context, 
        sender: com.example.noti251022.model.Sender, 
        data: String
    ) {
        val parts = data.split("|")
        if (parts.size < 3) {
            AppLogger.error("[카드처리] 잘못된 데이터 형식: $data")
            return
        }
        
        val type = parts[0]
        val cardNumber = parts[1]
        
        when (type) {
            "APPROVE" -> {
                if (parts.size < 6) {
                    AppLogger.error("[카드승인] 데이터 부족: $data")
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
                AppLogger.log("[카드승인] $cardNumber $amount $storeName")
            }
            "CANCEL" -> {
                if (parts.size < 5) {
                    AppLogger.error("[카드취소] 데이터 부족: $data")
                    return
                }
                val amount = parts[2]
                val datetime = parts[3]
                val storeName = parts[4]
                
                TelegramSender.handleCardCancellation(
                    context, sender,
                    cardNumber, amount, datetime, storeName
                )
                AppLogger.log("[카드취소] $cardNumber $amount $storeName")
            }
            else -> {
                AppLogger.error("[카드처리] 알 수 없는 타입: $type")
            }
        }
    }
}
