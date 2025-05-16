package com.example.bulletinboard.network
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val id: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val success: Boolean,
    val token: String?,        // 必要に応じて変更
    val message: String?
)
