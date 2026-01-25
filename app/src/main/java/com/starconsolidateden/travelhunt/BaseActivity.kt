package com.starconsolidateden.travelhunt.base

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.starconsolidateden.travelhunt.MainActivity
import com.starconsolidateden.travelhunt.utils.SecurePrefs

abstract class BaseActivity : AppCompatActivity() {

    override fun onResume() {
        super.onResume()
        checkSessionExpiry()
    }

    private fun checkSessionExpiry() {

        val token = SecurePrefs.getString("JWT_TOKEN")
        val expiryTimeStr = SecurePrefs.getString("EXPIRY_TIME")

        val expiryTime = expiryTimeStr?.toLongOrNull()

        val now = System.currentTimeMillis()

        val isExpired =
            token == null ||
                    expiryTime == null ||
                    now > expiryTime

        if (isExpired) {
            forceLogout()
        }
    }

    private fun forceLogout() {
        SecurePrefs.clear()

        val intent = Intent(this, MainActivity::class.java)
        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
