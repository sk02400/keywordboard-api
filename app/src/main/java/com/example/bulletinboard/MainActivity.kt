package com.example.bulletinboard

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.bulletinboard.UserSession
import android.widget.EditText
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    private lateinit var userSession: UserSession
    private lateinit var loginButton: Button  // ← 追加
    private lateinit var headerLayout: View   // ← 追加
    private lateinit var buttonGo: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        userSession = UserSession(this)
        buttonGo = findViewById(R.id.buttonGo)

        loginButton = findViewById(R.id.buttonLogin)
        headerLayout = findViewById(R.id.headerLayout)
        val bookmarkButton = findViewById<Button>(R.id.buttonBookmark)
        val messageButton = findViewById<Button>(R.id.buttonMessage)
        val notificationButton = findViewById<Button>(R.id.buttonNotification)
        val editTextBoardId = findViewById<EditText>(R.id.editTextBoardId)
        val editTextPostName = findViewById<EditText>(R.id.editTextName)

        if (userSession.isLoggedIn()) {
            loginButton.visibility = View.GONE
            headerLayout.visibility = View.VISIBLE
        } else {
            loginButton.visibility = View.VISIBLE
            headerLayout.visibility = View.GONE
        }

        val userId = UserSession(this).getLogin()
        //val postName = UserSession(this).getPostName()
        buttonGo.setOnClickListener {
            val boardId = editTextBoardId.text.toString()
            val postName = editTextPostName.text.toString()

            if (boardId.isBlank()) {
                Toast.makeText(this, "キーワードを入力してください", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

//            if (userId == null) {
//                Toast.makeText(this, "ログイン情報が見つかりません", Toast.LENGTH_SHORT).show()
//                return@setOnClickListener
//            }

            // BoardActivityへ遷移
            val intent = Intent(this, BoardActivity::class.java)
            intent.putExtra("BOARD_ID", boardId)
            intent.putExtra("USER_ID", userId)
            intent.putExtra("POST_NAME", postName)
            startActivity(intent)
        }

        loginButton.setOnClickListener {
            if (userSession.isLoggedIn()) {
                // ログアウト処理
                userSession.logout()
                loginButton.text = "ログイン"
                headerLayout.visibility = View.GONE
            } else {
                // ログイン画面へ遷移
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
            }
        }

        bookmarkButton.setOnClickListener {
            // 保存した掲示板一覧画面へ遷移
            val intent = Intent(this, BookmarkActivity::class.java)
            val postName = editTextPostName.text.toString()
            intent.putExtra("USER_ID", userId)
            intent.putExtra("POST_NAME", postName)
            startActivity(intent)
        }

        messageButton.setOnClickListener {
            // DM画面へ遷移
            intent.putExtra("USER_ID", userId)
            startActivity(Intent(this, MessageActivity::class.java))
        }

        notificationButton.setOnClickListener {
            // 通知画面へ遷移
            intent.putExtra("USER_ID", userId)
            startActivity(Intent(this, NotificationActivity::class.java))
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
