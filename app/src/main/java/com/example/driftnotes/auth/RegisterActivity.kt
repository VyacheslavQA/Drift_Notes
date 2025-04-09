package com.example.driftnotes.auth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.driftnotes.MainActivity
import com.example.driftnotes.R
import com.example.driftnotes.databinding.ActivityRegisterBinding
import com.example.driftnotes.utils.PasswordValidator
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Показываем требования к паролю
        setupPasswordRequirements()

        // Добавляем TextWatcher для валидации пароля в режиме реального времени
        binding.editTextPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                validatePassword(s.toString())
            }
        })

        // Обработка нажатия на кнопку регистрации
        binding.buttonRegister.setOnClickListener {
            val email = binding.editTextEmail.text.toString().trim()
            val password = binding.editTextPassword.text.toString().trim()
            val confirmPassword = binding.editTextConfirmPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(
                    this,
                    getString(R.string.passwords_not_match),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Проверяем пароль на соответствие требованиям
            val validationResult = PasswordValidator.validate(password)
            if (validationResult != PasswordValidator.PasswordValidationResult.Valid) {
                val errorMessageId = PasswordValidator.getErrorMessageResId(validationResult)
                Toast.makeText(this, getString(errorMessageId), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            registerUser(email, password)
        }

        // Обработка нажатия на ссылку входа
        binding.textViewLogin.setOnClickListener {
            finish()
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
            binding.textInputLayoutPassword.helperText = "Пароль соответствует требованиям"
        } else {
            val errorMessageId = PasswordValidator.getErrorMessageResId(validationResult)
            binding.textInputLayoutPassword.error = getString(errorMessageId)
        }
    }

    private fun setupPasswordRequirements() {
        // Добавляем требования к паролю в виде bulleted list
        val requirementsList = StringBuilder()
        requirementsList.append(getString(R.string.password_requirements_title)).append("\n")

        PasswordValidator.getPasswordRequirements().forEach { stringId ->
            requirementsList.append("• ").append(getString(stringId)).append("\n")
        }

        binding.textPasswordRequirements.text = requirementsList.toString()
    }

    private fun registerUser(email: String, password: String) {
        binding.buttonRegister.isEnabled = false

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                binding.buttonRegister.isEnabled = true

                if (task.isSuccessful) {
                    // Регистрация успешна
                    Toast.makeText(
                        this,
                        getString(R.string.register_success),
                        Toast.LENGTH_SHORT
                    ).show()

                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    // Ошибка регистрации
                    val message = task.exception?.message ?: "Неизвестная ошибка"
                    Toast.makeText(
                        this,
                        getString(R.string.register_failed, message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }
}