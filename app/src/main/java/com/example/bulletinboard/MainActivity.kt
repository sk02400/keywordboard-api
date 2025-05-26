package com.example.bulletinboard

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bulletinboard.adapter.RankingAdapter
import com.example.bulletinboard.databinding.ActivityMainBinding
import com.example.bulletinboard.model.BoardNameRequest
import com.example.bulletinboard.model.BoardRanking
import com.example.bulletinboard.network.ApiClient
import com.example.bulletinboard.network.ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var userSession: UserSession
    private lateinit var loginButton: Button
    private lateinit var headerLayout: View
    private lateinit var buttonGo: Button
    private lateinit var apiService: ApiService
    private lateinit var editTextBoardName: EditText
    private lateinit var clearIcon: Drawable

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        apiService = ApiClient.apiService
        userSession = UserSession(this)

        buttonGo = findViewById(R.id.buttonGo)
        loginButton = findViewById(R.id.buttonLogin)
        headerLayout = findViewById(R.id.headerLayout)
        val bookmarkButton = findViewById<ImageButton>(R.id.buttonBookmark)
        val messageButton = findViewById<ImageButton>(R.id.buttonMessage)
        val notificationButton = findViewById<ImageButton>(R.id.buttonNotification)
        editTextBoardName = findViewById(R.id.editTextBoardName)
        val editTextPostName = findViewById<EditText>(R.id.editTextName)

        clearIcon = ContextCompat.getDrawable(this, R.drawable.ic_clear)!!
        setClearIconVisible(false)

        val userId = userSession.getLogin()
        val postNameField = binding.editTextName

        // テキスト変更でクリアアイコン表示
        binding.editTextBoardName.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                setClearIconVisible(!s.isNullOrEmpty())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // クリアアイコンタップで入力リセット
        binding.editTextBoardName.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = binding.editTextBoardName.compoundDrawablesRelative[2]
                if (drawableEnd != null &&
                    event.rawX >= (binding.editTextBoardName.right - drawableEnd.bounds.width() - binding.editTextBoardName.paddingEnd)
                ) {
                    binding.editTextBoardName.text.clear()
                    return@setOnTouchListener true
                }
            }
            false
        }

        // ログインUI状態更新
        updateLoginUI()

        // ログイン・ログアウト切り替え
        binding.buttonLogin.setOnClickListener {
            if (userSession.isLoggedIn()) {
                userSession.logout()
                updateLoginUI()
            } else {
                startActivity(Intent(this, LoginActivity::class.java))
            }
        }

        // 掲示板に移動ボタン
        binding.buttonGo.setOnClickListener {
            val boardName = binding.editTextBoardName.text.toString()
            val postName = postNameField.text.toString()

            if (boardName.isBlank()) {
                Toast.makeText(this, "掲示板名を入力してください", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = apiService.getOrCreateBoard(BoardNameRequest(boardName, userId))
                    val boardId = response.board_id
                    withContext(Dispatchers.Main) {
                        val intent = Intent(this@MainActivity, BoardActivity::class.java).apply {
                            putExtra("BOARD_ID", boardId.toString())
                            putExtra("USER_ID", userId)
                            putExtra("POST_NAME", postName)
                        }
                        startActivity(intent)
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "掲示板作成エラー: ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "掲示板作成に失敗しました", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        loginButton.setOnClickListener {
            if (userSession.isLoggedIn()) {
                userSession.logout()
                loginButton.text = "ログイン"
                headerLayout.visibility = View.GONE
            } else {
                startActivity(Intent(this, LoginActivity::class.java))
            }
        }

        bookmarkButton.setOnClickListener {
            val intent = Intent(this, BookmarkActivity::class.java).apply {
                putExtra("USER_ID", userId)
                putExtra("POST_NAME", editTextPostName.text.toString())
            }
            startActivity(intent)
        }

        messageButton.setOnClickListener {
            val intent = Intent(this, MessageListActivity::class.java)
            intent.putExtra("USER_ID", userId)
            intent.putExtra("POST_NAME", editTextPostName.text.toString())
            startActivity(intent)
        }

        notificationButton.setOnClickListener {
            val intent = Intent(this, NotificationActivity::class.java).apply {
                putExtra("USER_ID", userId)
            }
            startActivity(intent)
        }
        // ランキング表示
        binding.rankingRecyclerView.layoutManager = LinearLayoutManager(this)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val rankings = apiService.getDailyRanking()
                withContext(Dispatchers.Main) {
                    val adapter = RankingAdapter(rankings) { board: BoardRanking ->
                        val intent = Intent(this@MainActivity, BoardActivity::class.java).apply {
                            putExtra("BOARD_ID", board.board_id.toString())
                            putExtra("USER_ID", userId)
                            putExtra("POST_NAME", postNameField.text.toString())
                        }
                        startActivity(intent)
                    }
                    binding.rankingRecyclerView.adapter = adapter
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "ランキング取得エラー: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "ランキング取得に失敗しました", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setClearIconVisible(visible: Boolean) {
        val icon = if (visible) clearIcon else null
        binding.editTextBoardName.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, icon, null)
    }

    private fun updateLoginUI() {
        if (userSession.isLoggedIn()) {
            binding.buttonLogin.text = "ログアウト"
            binding.buttonLogin.visibility = View.VISIBLE
            binding.headerLayout.visibility = View.VISIBLE
        } else {
            binding.buttonLogin.text = "ログイン"
            binding.buttonLogin.visibility = View.VISIBLE
            binding.headerLayout.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        updateLoginUI()
    }
}
