package com.example.bulletinboard

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bulletinboard.model.BoardNameRequest
import com.example.bulletinboard.network.ApiClient
import com.example.bulletinboard.network.ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var userSession: UserSession
    private lateinit var loginButton: Button
    private lateinit var headerLayout: View
    private lateinit var buttonGo: Button
    private lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        apiService = ApiClient.apiService
        userSession = UserSession(this)

        buttonGo = findViewById(R.id.buttonGo)
        loginButton = findViewById(R.id.buttonLogin)
        headerLayout = findViewById(R.id.headerLayout)
        val bookmarkButton = findViewById<Button>(R.id.buttonBookmark)
        val messageButton = findViewById<Button>(R.id.buttonMessage)
        val notificationButton = findViewById<Button>(R.id.buttonNotification)
        val editTextBoardName = findViewById<EditText>(R.id.editTextBoardName)
        val editTextPostName = findViewById<EditText>(R.id.editTextName)

        if (userSession.isLoggedIn()) {
            loginButton.visibility = View.GONE
            headerLayout.visibility = View.VISIBLE
        } else {
            loginButton.visibility = View.VISIBLE
            headerLayout.visibility = View.GONE
        }

        val userId = userSession.getLogin()

        buttonGo.setOnClickListener {
            val boardName = editTextBoardName.text.toString()
            val postName = editTextPostName.text.toString()

            if (boardName.isBlank()) {
                Toast.makeText(this, "掲示板名を入力してください", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = apiService.getOrCreateBoard(BoardNameRequest(boardName))
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
                    Log.e("Login", "エラー: ${e.message}", e)
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
            val intent = Intent(this, MessageActivity::class.java).apply {
                putExtra("USER_ID", userId)
            }
            startActivity(intent)
        }

        notificationButton.setOnClickListener {
            val intent = Intent(this, NotificationActivity::class.java).apply {
                putExtra("USER_ID", userId)
            }
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        if (userSession.isLoggedIn()) {
            loginButton.text = "ログアウト"
            loginButton.visibility = View.VISIBLE
            headerLayout.visibility = View.VISIBLE
        } else {
            loginButton.text = "ログイン"
            loginButton.visibility = View.VISIBLE
            headerLayout.visibility = View.GONE
        }
    }
}
