package com.example.driftnotes

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.driftnotes.auth.LoginActivity
import com.example.driftnotes.auth.PhoneAuthActivity
import com.example.driftnotes.auth.RegisterActivity
import com.example.driftnotes.databinding.ActivityWelcomeBinding
import com.example.driftnotes.utils.FirebaseManager

class WelcomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWelcomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Скрываем ActionBar для этого экрана
        supportActionBar?.hide()

        // Инициализируем Firebase при запуске приложения
        FirebaseManager.initialize(this)

        // Проверяем, авторизован ли пользователь
        if (FirebaseManager.isUserLoggedIn()) {
            // Если пользователь уже авторизован, переходим сразу на главный экран
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // Настраиваем обработчики нажатий
        binding.buttonLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        binding.buttonRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.buttonPhoneLogin.setOnClickListener {
            startActivity(Intent(this, PhoneAuthActivity::class.java))
        }
    }
}