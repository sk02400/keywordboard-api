package com.example.bulletinboard

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.bulletinboard.UserSession

class MainActivity : AppCompatActivity() {

    private lateinit var userSession: UserSession
    private lateinit var loginButton: Button  // ← 追加
    private lateinit var headerLayout: View   // ← 追加

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        userSession = UserSession(this)
        loginButton = findViewById(R.id.buttonLogin)
        headerLayout = findViewById(R.id.headerLayout)

        val loginButton = findViewById<Button>(R.id.buttonLogin)
        val headerLayout = findViewById<View>(R.id.headerLayout)
        val bookmarkButton = findViewById<Button>(R.id.buttonBookmark)
        val messageButton = findViewById<Button>(R.id.buttonMessage)
        val notificationButton = findViewById<Button>(R.id.buttonNotification)

        if (userSession.isLoggedIn()) {
            loginButton.visibility = View.GONE
            headerLayout.visibility = View.VISIBLE
        } else {
            loginButton.visibility = View.VISIBLE
            headerLayout.visibility = View.GONE
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
            startActivity(Intent(this, BookmarkActivity::class.java))
        }

        messageButton.setOnClickListener {
            // DM画面へ遷移
            startActivity(Intent(this, MessageActivity::class.java))
        }

        notificationButton.setOnClickListener {
            // 通知画面へ遷移
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
