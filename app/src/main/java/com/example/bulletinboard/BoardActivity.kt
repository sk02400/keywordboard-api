package com.example.bulletinboard

import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.bulletinboard.model.Post
import com.example.bulletinboard.network.ApiClient
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class BoardActivity : AppCompatActivity() {
    private val api = ApiClient.apiService
    private var isBookmarked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_board)

        val userId = intent.getStringExtra("USER_ID") ?: "匿名"
        val boardId = intent.getStringExtra("BOARD_ID") ?: "default"

        val postInput = findViewById<EditText>(R.id.editTextPost)
        val postButton = findViewById<Button>(R.id.buttonPost)
        val bookmarkButton = findViewById<ImageButton>(R.id.buttonBookmark)
        val postList = findViewById<ListView>(R.id.postListView)

        val adapter = PostAdapter(this, mutableListOf())
        postList.adapter = adapter

        fun loadPosts() {
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

        fun updateBookmarkIcon() {
            bookmarkButton.setImageResource(
                if (isBookmarked) R.drawable.ic_heart_filled else R.drawable.ic_heart_border
            )
        }

        suspend fun checkBookmarkStatus() {
            try {
                val response = api.getBookmarkStatus(userId, boardId)
                if (response.isSuccessful) {
                    isBookmarked = response.body()?.bookmarked ?: false
                    Log.d("BookmarkCheck", "Bookmarked = $isBookmarked")
                } else {
                    Log.e("BookmarkCheck", "Failed: ${response.code()}, errorBody: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("BookmarkCheck", "Exception occurred", e)
            }
        }


        postButton.setOnClickListener {
            val content = postInput.text.toString()
            if (content.isBlank()) {
                Toast.makeText(this, "投稿内容を入力してください", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            val post = Post(name = userId, content = content, timestamp = timestamp)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = api.createPost(boardId, post)
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            postInput.text.clear()
                            loadPosts()
                        } else {
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

        bookmarkButton.setOnClickListener {
            if (userId == "匿名") {
                Toast.makeText(this, "ログインが必要です", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    if (!isBookmarked) {
                        val response = api.bookmarkBoard(userId, boardId)
                        if (response.isSuccessful) {
                            isBookmarked = true
                            withContext(Dispatchers.Main) {
                                updateBookmarkIcon()
                                Toast.makeText(this@BoardActivity, "ブックマークしました", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        val response = api.unbookmarkBoard(userId, boardId)
                        if (response.isSuccessful) {
                            isBookmarked = false
                            withContext(Dispatchers.Main) {
                                updateBookmarkIcon()
                                Toast.makeText(this@BoardActivity, "ブックマークを解除しました", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@BoardActivity, "通信エラーが発生しました", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // 初期読み込み
        CoroutineScope(Dispatchers.IO).launch {
            checkBookmarkStatus()
            withContext(Dispatchers.Main) {
                updateBookmarkIcon()
            }
        }

        loadPosts()
    }
}
