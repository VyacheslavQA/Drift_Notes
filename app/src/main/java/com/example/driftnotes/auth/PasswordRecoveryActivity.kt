package com.example.driftnotes.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.driftnotes.databinding.ActivityPasswordRecoveryBinding

/**
 * Активность для выбора способа восстановления пароля
 */
class PasswordRecoveryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPasswordRecoveryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPasswordRecoveryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Настраиваем обработчики кнопок
        binding.buttonRecoveryByEmail.setOnClickListener {
            // Переход на восстановление через email
            startActivity(Intent(this, EmailRecoveryActivity::class.java))
        }

        binding.buttonRecoveryByPhone.setOnClickListener {
            // Переход на восстановление через телефон
            startActivity(Intent(this, PhoneRecoveryActivity::class.java))
        }

        binding.textViewBackToLogin.setOnClickListener {
            // Возвращаемся на экран входа
            finish()
        }
    }
}