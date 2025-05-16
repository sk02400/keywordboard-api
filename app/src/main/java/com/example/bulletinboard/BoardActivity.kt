package com.example.bulletinboard

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.example.bulletinboard.model.Post
import com.example.bulletinboard.network.ApiService
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class BoardActivity : AppCompatActivity() {
    private val api = ApiService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_board)

        val name = intent.getStringExtra("userName") ?: "匿名"
        val boardId = intent.getStringExtra("boardId") ?: "default"

        val postInput = findViewById<EditText>(R.id.editTextPost)
        val postButton = findViewById<Button>(R.id.buttonPost)
        val postList = findViewById<ListView>(R.id.postListView)

        val adapter = PostAdapter(this, mutableListOf())
        postList.adapter = adapter

        fun loadPosts() {
            CoroutineScope(Dispatchers.IO).launch {
                val posts = api.getPosts(boardId)
                withContext(Dispatchers.Main) {
                    adapter.update(posts)
                }
            }
        }

        postButton.setOnClickListener {
            val content = postInput.text.toString()
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            val post = Post(userName = name, content = content, createdAt = timestamp)

            CoroutineScope(Dispatchers.IO).launch {
                api.createPost(boardId, post)
                loadPosts()
            }

            postInput.text.clear()
        }

        loadPosts()
    }
}
