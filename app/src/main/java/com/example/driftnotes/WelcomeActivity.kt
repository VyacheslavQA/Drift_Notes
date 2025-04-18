package com.example.driftnotes

import android.content.Intent
import android.widget.Toast
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.driftnotes.auth.AuthMethodsActivity
import com.example.driftnotes.auth.RegisterActivity
import com.example.driftnotes.databinding.ActivityWelcomeBinding
import com.example.driftnotes.utils.FirebaseManager
import com.google.firebase.auth.FirebaseAuth

class WelcomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWelcomeBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Скрываем ActionBar для этого экрана
        supportActionBar?.hide()

        // Инициализируем Firebase
        FirebaseManager.initialize(this)
        auth = FirebaseAuth.getInstance()

        // Проверяем, авторизован ли пользователь
        if (FirebaseManager.isUserLoggedIn()) {
            // Если пользователь уже авторизован, переходим сразу на главный экран
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // Настраиваем обработчик нажатия на кнопку "Войти"
        binding.buttonLogin.setOnClickListener {
            startActivity(Intent(this, AuthMethodsActivity::class.java))
        }

        // Обработчик для кнопки создания аккаунта
        binding.buttonCreateAccount.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Обработчик для анонимного входа
        binding.textAnonymous.setOnClickListener {
            signInAnonymously()
        }
    }

    private fun signInAnonymously() {
        auth.signInAnonymously()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Успешный анонимный вход
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    // Ошибка
                    val message = task.exception?.message ?: "Неизвестная ошибка"
                    showToast("Ошибка анонимного входа: $message")
                }
            }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}