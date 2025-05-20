package com.example.bulletinboard.model

import com.google.gson.annotations.SerializedName

data class ChatRoom(
    @SerializedName("room_id")    val room_id: String,
    @SerializedName("partner_id") val partner_id: String,
    @SerializedName("partner_name") val partner_name: String,
    @SerializedName("last_message") val last_message: String?
)
