package com.example.bulletinboard.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Post(
    val id: Int? = null,
    //@SerialName("name")
    val name: String? = null,     // nullable にする＆デフォルト値を null に
    val content: String,
    //@SerialName("timestamp")
    val timestamp: String? = null      // nullable にする＆デフォルト値を null に
)
