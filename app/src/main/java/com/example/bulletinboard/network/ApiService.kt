package com.example.bulletinboard.network

import com.example.bulletinboard.model.Post
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

class ApiService {
    private val serverUrl = "http://10.0.2.2:3000"

    // Jsonのカスタム設定を作成（ignoreUnknownKeys と coerceInputValuesを追加）
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    fun getPosts(boardId: String): List<Post> {
        val url = URL("$serverUrl/posts/$boardId")
        val connection = url.openConnection() as HttpURLConnection
        return connection.inputStream.bufferedReader().use {
            json.decodeFromString(it.readText())  // ここでカスタムjsonを使う
        }
    }

    fun createPost(boardId: String, post: Post) {
        val url = URL("$serverUrl/posts/$boardId")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
        val jsonString = json.encodeToString(post)  // ここもカスタムjsonを使う
        connection.outputStream.write(jsonString.toByteArray())
        connection.inputStream.close()
    }
}
