package com.example.bulletinboard.model

import kotlinx.serialization.Serializable

@Serializable
data class Post(
    val id: Int? = null,
    val board_id: String,
    val user_id: String? = null,
    val post_name: String,
    val content: String,
    val created_at: String? = null,
    val post_number: Int? = null
)
