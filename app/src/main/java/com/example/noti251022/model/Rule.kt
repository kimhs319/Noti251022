package com.example.noti251022.model

data class Rule(
    val source: String,
    val condition: (title: String, text: String) -> Boolean,
    val sender: String,
    val buildMessage: (title: String, text: String) -> String?
)
