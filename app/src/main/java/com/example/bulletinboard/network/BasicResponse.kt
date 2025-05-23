package com.example.bulletinboard.network
import kotlinx.serialization.Serializable

@Serializable
data class BasicResponse(
    val success: Boolean,
    val message: String
)