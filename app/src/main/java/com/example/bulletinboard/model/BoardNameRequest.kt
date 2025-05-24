package com.example.bulletinboard.model

import kotlinx.serialization.Serializable

@Serializable
data class BoardNameRequest(
    val board_code: String,
    val user_id: String?
)
