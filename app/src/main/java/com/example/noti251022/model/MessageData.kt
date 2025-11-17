package com.example.noti251022.model

data class MessageData(
    val source: String,   // 패키지명 또는 발신번호
    val title: String,    // 알림 제목 혹은 발신번호
    val text: String      // 본문 내용
)