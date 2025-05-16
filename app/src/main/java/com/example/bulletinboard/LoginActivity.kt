package com.example.bulletinboard

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bulletinboard.UserSession

class LoginActivity : AppCompatActivity() {

    private lateinit var editTextId: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var buttonLogin: Button
    private lateinit var buttonSignup: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        editTextId = findViewById(R.id.editTextId)
        editTextPassword = findViewById(R.id.editTextPassword)
        buttonLogin = findViewById(R.id.buttonLogin)
        buttonSignup = findViewById(R.id.buttonSignup)

        buttonLogin.setOnClickListener {
            val id = editTextId.text.toString()
            val password = editTextPassword.text.toString()

            if (id.isBlank() || password.isBlank()) {
                Toast.makeText(this, "IDとパスワードを入力してください", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ここで実際はAPI呼び出しで認証しますが、例として簡単にチェックします
            if (id == "testuser" && password == "password123") {
                // UserSessionのインスタンスを作成
                val userSession = UserSession(this)

                // 認証成功 → セッション保存など
                userSession.saveUserId(id)
                userSession.setLogin(id)  // ログイン状態も保存したいなら

                Toast.makeText(this, "ログイン成功", Toast.LENGTH_SHORT).show()

                // 初期画面に戻る
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "IDまたはパスワードが違います", Toast.LENGTH_SHORT).show()
            }
        }

        buttonSignup.setOnClickListener {
            // サインアップ画面があれば遷移させる。未実装ならトーストなど
            Toast.makeText(this, "サインアップ画面は未実装です", Toast.LENGTH_SHORT).show()
        }
    }
}
