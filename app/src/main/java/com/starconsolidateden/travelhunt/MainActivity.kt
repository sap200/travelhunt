package com.starconsolidateden.travelhunt

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.starconsolidateden.travelhunt.utils.SecurePrefs
import com.google.android.gms.common.api.ApiException
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.starconsolidateden.travelhunt.api.RestService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TOKEN_EXPIRY_MS = 36_000_000L // 10 hours


class MainActivity : ComponentActivity() {
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var btnGoogleLogin: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main);


        if (isUserLoggedIn()) {
            val intent = Intent(this, MapActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        if (!isUserLoggedIn()) {
            SecurePrefs.clear()
        }

        val btnSignup = findViewById<Button>(R.id.btnSignup)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnNFCLogin = findViewById<Button>(R.id.btnNFCLogin)
        btnGoogleLogin = findViewById<Button>(R.id.btnGoogleLogin)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.server_client_id)) // WEB CLIENT ID
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        btnGoogleLogin.setOnClickListener {
            btnGoogleLogin.isEnabled = false;
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)

        }

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

    // 🔹 NEW: Handle Google login result
    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                handleGoogleSignInResult(task)
            }
        }

    private fun handleGoogleSignInResult(task: Task<GoogleSignInAccount>) {
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken

            Log.d("GOOGLE_LOGIN", "ID Token received")

            // 🔹 NEXT STEP:
            // send idToken to backend → receive JWT
            // then store JWT in SecurePrefs
            sendGoogleTokenToBackend(idToken)

        } catch (e: ApiException) {
            Log.e("GOOGLE_LOGIN", "Google sign-in failed", e)
        } finally {
            btnGoogleLogin.isEnabled = true;
        }
    }

    private fun sendGoogleTokenToBackend(idToken: String?) {
        if (idToken == null) return

        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    RestService.googleLogin(idToken)
                }
                result.onSuccess {response->
                    if(response.status.equals("success")) {
                        SecurePrefs.saveString("JWT_TOKEN", response.token)
                        SecurePrefs.saveString("EMAIL", response.email)
                        SecurePrefs.saveString("PASSWORD", "")
                        SecurePrefs.saveString("ADDRESS", response.address)
                        SecurePrefs.saveString("OBJECT_ID", response.objectId)
                        SecurePrefs.saveString("EXPIRY_TIME",  (System.currentTimeMillis() + TOKEN_EXPIRY_MS).toString())

                        Toast.makeText(this@MainActivity, "Login successful!", Toast.LENGTH_SHORT).show()
                        // move on to the map activity
                        val intent = Intent(this@MainActivity, MapActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    } else {
                        Toast.makeText(this@MainActivity, response.errorMessage, Toast.LENGTH_SHORT).show()
                    }
                }.onFailure { error ->
                    Toast.makeText(this@MainActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()

                }
            } catch(ex:Exception) {
                Toast.makeText(this@MainActivity, "Error: ${ex.message}", Toast.LENGTH_SHORT).show()
            }
        }
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
