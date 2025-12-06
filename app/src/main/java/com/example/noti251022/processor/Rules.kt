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
