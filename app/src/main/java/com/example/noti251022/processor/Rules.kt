package com.example.noti251022.processor

import com.example.noti251022.model.Rule
import com.example.noti251022.util.AppLogger

object Rules {

    val rulesMap: Map<String, Rule> = mapOf(

        "com.naver.smartplace" to Rule(
            source = "com.naver.smartplace",
            condition = { _, _ -> true },
            sender = "MGKH",
            buildMessage = { _, text ->
                AppLogger.log("[네이버예약] $text 입력확인")

                val dateTimeMatch = Regex("""\d{4}\.\d{2}\.\d{2}\.\([가-힣]\)\s+(오전|오후)\s+\d{1,2}:\d{2}""")
                val dateTime = dateTimeMatch.find(text)?.value ?: run {
                    AppLogger.log("네이버예약 dateTime null")
                    return@buildMessage null
                }

                val nameMatch = """([\p{L}\p{M}\s\-]+)님""".toRegex().find(text)
                val name = nameMatch?.groups?.get(1)?.value ?: ""

                val status = when {
                    text.contains("확정") -> "[예약]"
                    text.contains("취소") -> "[취소]"
                    else -> {
                        AppLogger.log("[네이버] 예약 관련 아님")
                        return@buildMessage null
                    }
                }
                "[네이버] $status $name $dateTime"
            }
        ),

        "com.shcard.smartpay" to Rule(
            source = "com.shcard.smartpay",
            condition = { _, text -> text.contains("6585") },
            sender = "MJCard",
            buildMessage = { _, text ->
                val amountMatch = Regex("""승인금액: ([\d,]+원)""").find(text) ?: return@buildMessage null
                val dateTimeMatch = Regex("""승인일시: ([\d/ ]+:\d{2})""").find(text) ?: return@buildMessage null
                val storeMatch = Regex("""가맹점명: ([^\[]+)""").find(text) ?: return@buildMessage null

                "[6585]\n${amountMatch.groupValues[1]}\n${dateTimeMatch.groupValues[1]}\n${storeMatch.groupValues[1].trim()}"
            }
        )
    )
}
