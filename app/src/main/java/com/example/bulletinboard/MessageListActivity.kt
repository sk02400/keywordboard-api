package com.example.bulletinboard

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bulletinboard.adapter.ChatRoomAdapter
import com.example.bulletinboard.databinding.ActivityMessageListBinding
import com.example.bulletinboard.model.ChatRoom
import com.example.bulletinboard.network.ApiClient
import com.example.bulletinboard.network.ApiService
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MessageListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMessageListBinding
    private lateinit var api: ApiService
    private lateinit var userId: String
    private lateinit var userName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMessageListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userId   = intent.getStringExtra("USER_ID") ?: ""
        userName = intent.getStringExtra("POST_NAME") ?: ""

        api = Retrofit.Builder()
            .baseUrl(ApiClient.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)

        setSupportActionBar(binding.toolbar)
        binding.chatRecycler.layoutManager = LinearLayoutManager(this)

        loadChats()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_message_list, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add_chat -> {
                showAddChatDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadChats() = CoroutineScope(Dispatchers.IO).launch {
        runCatching { api.getChatRooms(userId) }
            .onSuccess { rooms ->
                runOnUiThread {
                    binding.chatRecycler.adapter =
                        ChatRoomAdapter(rooms) { room ->
                            val i = Intent(this@MessageListActivity, ChatActivity::class.java)
                            i.putExtra("ROOM_ID", room.room_id)
                            i.putExtra("PARTNER_ID", room.partner_id)
                            i.putExtra("USER_ID", userId)
                            startActivity(i)
                        }
                }
            }
    }

    private fun showAddChatDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_chat, null)
        val editText = dialogView.findViewById<TextInputEditText>(R.id.inputUserId)

        AlertDialog.Builder(this)
            .setTitle("ユーザーIDを入力")
            .setView(dialogView)
            .setPositiveButton("追加") { dlg, _ ->
                val targetId = editText.text.toString()
                if (targetId.isNotBlank()) addChat(targetId)
                dlg.dismiss()
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }


    private fun addChat(targetId: String) = CoroutineScope(Dispatchers.IO).launch {
        val exists = api.checkUserExists(targetId).exists
        if (!exists) {
            runOnUiThread { toast("ユーザーが存在しません") }
            return@launch
        }

        runCatching { api.createChat(ApiService.CreateChatRequest(userId, targetId)) }
            .onSuccess {
                runOnUiThread {
                    toast("チャットを開始しました")
                    // チャット一覧を更新
                    loadChats()
                    // チャット画面に遷移
                    val intent = Intent(this@MessageListActivity, ChatActivity::class.java)
                    intent.putExtra("ROOM_ID", it.room_id)
                    intent.putExtra("PARTNER_ID", targetId)
                    intent.putExtra("USER_ID", userId)
                    startActivity(intent)
                }
            }
            .onFailure {
                runOnUiThread { toast("作成に失敗しました") }
            }
    }

    private fun toast(msg: String) =
        runOnUiThread { android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show() }
}
