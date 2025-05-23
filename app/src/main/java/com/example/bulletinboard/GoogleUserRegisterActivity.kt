package com.example.bulletinboard

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bulletinboard.databinding.ActivityGoogleUserRegisterBinding
import com.example.bulletinboard.network.ApiService
import com.example.bulletinboard.network.RegisterAfterGoogleRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class GoogleUserRegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGoogleUserRegisterBinding
    private lateinit var api: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGoogleUserRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val email = intent.getStringExtra("EMAIL") ?: ""

        binding.emailTextView.text = email

        api = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:3000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)

        binding.registerButton.setOnClickListener {
            val userId = binding.userIdEditText.text.toString().trim()
            val userName = binding.userNameEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString()

            if (userId.isEmpty() || userName.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "すべての項目を入力してください", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val request = RegisterAfterGoogleRequest(
                        email = email,
                        userId = userId,
                        userName = userName,
                        password = password
                    )
                    val response = api.registerAfterGoogle(request)
                    if (response.isSuccessful && response.body()?.success == true) {
                        runOnUiThread {
                            Toast.makeText(this@GoogleUserRegisterActivity, "登録成功しました", Toast.LENGTH_SHORT).show()
                            // 登録後はログイン済みとして次画面へ遷移（例：MessageListActivity）
                            val intent = Intent(this@GoogleUserRegisterActivity, MessageListActivity::class.java)
                            intent.putExtra("USER_ID", userId)
                            intent.putExtra("POST_NAME", userName)
                            startActivity(intent)
                            finish()
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@GoogleUserRegisterActivity, "登録に失敗しました", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@GoogleUserRegisterActivity, "エラーが発生しました: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
