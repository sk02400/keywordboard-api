package com.example.bulletinboard

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val editId = findViewById<EditText>(R.id.editTextId)
        val editPassword = findViewById<EditText>(R.id.editTextPassword)
        val loginButton = findViewById<Button>(R.id.buttonLogin)
        val signupButton = findViewById<Button>(R.id.buttonSignup)

        loginButton.setOnClickListener {
            val id = editId.text.toString()
            val pw = editPassword.text.toString()
            // TODO: 認証処理をここに書く
        }

        signupButton.setOnClickListener {
            // TODO: サインアップ画面に遷移（未実装の場合はToastなどでもOK）
        }
    }
}
