package com.example.noti251022.model

data class Sender(
    val name: String,
    var token: String? = null,
    var chatId: String? = null
)