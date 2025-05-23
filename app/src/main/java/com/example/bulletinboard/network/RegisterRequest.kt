package com.example.bulletinboard.network
import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val email: String,
    val userId: String,
    val userName: String,
    val password: String
)
