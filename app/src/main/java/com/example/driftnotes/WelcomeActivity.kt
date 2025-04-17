package com.example.driftnotes

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.driftnotes.auth.AuthMethodsActivity
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
            startActivity(Intent(this, AuthMethodsActivity::class.java))
        }

        binding.buttonRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Прямая кнопка для входа по телефону
        binding.buttonPhoneLogin.setOnClickListener {
            val intent = Intent(this, AuthMethodsActivity::class.java)
            intent.putExtra("START_PHONE_AUTH", true)
            startActivity(intent)
        }
    }
}