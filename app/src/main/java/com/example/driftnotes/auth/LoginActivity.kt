package com.example.driftnotes.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.driftnotes.MainActivity
import com.example.driftnotes.R
import com.example.driftnotes.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        auth = FirebaseAuth.getInstance()
        
        // Обработка нажатия на кнопку входа
        binding.buttonLogin.setOnClickListener {
            val email = binding.editTextEmail.text.toString().trim()
            val password = binding.editTextPassword.text.toString().trim()
            
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            loginUser(email, password)
        }
        
        // Обработка нажатия на ссылку регистрации
        binding.textViewRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
    
    private fun loginUser(email: String, password: String) {
        binding.buttonLogin.isEnabled = false
        
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                binding.buttonLogin.isEnabled = true
                
                if (task.isSuccessful) {
                    // Вход успешен
                    Toast.makeText(
                        this, 
                        getString(R.string.login_success), 
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    // Ошибка входа
                    val message = task.exception?.message ?: "Неизвестная ошибка"
                    Toast.makeText(
                        this, 
                        getString(R.string.login_failed, message), 
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }
}