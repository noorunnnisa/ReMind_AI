package com.example.remind_ai.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.remind_ai.R
import com.example.remind_ai.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var tvLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etRegisteredEmail)
        etPassword = findViewById(R.id.etPassword)
        btnRegister = findViewById(R.id.btnRegister)
        tvLogin = findViewById(R.id.tvLogin)

        btnRegister.setOnClickListener {
            registerUser()
        }

        tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun registerUser() {
        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (name.isEmpty()) {
            etName.error = "Name required"
            etName.requestFocus()
            return
        }

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

        if (password.length < 6) {
            etPassword.error = "Password must be at least 6 characters"
            etPassword.requestFocus()
            return
        }

        btnRegister.isEnabled = false
        btnRegister.text = "Registering..."

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid

                    if (uid != null) {
                        saveUserToRealtimeDatabase(uid, name, email)
                    } else {
                        resetButton()
                        Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    resetButton()
                    Log.e("RegisterActivity", "Auth failed", task.exception)
                    Toast.makeText(
                        this,
                        task.exception?.message ?: "Registration failed",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun saveUserToRealtimeDatabase(uid: String, name: String, email: String) {
        val user = User(
            uid = uid,
            fullName = name,
            email = email,
            role = "",
            createdAt = System.currentTimeMillis()
        )

        database.reference
            .child("users")
            .child(uid)
            .setValue(user)
            .addOnSuccessListener {
                Toast.makeText(this, "Registration Successful", Toast.LENGTH_LONG).show()

                val intent = Intent(this, RoleSelectionActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                resetButton()
                Log.e("RegisterActivity", "Realtime DB save failed", e)
                Toast.makeText(
                    this,
                    e.message ?: "Failed to save user",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun resetButton() {
        btnRegister.isEnabled = true
        btnRegister.text = "Register"
    }
}
