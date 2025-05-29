package com.example.bulletinboard.model
import kotlinx.serialization.Serializable

@Serializable
data class BoardRanking(
    val board_id: String,
    val board_name: String,
    val page_title: String?,
    val is_link: Boolean,
    val favicon_url: String?,
    val posts: List<Post> = emptyList()
)
