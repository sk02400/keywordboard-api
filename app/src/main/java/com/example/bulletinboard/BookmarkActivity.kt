package com.example.bulletinboard

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bulletinboard.adapter.BookmarkAdapter
import com.example.bulletinboard.databinding.ActivityBookmarkBinding
import com.example.bulletinboard.model.Bookmark
import com.example.bulletinboard.network.ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class BookmarkActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBookmarkBinding
    private lateinit var apiService: ApiService
    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookmarkBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // IntentからユーザーIDを取得
        userId = intent.getStringExtra("USER_ID") ?: ""

        if (userId.isEmpty()) {
            // ユーザーIDがなければログイン画面などへ遷移させる処理を入れると安全です
            finish()
            return
        }

        apiService = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:3000/") // エミュレーターからローカルアクセス
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)

        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        loadBookmarks()
    }

    private fun loadBookmarks() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val bookmarks = apiService.getBookmarks(userId)
                val boardIds = bookmarks.map { it.board_id }

                runOnUiThread {
                    binding.recyclerView.adapter = BookmarkAdapter(this@BookmarkActivity, boardIds) { boardId ->
                        val intent = Intent(this@BookmarkActivity, BoardActivity::class.java)
                        intent.putExtra("BOARD_ID", boardId)
                        startActivity(intent)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // エラーハンドリング（Toastなど）
            }
        }
    }
}
