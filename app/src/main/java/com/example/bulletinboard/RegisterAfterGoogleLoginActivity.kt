package com.example.bulletinboard

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.bulletinboard.network.ApiClient
import com.example.bulletinboard.network.RegisterRequest
import kotlinx.coroutines.launch

class RegisterAfterGoogleLoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_after_google)

        val email = intent.getStringExtra("email") ?: return

        val editTextUserId = findViewById<EditText>(R.id.editTextUserId)
        val editTextUserName = findViewById<EditText>(R.id.editTextUserName)
        val editTextPassword = findViewById<EditText>(R.id.editTextPassword)
        val buttonRegister = findViewById<Button>(R.id.buttonRegister)

        buttonRegister.setOnClickListener {
            val userId = editTextUserId.text.toString()
            val userName = editTextUserName.text.toString()
            val password = editTextPassword.text.toString()

            if (userId.isBlank() || userName.isBlank() || password.isBlank()) {
                Toast.makeText(this, "全て入力してください", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val request = RegisterRequest(email, userId, userName, password)
                    val response = ApiClient.apiService.registerAfterGoogleLogin(request)
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@RegisterAfterGoogleLoginActivity, "登録成功", Toast.LENGTH_SHORT).show()
                        finish() // LoginActivity に戻る
                    } else {
                        val body = response.body()
                        val error = response.errorBody()?.string()

                        Log.e("Login", "response.isSuccessful=${response.isSuccessful}, body=$body, error=$error")

                        val errorMessage = response.body()?.message ?: "不明なエラー"
                        Log.e("Login", "登録失敗: $errorMessage")
                        Toast.makeText(this@RegisterAfterGoogleLoginActivity, "登録失敗", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("Login", "通信エラー: ${e.message}", e)
                    Toast.makeText(this@RegisterAfterGoogleLoginActivity, "通信エラー", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
