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
        ),

        // 드롭박스 업로드 완료
        "com.dropbox.android" to Rule(
            source = "com.dropbox.android",
            condition = { title, _ -> title.contains("업로드 완료") },
            sender = "MGKH",
            buildMessage = buildMessage@{ _, _ -> "[팩스] 저장 완료" }
        ),

        // Gmail - 서류 발급 요청
        "com.google.android.gm" to Rule(
            source = "com.google.android.gm",
            condition = { _, text -> text.contains("Contact form submitted") },
            sender = "MGKH",
            buildMessage = buildMessage@{ _, _ -> "[서류 발급 요청]" }
        ),

        // 제로페이
        "kr.or.zeropay.zip" to Rule(
            source = "kr.or.zeropay.zip",
            condition = { title, _ -> title.contains("결제완료") },
            sender = "MGKH",
            buildMessage = buildMessage@{ title, _ ->
                val paymentMatch = Regex("""\d{1,3}(?:,\d{3})*원""").find(title)
                if (paymentMatch == null) {
                    AppLogger.log("[$TAG-제로페이] 금액 파싱 실패")
                    return@buildMessage null
                }
                "[제로페이] ${paymentMatch.value}"
            }
        ),

        // 네이버 회원정보 인증
        "com.nhn.android.search" to Rule(
            source = "com.nhn.android.search",
            condition = { title, _ -> title.contains("네이버 회원정보") },
            sender = "MGKH",
            buildMessage = buildMessage@{ _, _ -> "[네이버 인증]" }
        ),

        // 우리은행 입금
        "com.wooribank.smart.npib" to Rule(
            source = "com.wooribank.smart.npib",
            condition = { _, text -> text.contains("입금") },
            sender = "MGKH",
            buildMessage = buildMessage@{ _, text ->
                val match = Regex("""^\[입금\]\s*([가-힣a-zA-Z\s]+?)\s+(\d{1,3}(?:,\d{3})*원)""")
                    .find(text)
                if (match == null) {
                    AppLogger.log("[$TAG-우리은행] 입금 정보 파싱 실패")
                    return@buildMessage null
                }
                "[입금] ${match.groupValues[1].trim()} ${match.groupValues[2].trim()}"
            }
        ),

        // 카카오톡 예약하기
        "com.kakao.talk" to Rule(
            source = "com.kakao.talk",
            condition = { title, text -> 
                title.contains("카카오톡 예약하기 파트너센터") && text.contains("예약자명") 
            },
            sender = "MGKH",
            buildMessage = buildMessage@{ _, text ->
                val dateTimeMatch = Regex("""\d{4}\.\d{2}\.\d{2}\(.+\) \d{2}:\d{2}""")
                    .find(text)
                if (dateTimeMatch == null) {
                    AppLogger.log("[$TAG-카카오예약] 날짜/시간 파싱 실패")
                    return@buildMessage null
                }
                
                val nameMatch = Regex("""예약자명\s*:\s*([^\n\r]+)""")
                    .find(text)
                if (nameMatch == null) {
                    AppLogger.log("[$TAG-카카오예약] 예약자명 파싱 실패")
                    return@buildMessage null
                }
                
                "[카카오 예약 또는 취소] ${dateTimeMatch.value} ${nameMatch.groupValues[1].trim()}"
            }
        ),

        // 신한카드 (6585, 7293 승인 및 취소 통합 처리)
        "com.shcard.smartpay" to Rule(
            source = "com.shcard.smartpay",
            condition = { _, text -> 
                (text.contains("6585") || text.contains("7293")) &&
                (text.contains("승인]") || text.contains("승인 취소]"))
            },
            sender = "MJCard",
            buildMessage = buildMessage@{ _, text ->
                // 카드 번호 확인
                val cardNumber = when {
                    text.contains("6585") -> "6585"
                    text.contains("7293") -> "7293"
                    else -> {
                        AppLogger.log("[$TAG-신한카드] 카드번호 찾을 수 없음")
                        return@buildMessage null
                    }
                }

                // 승인인지 취소인지 확인 (text에서 확인)
                val isCancellation = text.contains("승인 취소]")

                // 금액 추출 (승인금액 또는 취소금액)
                val amountPattern = if (isCancellation) {
                    Regex("""취소금액:\s*([\d,]+원)""")
                } else {
                    Regex("""승인금액:\s*([\d,]+원)""")
                }
                val amountMatch = amountPattern.find(text)
                if (amountMatch == null) {
                    AppLogger.log("[$TAG-신한카드] $cardNumber 금액 파싱 실패")
                    return@buildMessage null
                }
                val amount = amountMatch.groupValues[1]

                // 일시 추출 (승인일시 또는 취소일시)
                val datetimePattern = if (isCancellation) {
                    Regex("""취소일시:\s*([\d/ ]+:\d{2})""")
                } else {
                    Regex("""승인일시:\s*([\d/ ]+:\d{2})""")
                }
                val dateTimeMatch = datetimePattern.find(text)
                if (dateTimeMatch == null) {
                    AppLogger.log("[$TAG-신한카드] $cardNumber 일시 파싱 실패")
                    return@buildMessage null
                }
                val datetime = dateTimeMatch.groupValues[1]

                // 가맹점명 추출
                val storeMatch = Regex("""가맹점명:\s*([^\[]+)""").find(text)
                if (storeMatch == null) {
                    AppLogger.log("[$TAG-신한카드] $cardNumber 가맹점명 파싱 실패")
                    return@buildMessage null
                }
                val storeName = storeMatch.groupValues[1].trim()

                // 승인과 취소를 구분해서 데이터 반환
                if (isCancellation) {
                    // 취소: CANCEL|카드번호|금액|일시|가맹점
                    "CANCEL|$cardNumber|$amount|$datetime|$storeName"
                } else {
                    // 승인: APPROVE|카드번호|메시지|금액|일시|가맹점
                    // 7293 카드는 들여쓰기 추가
                    if (cardNumber == "7293") {
                        "APPROVE|$cardNumber|ㅤㅤㅤㅤ[$cardNumber]\nㅤㅤㅤㅤ$amount\nㅤㅤㅤㅤ$datetime\nㅤㅤㅤㅤ$storeName|$amount|$datetime|$storeName"
                    } else {
                        "APPROVE|$cardNumber|[$cardNumber]\n$amount\n$datetime\n$storeName|$amount|$datetime|$storeName"
                    }
                }
            }
        ),

        // 스마일페이
        "com.mysmilepay.app" to Rule(
            source = "com.mysmilepay.app",
            condition = { _, _ ->
                // 일단 모든 알림을 통과시켜서 로그 확인
                true
            },
            sender = "MJCard",
            buildMessage = buildMessage@{ title, text ->
                AppLogger.log("[$TAG-스마일페이] 알림 수신")
                AppLogger.log("[$TAG-스마일페이] title: $title")
                AppLogger.log("[$TAG-스마일페이] text 앞부분: ${text.take(200)}")

                // 조건 하나씩 확인
                val hasTitle = title.contains("스마일페이")
                val hasComplete = text.contains("결제가 완료")
                val hasCard = text.contains("신한카드")

                AppLogger.log("[$TAG-스마일페이] title에 '스마일페이' 포함: $hasTitle")
                AppLogger.log("[$TAG-스마일페이] text에 '결제가 완료' 포함: $hasComplete")
                AppLogger.log("[$TAG-스마일페이] text에 '신한카드' 포함: $hasCard")

                // 조건 불만족 시 null 반환
                if (!hasTitle || !hasComplete || !hasCard) {
                    AppLogger.log("[$TAG-스마일페이] 조건 불만족으로 무시")
                    return@buildMessage null
                }

                // 상점명 추출
                val storeMatch = Regex("""▶\s*상점\s*:\s*([^\n▶]+)""").find(text)
                if (storeMatch == null) {
                    AppLogger.log("[$TAG-스마일페이] 상점명 파싱 실패")
                    return@buildMessage null
                }
                val storeName = storeMatch.groupValues[1].trim()

                // 결제금액 추출
                val amountMatch = Regex("""▶\s*결제금액\s*:\s*(\d{1,3}(?:,\d{3})*원)""").find(text)
                if (amountMatch == null) {
                    AppLogger.log("[$TAG-스마일페이] 결제금액 파싱 실패")
                    return@buildMessage null
                }
                val amount = amountMatch.groupValues[1]

                "[스마일페이] $storeName $amount"
            }
        ),

        // Samsung 메시지 - 카카오톡 인증
        "com.samsung.android.messaging" to Rule(
            source = "com.samsung.android.messaging",
            condition = { title, text -> title.contains("1644-4174") && text.contains("인증번호") },
            sender = "MGKH",
            buildMessage = buildMessage@{ _, text ->
                val regex = """\b\d{6}\b""".toRegex()
                val code = regex.find(text)?.value
                if (code == null) {
                    AppLogger.log("[$TAG-카톡인증] 인증번호 파싱 실패")
                    return@buildMessage null
                }
                "[카카오톡인증] $code"
            }
        )
    )

    /**
     * 특정 소스의 Rule 조회
     */
    fun getRuleForSource(source: String): Rule? = rulesMap[source]

    /**
     * 모든 등록된 소스 목록 반환
     */
    fun getAllSources(): Set<String> = rulesMap.keys
}
