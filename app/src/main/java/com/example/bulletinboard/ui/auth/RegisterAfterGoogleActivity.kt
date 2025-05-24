package com.example.bulletinboard.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bulletinboard.LoginActivity
import com.example.bulletinboard.databinding.ActivityRegisterAfterGoogleBinding
import com.example.bulletinboard.network.RegisterAfterGoogleRequest
import com.example.bulletinboard.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RegisterAfterGoogleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterAfterGoogleBinding
    private val api = RetrofitClient.instance

    private lateinit var googleEmail: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterAfterGoogleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        googleEmail = intent.getStringExtra("email") ?: ""

        binding.buttonRegister.setOnClickListener {
            val id = binding.editTextUserId.text.toString().trim()
            val password = binding.editTextPassword.text.toString()
            val userName = binding.editTextUserName.text.toString().trim()

            var hasError = false

            if (id.isBlank()) {
                binding.editTextUserId.error = "ユーザーIDを入力してください"
                hasError = true
            } else if (id.length < 6 || id.length > 50) {
                binding.editTextUserId.error = "ユーザーIDは6〜50文字で入力してください"
                hasError = true
            } else {
                binding.editTextUserId.error = null
            }

            if (userName.isBlank()) {
                binding.editTextUserName.error = "表示名を入力してください"
                hasError = true
            } else {
                binding.editTextUserName.error = null
            }

            val passwordRegex = Regex("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,50}$")
            if (password.isBlank()) {
                binding.editTextPassword.error = "パスワードを入力してください"
                hasError = true
            } else if (!passwordRegex.matches(password)) {
                binding.editTextPassword.error = "8〜50文字の英字と数字を含めてください"
                hasError = true
            } else {
                binding.editTextPassword.error = null
            }

            if (hasError) return@setOnClickListener

            val request = RegisterAfterGoogleRequest(
                userId = id,
                password = password,
                userName = userName,
                email = googleEmail
            )

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = api.registerAfterGoogle(request)
                    runOnUiThread {
                        if (response.isSuccessful && response.body()?.success == true) {
                            Toast.makeText(this@RegisterAfterGoogleActivity, "登録完了", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@RegisterAfterGoogleActivity, LoginActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this@RegisterAfterGoogleActivity, response.body()?.message ?: "登録に失敗しました", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@RegisterAfterGoogleActivity, "通信エラー", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
