package com.example.bulletinboard

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bulletinboard.databinding.ActivityGoogleLoginBinding
import com.example.bulletinboard.network.ApiService
import com.example.bulletinboard.network.RegisterRequest
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class GoogleLoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGoogleLoginBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var api: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGoogleLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        api = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:3000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.googleSignInButton.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, 1001)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1001) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                val email = account.email ?: ""
                Log.d("GoogleLogin", "Email: $email")

                CoroutineScope(Dispatchers.IO).launch {
                    val result = api.checkUserExistsByEmail(email)
                    if (result.exists) {
                        // ユーザーが存在する → ログインして次の画面へ
                        runOnUiThread {
                            Toast.makeText(this@GoogleLoginActivity, "ログイン成功", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this@GoogleLoginActivity, MessageListActivity::class.java)
                            intent.putExtra("USER_ID", result.userId)
                            intent.putExtra("POST_NAME", result.name)
                            startActivity(intent)
                        }
                    } else {
                        // ユーザーが存在しない → 登録画面へ遷移
                        runOnUiThread {
                            val intent = Intent(this@GoogleLoginActivity, GoogleUserRegisterActivity::class.java)
                            intent.putExtra("EMAIL", email)
                            startActivity(intent)
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Googleログインに失敗", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
