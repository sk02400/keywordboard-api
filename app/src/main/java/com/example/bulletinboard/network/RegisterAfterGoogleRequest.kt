package com.example.bulletinboard.network

data class RegisterAfterGoogleRequest(
    val email: String,
    val userId: String,
    val userName: String,
    val password: String
)
