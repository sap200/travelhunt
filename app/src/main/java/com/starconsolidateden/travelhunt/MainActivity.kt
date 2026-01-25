package com.starconsolidateden.travelhunt

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import android.widget.Button
import com.starconsolidateden.travelhunt.utils.SecurePrefs


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main);


        if(isUserLoggedIn()) {
            val intent = Intent(this, MapActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        if(!isUserLoggedIn()) {
            SecurePrefs.clear()
        }

        val btnSignup = findViewById<Button>(R.id.btnSignup)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnNFCLogin = findViewById<Button>(R.id.btnNFCLogin)

        btnSignup.setOnClickListener {
            val intent = Intent(this@MainActivity, SignupActivity::class.java);
            startActivity(intent);
        }

        btnLogin.setOnClickListener {
            val intent = Intent(this@MainActivity, LoginActivity::class.java)
            startActivity(intent)
        }

        btnNFCLogin.setOnClickListener {
            val intent = Intent(this@MainActivity, NFCLoginActivity::class.java)
            startActivity(intent)
        }

        // on load we will check the shared preference, if the keys exists then, go to Mapping page.
        // else, stay in the login page.
    }

    private fun isUserLoggedIn(): Boolean {
        val hasJwt = !SecurePrefs.getString("JWT_TOKEN").isNullOrEmpty()
        val hasEmail = !SecurePrefs.getString("EMAIL").isNullOrEmpty()
        val hasAddress = !SecurePrefs.getString("ADDRESS").isNullOrEmpty()

        val hasPassword = !SecurePrefs.getString("PASSWORD").isNullOrEmpty()
        val hasObjectId = !SecurePrefs.getString("OBJECT_ID").isNullOrEmpty()


        val expiryTimeStr = SecurePrefs.getString("EXPIRY_TIME")

        val expiryTime = expiryTimeStr?.toLongOrNull()

        val now = System.currentTimeMillis()

        val isExpired =  expiryTime == null || now > expiryTime

        return hasJwt && hasEmail && hasAddress && hasObjectId && !isExpired
    }


}
