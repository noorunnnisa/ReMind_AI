package com.example.remind_ai.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.remind_ai.R
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: Button
    private lateinit var tvSignUp: TextView
    private lateinit var tvForgotPassword: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvSignUp = findViewById(R.id.tvSignUp)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)

        btnLogin.setOnClickListener {
            loginUser()
        }

        tvSignUp.setOnClickListener {
            startActivity(Intent(this@LoginActivity, RegisterActivity::class.java))
        }

        tvForgotPassword.setOnClickListener {
            resetPassword()
        }
    }

    private fun loginUser() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (email.isEmpty()) {
            etEmail.error = "Email required"
            etEmail.requestFocus()
            return
        }

        if (password.isEmpty()) {
            etPassword.error = "Password required"
            etPassword.requestFocus()
            return
        }

        btnLogin.isEnabled = false

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                btnLogin.isEnabled = true

                if (task.isSuccessful) {
                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this@LoginActivity, RoleSelectionActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(
                        this,
                        task.exception?.message ?: "Login failed",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun resetPassword() {
        val email = etEmail.text.toString().trim()

        if (email.isEmpty()) {
            etEmail.error = "Enter email first"
            etEmail.requestFocus()
            return
        }

        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                Toast.makeText(this, "Password reset email sent", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, it.message ?: "Reset failed", Toast.LENGTH_LONG).show()
            }
    }
}