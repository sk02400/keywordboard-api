package com.example.bulletinboard

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bulletinboard.databinding.ActivityBoardBinding
import com.example.bulletinboard.model.Post
import com.example.bulletinboard.network.ApiClient
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class BoardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBoardBinding
    private val api = ApiClient.apiService
    private var isBookmarked = false
    private lateinit var adapter: PostAdapter
    private lateinit var userId: String
    private lateinit var postName: String
    private lateinit var boardId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBoardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userId = intent.getStringExtra("USER_ID") ?: "匿名"
        postName = intent.getStringExtra("POST_NAME") ?: "匿名"
        boardId = intent.getStringExtra("BOARD_ID") ?: "default"

        binding.editTextPost.hint = "$postName の投稿"
        // ツールバーセット＆戻るボタン表示
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_arrow_back) // 戻るアイコンを適宜用意してください
            title = ""
        }

        adapter = PostAdapter(emptyList())
        binding.postRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.postRecyclerView.adapter = adapter

        binding.buttonPost.setOnClickListener {
            val content = binding.editTextPost.text.toString()
            if (content.isBlank()) {
                Toast.makeText(this, "投稿内容を入力してください", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val created_at = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            val post = Post(user_id = userId , post_name = postName, content = content, created_at = created_at, board_id = boardId)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = api.createPost(boardId, post)
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            binding.editTextPost.text?.clear()
                            loadPosts()
                        } else {
                            val errorBodyString = response.errorBody()?.string()
                            Log.e("board", "エラーレスポンス body: $errorBodyString")
                            Toast.makeText(this@BoardActivity, "投稿に失敗しました", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@BoardActivity, "エラーが発生しました", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // ブックマーク状態チェック
        CoroutineScope(Dispatchers.IO).launch {
            checkBookmarkStatus()
            withContext(Dispatchers.Main) {
                updateBookmarkIcon()
            }
        }

        loadPosts()
    }

    // メニューをセット
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.board_menu, menu)
        updateBookmarkIcon()
        return true
    }

    // メニューアイテムクリック処理
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // 戻るボタン
                finish()
                true
            }
            R.id.action_bookmark -> {
                if (userId == "匿名") {
                    Toast.makeText(this, "ログインが必要です", Toast.LENGTH_SHORT).show()
                    return true
                }

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val response = if (!isBookmarked) {
                            api.bookmarkBoard(userId, boardId)
                        } else {
                            api.unbookmarkBoard(userId, boardId)
                        }

                        if (response.isSuccessful) {
                            isBookmarked = !isBookmarked
                            withContext(Dispatchers.Main) {
                                updateBookmarkIcon()
                                val message = if (isBookmarked) "ブックマークしました" else "ブックマークを解除しました"
                                Toast.makeText(this@BoardActivity, message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@BoardActivity, "通信エラーが発生しました", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                true
            }
            R.id.action_refresh -> {
                loadPosts()
                Toast.makeText(this, "更新しました", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateBookmarkIcon() {
        val icon = if (isBookmarked) R.drawable.ic_heart_filled else R.drawable.ic_heart_border
        binding.toolbar.menu.findItem(R.id.action_bookmark)?.setIcon(icon)
    }

    private suspend fun checkBookmarkStatus() {
        try {
            val response = api.getBookmarkStatus(userId, boardId)
            if (response.isSuccessful) {
                isBookmarked = response.body()?.bookmarked ?: false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadPosts() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = api.getPosts(boardId)
                if (response.isSuccessful) {
                    val posts = response.body() ?: emptyList()
                    withContext(Dispatchers.Main) {
                        adapter.update(posts)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
