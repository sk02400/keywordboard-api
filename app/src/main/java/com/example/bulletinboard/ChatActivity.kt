package com.example.bulletinboard

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bulletinboard.adapter.ChatAdapter
import com.example.bulletinboard.databinding.ActivityChatBinding
import com.example.bulletinboard.model.Message
import com.example.bulletinboard.network.ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.gson.Gson

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var api: ApiService
    private lateinit var roomId: String
    private lateinit var userId: String
    private lateinit var userName: String
    private lateinit var partnerId: String
    private val messages = mutableListOf<Message>()
    private lateinit var adapter: ChatAdapter
    private lateinit var webSocket: WebSocket
    private val gson = Gson()
    private val client = OkHttpClient() // 🔄 shutdownの位置修正に合わせてここで保持

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        roomId = intent.getStringExtra("ROOM_ID") ?: ""
        userId = intent.getStringExtra("USER_ID") ?: ""
        userName = intent.getStringExtra("USER_NAME") ?: ""
        partnerId = intent.getStringExtra("PARTNER_ID") ?: ""

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "チャット - $partnerId"

        api = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:3000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)

        adapter = ChatAdapter(messages, userId)
        binding.messageRecycler.layoutManager = LinearLayoutManager(this)
        binding.messageRecycler.adapter = adapter

        binding.sendButton.setOnClickListener {
            val text = binding.messageInput.text.toString()
            if (text.isNotBlank()) {
                sendMessage(text)
                binding.messageInput.setText("")
            }
        }

        loadMessages()
        connectWebSocket()
    }

    private fun loadMessages() = CoroutineScope(Dispatchers.IO).launch {
        runCatching { api.getMessages(roomId) }
            .onSuccess {
                messages.clear()
                messages.addAll(it)
                runOnUiThread {
                    adapter.notifyDataSetChanged()
                    binding.messageRecycler.scrollToPosition(messages.size - 1)
                }
            }
            .onFailure {
                runOnUiThread { toast("メッセージ取得に失敗しました") }
            }
    }

    private fun connectWebSocket() {
        val request = Request.Builder()
            .url("ws://10.0.2.2:3000/ws/$roomId")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                // 必要ならログ出力
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val msg = gson.fromJson(text, Message::class.java)
                runOnUiThread {
                    messages.add(msg)
                    adapter.notifyItemInserted(messages.size - 1)
                    binding.messageRecycler.scrollToPosition(messages.size - 1)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                runOnUiThread {
                    toast("WebSocketエラー: ${t.localizedMessage}")
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                runOnUiThread {
                    toast("WebSocket切断: $reason")
                }
            }
        })
    }

    private fun sendMessage(text: String) {
        val roomIdInt = roomId.toIntOrNull() ?: return
        val message = Message(
            room_id = roomIdInt,
            sender_id = userId,
            sender_name = userName,
            content = text,
            created_at = "" // サーバーでセットされる
        )
        webSocket.send(gson.toJson(message))
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocket.close(1000, "Activity終了")
        client.dispatcher.executorService.shutdown() // 🔄 ここに移動
    }

    private fun toast(msg: String) =
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show()
}
