package com.starconsolidateden.travelhunt

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.EditText
import android.widget.Button
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.starconsolidateden.travelhunt.api.RestService
import com.starconsolidateden.travelhunt.utils.SecurePrefs
import kotlinx.coroutines.launch
import java.util.regex.Pattern

private const val TOKEN_EXPIRY_MS = 36_000_000L // 10 hours


class SignupActivity : AppCompatActivity() {

    private var passwordVisible = false;


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_signup)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.SignupPage)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnSignup = findViewById<Button>(R.id.btnSignup)

        // Eye toggle for password
        etPassword.setOnTouchListener { _, event ->
            val DRAWABLE_END = 2
            if (event.rawX >= (etPassword.right - etPassword.compoundDrawables[DRAWABLE_END].bounds.width())) {
                passwordVisible = !passwordVisible
                if (passwordVisible) {
                    etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                } else {
                    etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                }
                etPassword.setSelection(etPassword.text.length)
                true
            } else {
                false
            }
        }

        // Signup button click
        btnSignup.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
            val pattern = Pattern.compile(emailPattern)
            if (!pattern.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // do custom texts;
            // TODO: CALL THE APIS.
            lifecycleScope.launch {
                try {
                    val result = RestService.signup(email, password);
                    result.onSuccess {response->
                        if(response.status.equals("success")) {
                            SecurePrefs.saveString("JWT_TOKEN", response.token)
                            SecurePrefs.saveString("EMAIL", email)
                            SecurePrefs.saveString("PASSWORD", password)
                            SecurePrefs.saveString("ADDRESS", response.address)
                            SecurePrefs.saveString("OBJECT_ID", response.objectId)
                            SecurePrefs.saveString("EXPIRY_TIME",  (System.currentTimeMillis() + TOKEN_EXPIRY_MS).toString())
                            Toast.makeText(this@SignupActivity, "Signup successful!", Toast.LENGTH_SHORT).show()
                            // move on to the map activity
                            val intent = Intent(this@SignupActivity, MapActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                        } else {
                            Toast.makeText(this@SignupActivity, response.errorMessage, Toast.LENGTH_SHORT).show()
                        }
                    }.onFailure { error ->
                        Toast.makeText(this@SignupActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()

                    }
                } catch(ex:Exception) {
                    Toast.makeText(this@SignupActivity, "Error: ${ex.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}