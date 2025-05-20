package com.example.bulletinboard.network

import android.os.Handler
import android.os.Looper
import okhttp3.*
import okio.ByteString
import com.example.bulletinboard.model.Message
import org.json.JSONObject

class WebSocketManager(
    private val userId: String,
    private val roomId: String,
    private val listener: Listener
) {

    interface Listener {
        fun onNewMessage(message: Message)
        fun onError(error: String)
    }

    private val client = OkHttpClient()

    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var webSocket: WebSocket

    fun connect() {
        val request = Request.Builder()
            .url("ws://10.0.2.2:3000")  // 開発環境に合わせてURL変更
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                // ルーム参加メッセージを送る
                val joinJson = JSONObject()
                joinJson.put("event", "joinRoom")
                joinJson.put("roomId", roomId)
                webSocket.send(joinJson.toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)

                    // サーバーからのメッセージイベント
                    if (json.has("room_id") && json.has("sender_id") && json.has("content") && json.has("created_at")) {
                        val msg = Message(
                            room_id = Integer.parseInt(json.getString("room_id")),
                            sender_id = json.getString("sender_id"),
                            sender_name = "", // サーバーから名前があればセット。なければ空
                            content = json.getString("content"),
                            created_at = json.getString("created_at")
                        )
                        mainHandler.post {
                            listener.onNewMessage(msg)
                        }
                    }
                } catch (e: Exception) {
                    mainHandler.post {
                        listener.onError("メッセージ解析失敗")
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                mainHandler.post {
                    listener.onError("WebSocket接続失敗: ${t.message}")
                }
            }
        })
    }

    fun sendMessage(content: String) {
        val json = JSONObject()
        json.put("event", "sendMessage")
        json.put("roomId", roomId)
        json.put("senderId", userId)
        json.put("content", content)
        webSocket.send(json.toString())
    }

    fun close() {
        webSocket.close(1000, "Close normally")
    }
}
