package com.example.bulletinboard

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.example.bulletinboard.model.Post
import com.example.bulletinboard.network.ApiClient
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import android.widget.Toast

class BoardActivity : AppCompatActivity() {
    private val api = ApiClient.apiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_board)

        //val name = intent.getStringExtra("userName") ?: "匿名"
        val name = intent.getStringExtra("USER_ID") ?: "匿名"
        val boardId = intent.getStringExtra("BOARD_ID") ?: "default"

        val postInput = findViewById<EditText>(R.id.editTextPost)
        val postButton = findViewById<Button>(R.id.buttonPost)
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
                    } else {
                        // レスポンス失敗時の処理
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }


        postButton.setOnClickListener {
            val content = postInput.text.toString()
            if (content.isBlank()) {
                Toast.makeText(this, "投稿内容を入力してください", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            val post = Post(name = name, content = content, timestamp = timestamp)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = api.createPost(boardId, post)
                    if (response.isSuccessful) {
                        withContext(Dispatchers.Main) {
                            postInput.text.clear()
                            loadPosts()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
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


        loadPosts()
    }
}
