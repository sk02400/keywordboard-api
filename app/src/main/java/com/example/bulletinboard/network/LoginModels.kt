package com.example.bulletinboard.network
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val id: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val token: String? = null,
    val message: String? = null,
    val success: Boolean
)

