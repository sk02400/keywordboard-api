package com.example.bulletinboard

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.bulletinboard.network.ApiClient
import com.example.bulletinboard.network.GoogleLoginRequest
import com.example.bulletinboard.network.LoginRequest
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var editTextId: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var buttonLogin: Button
    private lateinit var buttonSignup: Button
    private lateinit var buttonGoogleSignIn: Button

    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        handleGoogleSignInResult(task)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        editTextId = findViewById(R.id.editTextId)
        editTextPassword = findViewById(R.id.editTextPassword)
        buttonLogin = findViewById(R.id.buttonLogin)
        buttonSignup = findViewById(R.id.buttonSignup)
        buttonGoogleSignIn = findViewById(R.id.buttonGoogleSignIn)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

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
                    logResponseBody(response, "Login")

                    if (response.isSuccessful) {
                        UserSession(this@LoginActivity).setLogin(id)
                        Toast.makeText(this@LoginActivity, "ログイン成功", Toast.LENGTH_SHORT).show()
                        navigateToMain()
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
            Toast.makeText(this, "サインアップ画面は未実装です", Toast.LENGTH_SHORT).show()
        }

        buttonGoogleSignIn.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }

    private fun handleGoogleSignInResult(task: Task<GoogleSignInAccount>) {
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            val userId = account.email ?: account.id ?: "unknown"
            val email = account.email

            if (idToken != null && email != null) {
                val request = GoogleLoginRequest(idToken, email)
                lifecycleScope.launch {
                    try {
                        val response = ApiClient.apiService.googleLogin(request)
                        //logResponseBody(response, "GoogleLogin")

                        if (response.isSuccessful && response.body()?.success == true) {
                            UserSession(this@LoginActivity).setLogin(email)
                            Toast.makeText(this@LoginActivity, "Googleログイン成功", Toast.LENGTH_SHORT).show()
                            navigateToMain()
                        } else if (response.errorBody()?.string()?.contains("User not registered") == true) {
                            // 🔴 ユーザー未登録 → 登録画面へ遷移
                            val intent = Intent(this@LoginActivity, RegisterAfterGoogleLoginActivity::class.java)
                            intent.putExtra("email", email)
                            startActivity(intent)
                        }  else {
                            Log.d("GoogleLogin", "ID Token: $idToken")
                            Toast.makeText(this@LoginActivity, "Google認証失敗", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Log.e("GoogleLogin", "通信エラー: ${e.message}", e)
                        Toast.makeText(this@LoginActivity, "通信エラーが発生しました", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "メールアドレスまたはIDトークンの取得に失敗しました", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            Log.w("GoogleSignIn", "signInResult:failed code=" + e.statusCode)
            Toast.makeText(this, "Googleサインイン失敗", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    /**
     * レスポンスの生のJSONボディをログに出す（成功・失敗問わず）
     * RetrofitのResponse<T>を受け取って文字列化
     */
    private suspend fun <T> logResponseBody(response: Response<T>, tag: String) {
        try {
            if (response.isSuccessful) {
                // 成功時のbodyはすでにオブジェクト。JSON文字列は直接取れないことが多い
                // なので toString() で代用（デバッグ用）
                Log.d(tag, "成功レスポンス body.toString(): ${response.body()?.toString()}")
            } else {
                // エラーボディはstring()で生のJSONを取得可能。ただし1回だけ読み取り可
                val errorBodyString = response.errorBody()?.string()
                Log.e(tag, "エラーレスポンス body: $errorBodyString")
            }
        } catch (e: Exception) {
            Log.e(tag, "レスポンスログ取得エラー: ${e.message}", e)
        }
    }
}
