package com.example.bulletinboard.model

import kotlinx.serialization.Serializable

@Serializable
data class BoardIdResponse(val board_id: Long, val page_title: String, val board_name: String, val is_link: Boolean)
