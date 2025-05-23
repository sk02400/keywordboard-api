package com.example.bulletinboard.network
import kotlinx.serialization.Serializable

@Serializable
data class GoogleLoginRequest(
    val idToken: String,
    val email: String
)
