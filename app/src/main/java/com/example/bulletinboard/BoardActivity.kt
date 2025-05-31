package com.example.bulletinboard

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
    private lateinit var pageTitle: String

    private var isLoading = false
    private var allLoaded = false
    private var offset = 0
    private val limit = 50
    private val posts = mutableListOf<Post>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBoardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Intent から取得
        userId = intent.getStringExtra("USER_ID") ?: "匿名"
        postName = intent.getStringExtra("POST_NAME") ?: "匿名"
        boardId = intent.getStringExtra("BOARD_ID") ?: "default"
        pageTitle = intent.getStringExtra("PAGE_TITLE") ?: "default"

        binding.editTextPost.hint = "$postName の投稿"

        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_arrow_back)
            title = ""
        }

        adapter = PostAdapter(posts)
        binding.postRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.postRecyclerView.adapter = adapter

        // スクロール監視で追加読み込み
        binding.postRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val firstVisibleItem = layoutManager.findFirstVisibleItemPosition()

                // 上にスクロールして最上部に達した場合に過去の投稿読み込み
                if (!isLoading && !allLoaded && firstVisibleItem <= 5) {
                    loadPosts()
                }
            }
        })


        // ヘッダーにタイトル表示
        binding.toolbar.title = pageTitle
        binding.toolbar.setOnClickListener {
            showBoardInfoDialog()
        }

        binding.buttonPost.setOnClickListener {
            val content = binding.editTextPost.text.toString()
            if (content.isBlank()) {
                Toast.makeText(this, "投稿内容を入力してください", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val created_at = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            val post = Post(user_id = userId, post_name = postName, content = content, created_at = created_at, board_id = boardId, post_number = 0)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = api.createPost(boardId, post)
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            binding.editTextPost.text?.clear()
                            // 投稿追加のためにリセットして再読み込み
                            offset = 0
                            allLoaded = false
                            posts.clear()
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

        CoroutineScope(Dispatchers.IO).launch {
            checkBookmarkStatus()
            withContext(Dispatchers.Main) {
                updateBookmarkIcon()
            }
        }

        loadPosts()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.board_menu, menu)
        updateBookmarkIcon()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
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
                // リフレッシュ時はoffsetリセットして全件再取得
                offset = 0
                allLoaded = false
                posts.clear()
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
        if (isLoading || allLoaded) return
        isLoading = true

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = api.getPosts(boardId, offset, limit)
                if (response.isSuccessful) {
                    val newPosts = response.body() ?: emptyList()
                    withContext(Dispatchers.Main) {
                        if (offset == 0) {
                            // 初回読み込み時（最新50件）
                            posts.clear()
                            posts.addAll(newPosts)
                            adapter.update(posts)
                            binding.postRecyclerView.scrollToPosition(posts.size - 1)
                        } else {
                            // 過去分追加（上に追加）
                            val oldSize = posts.size
                            posts.addAll(0, newPosts)
                            adapter.update(posts)

                            // 追加後のスクロール位置調整
                            binding.postRecyclerView.scrollToPosition(newPosts.size)
                        }

                        if (newPosts.size < limit) {
                            allLoaded = true
                        }
                        offset += newPosts.size
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    private fun showBoardInfoDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_board_info, null)
        val titleTextView = dialogView.findViewById<TextView>(R.id.titleTextView)
        val urlTextView = dialogView.findViewById<TextView>(R.id.urlTextView)

        val page_title = intent.getStringExtra("PAGE_TITLE") ?: "default"
        val is_link = intent.getStringExtra("IS_LINK").toBoolean()
        val board_name = intent.getStringExtra("BOARD_NAME") ?: "default"

        titleTextView.text = page_title

        if (is_link) {
            urlTextView.apply {
                text = board_name
                visibility = View.VISIBLE
                setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(board_name))
                    startActivity(intent)
                }
            }
        } else {
            urlTextView.visibility = View.GONE
        }

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("閉じる", null)
            .show()
    }
}
