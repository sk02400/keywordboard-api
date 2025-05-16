// LoginActivity.kt
package com.example.bulletinboard

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.bulletinboard.network.ApiClient
import com.example.bulletinboard.network.LoginRequest
import kotlinx.coroutines.launch
import android.util.Log
import com.example.bulletinboard.network.LoginResponse

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

            lifecycleScope.launch {
                try {
                    val response = ApiClient.apiService.login(LoginRequest(id, password))
                    if (response.isSuccessful && response.body()?.success == true) {
                        val token = response.body()?.token
                        // トークンやIDの保存（例としてUserSessionのsetLoginでID保存）
                        UserSession(this@LoginActivity).setLogin(id)

                        Toast.makeText(this@LoginActivity, "ログイン成功", Toast.LENGTH_SHORT).show()

                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, response.body()?.message ?: "ログイン失敗", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("Login", "通信エラー: ${e.message}", e)
                    Toast.makeText(this@LoginActivity, "通信エラーが発生しました", Toast.LENGTH_SHORT).show()
                }
            }
        }

        buttonSignup.setOnClickListener {
            // サインアップ画面への遷移（未実装の場合はトースト表示）
            Toast.makeText(this, "サインアップ画面は未実装です", Toast.LENGTH_SHORT).show()
        }
    }
}
