package com.example.bulletinboard.model

import kotlinx.serialization.Serializable

@Serializable
data class BookmarkStatusResponse(
    val bookmarked: Boolean
)
