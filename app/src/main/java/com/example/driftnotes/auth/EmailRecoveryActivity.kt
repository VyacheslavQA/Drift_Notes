package com.example.driftnotes.auth

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.driftnotes.R
import com.example.driftnotes.databinding.ActivityEmailRecoveryBinding
import com.google.firebase.auth.FirebaseAuth

/**
 * Активность для восстановления пароля через email
 */
class EmailRecoveryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmailRecoveryBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmailRecoveryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Настраиваем слушатели
        binding.buttonSendResetLink.setOnClickListener {
            val email = binding.editTextEmail.text.toString().trim()
            if (email.isEmpty()) {
                binding.textInputLayoutEmail.error = getString(R.string.email) + " " + "не может быть пустым"
                return@setOnClickListener
            }

            sendPasswordResetEmail(email)
        }

        binding.textViewBack.setOnClickListener {
            finish()
        }
    }

    private fun sendPasswordResetEmail(email: String) {
        // Показываем индикатор загрузки
        binding.progressBar.visibility = View.VISIBLE
        binding.buttonSendResetLink.isEnabled = false

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                // Скрываем индикатор загрузки
                binding.progressBar.visibility = View.GONE
                binding.buttonSendResetLink.isEnabled = true

                if (task.isSuccessful) {
                    // Успешно отправлено письмо восстановления
                    Toast.makeText(
                        this,
                        getString(R.string.recovery_email_sent, email),
                        Toast.LENGTH_LONG
                    ).show()
                    // Возвращаемся на предыдущий экран
                    finish()
                } else {
                    // Ошибка при отправке
                    val message = task.exception?.message ?: "Неизвестная ошибка"
                    Toast.makeText(
                        this,
                        getString(R.string.recovery_email_error, message),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }
}