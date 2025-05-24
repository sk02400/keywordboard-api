package com.example.bulletinboard.model

data class Bookmark(
    val board_id: String,
    val board_code: String,
    val page_title: String  // 追加：ページタイトル
)
