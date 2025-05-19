package com.example.bulletinboard.model

import kotlinx.serialization.Serializable

@Serializable
data class Post(
    val id: Int? = null,
    val user_id: String? = null,
    val post_name: String? = null,
    val content: String,
    val timestamp: String? = null
)
