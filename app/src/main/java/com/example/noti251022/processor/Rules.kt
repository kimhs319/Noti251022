package com.example.noti251022.processor

import com.example.noti251022.model.Rule
import com.example.noti251022.util.AppLogger

object Rules {
    private const val TAG = "Rules"

    val rulesMap: Map<String, Rule> = mapOf(

        // 네이버 스마트플레이스 예약
        "com.naver.smartplace" to Rule(
            source = "com.naver.smartplace",
            condition = { _, _ -> true },
            sender = "MGKH",
            buildMessage = buildMessage@{ _, text ->
                AppLogger.log("[$TAG-네이버예약] 입력확인")

                val dateTimeMatch = Regex("""\d{4}\.\d{2}\.\d{2}\.\([가-힣]\)\s+(오전|오후)\s+\d{1,2}:\d{2}""")
                val dateTime = dateTimeMatch.find(text)?.value
                if (dateTime == null) {
                    AppLogger.log("[$TAG-네이버예약] dateTime null")
                    return@buildMessage null
                }

                val nameMatch = """([\p{L}\p{M}\s\-]+)님""".toRegex().find(text)
                val name = nameMatch?.groups?.get(1)?.value ?: ""

                val status = when {
                    text.contains("확정") -> "[예약]"
                    text.contains("취소") -> "[취소]"
                    else -> {
                        AppLogger.log("[$TAG-네이버예약] 예약 관련 아님")
                        return@buildMessage null
                    }
                }
                
                "[네이버] $status $name $dateTime"
            }
        ),

        // 카카오 옐로아이디
        "com.kakao.yellowid" to Rule(
            source = "com.kakao.yellowid",
            condition = { title, _ -> title.startsWith("마곡경희한의원 - ") },
            sender = "MGKH",
            buildMessage = buildMessage@{ title, text ->
                "[카톡] ${title.removePrefix("마곡경희한의원 - ")}\n$text"
            }
        ),

        // 네이버 쇼핑 비즈톡
        "com.naver.shopping.biztalk" to Rule(
            source = "com.naver.shopping.biztalk",
            condition = { title, text ->
                title.startsWith("[마곡경희한의원 발산역점] ") &&
                !text.contains("톡톡 메시지가 왔습니다.")
            },
            sender = "MGKH",
            buildMessage = buildMessage@{ title, text ->
                "[네이버톡톡] ${title.removePrefix("[마곡경희한의원 발산역점] ")}\n$text"
            }
        ),

        // 모바일팩스
        "com.dho.mobilefax" to Rule(
            source = "com.dho.mobilefax",
            condition = { _, _ -> true },
            sender = "MGKH",
            buildMessage = buildMessage@{ _, text -> "[팩스] $text" }