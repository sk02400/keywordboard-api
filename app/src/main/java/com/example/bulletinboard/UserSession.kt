package com.example.bulletinboard

import android.content.Context
import android.content.SharedPreferences

class UserSession(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
    }

    fun setLogin(userId: String) {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_USER_ID, userId)
            .apply()
    }
    fun saveUserId(userId: String) {
        prefs.edit()
            .putString(KEY_USER_ID, userId)
            .apply()
    }
    fun logout() {
        prefs.edit()
            .clear()
            .apply()
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun getUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }
}
