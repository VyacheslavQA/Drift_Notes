package com.example.driftnotes.auth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.driftnotes.MainActivity
import com.example.driftnotes.R
import com.example.driftnotes.databinding.ActivityLoginBinding
import com.example.driftnotes.utils.PasswordValidator
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Добавляем TextWatcher для валидации пароля в режиме реального времени
        binding.editTextPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                validatePassword(s.toString())
            }
        })

        // Обработка нажатия на кнопку входа
        binding.buttonLogin.setOnClickListener {
            val email = binding.editTextEmail.text.toString().trim()
            val password = binding.editTextPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Проверяем пароль на соответствие требованиям
            val validationResult = PasswordValidator.validate(password)
            if (validationResult != PasswordValidator.PasswordValidationResult.Valid) {
                val errorMessageId = PasswordValidator.getErrorMessageResId(validationResult)
                Toast.makeText(this, getString(errorMessageId), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginUser(email, password)
        }

        // Обработка нажатия на ссылку регистрации
        binding.textViewRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun validatePassword(password: String) {
        if (password.isEmpty()) {
            binding.textInputLayoutPassword.error = null
            return
        }

        val validationResult = PasswordValidator.validate(password)
        if (validationResult == PasswordValidator.PasswordValidationResult.Valid) {
            binding.textInputLayoutPassword.error = null
        } else {
            val errorMessageId = PasswordValidator.getErrorMessageResId(validationResult)
            binding.textInputLayoutPassword.error = getString(errorMessageId)
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