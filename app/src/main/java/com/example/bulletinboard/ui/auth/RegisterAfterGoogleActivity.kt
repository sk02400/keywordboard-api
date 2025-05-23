package com.example.bulletinboard.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bulletinboard.databinding.ActivityRegisterAfterGoogleBinding
import com.example.bulletinboard.network.ApiService
import com.example.bulletinboard.network.RegisterAfterGoogleRequest
import com.example.bulletinboard.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.bulletinboard.LoginActivity

class RegisterAfterGoogleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterAfterGoogleBinding
    val api = RetrofitClient.instance

    private lateinit var googleEmail: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterAfterGoogleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        googleEmail = intent.getStringExtra("email") ?: ""

        binding.buttonRegister.setOnClickListener {
            val id = binding.editTextUserId.text.toString()
            val password = binding.editTextPassword.text.toString()
            val userName = binding.editTextUserName.text.toString()

            if (id.isBlank() || password.isBlank() || userName.isBlank()) {
                Toast.makeText(this, "すべて入力してください", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

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
                            Toast.makeText(this@RegisterAfterGoogleActivity, response.body()?.message ?: "失敗", Toast.LENGTH_SHORT).show()
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
