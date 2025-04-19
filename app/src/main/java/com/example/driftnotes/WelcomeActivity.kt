package com.example.driftnotes

import android.content.Intent
import android.widget.Toast
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.driftnotes.auth.AuthMethodsActivity
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

        // Настраиваем обработчик нажатия на кнопку "ВОЙТИ"
        binding.buttonLogin.setOnClickListener {
            startActivity(Intent(this, AuthMethodsActivity::class.java))
        }

        // Обработчик для кнопки "Назад" (закрывает приложение)
        binding.textBack.setOnClickListener {
            finish()
        }
    }
}