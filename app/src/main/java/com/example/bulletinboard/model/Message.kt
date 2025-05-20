package com.example.bulletinboard.model

data class Message(
    val room_id: Int,
    val sender_id: String,
    val sender_name: String,
    val content: String,
    val created_at: String
)

