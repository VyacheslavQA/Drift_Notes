package com.example.driftnotes.profile

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import com.example.driftnotes.auth.PasswordRecoveryActivity
import com.example.driftnotes.databinding.DialogChangePasswordBinding
import com.example.driftnotes.utils.PasswordValidator
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class ChangePasswordDialog(
    context: Context,
    private val onPasswordChanged: () -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogChangePasswordBinding
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Убираем заголовок диалога
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        // Инициализация привязки
        binding = DialogChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Настраиваем размер диалога
        window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        // Настраиваем обработчики кнопок
        setupButtons()
    }

    private fun setupButtons() {
        // Обработчик кнопки смены пароля
        binding.buttonChangePassword.setOnClickListener {
            val currentPassword = binding.editTextCurrentPassword.text.toString().trim()
            val newPassword = binding.editTextNewPassword.text.toString().trim()
            val confirmPassword = binding.editTextConfirmPassword.text.toString().trim()

            // Проверяем валидность ввода
            if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(context, "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Проверяем совпадение паролей
            if (newPassword != confirmPassword) {
                Toast.makeText(context, "Новые пароли не совпадают", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Проверяем соответствие нового пароля требованиям безопасности
            val validationResult = PasswordValidator.validate(newPassword)
            if (validationResult != PasswordValidator.PasswordValidationResult.Valid) {
                val errorMessageId = PasswordValidator.getErrorMessageResId(validationResult)
                Toast.makeText(context, context.getString(errorMessageId), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Повторная аутентификация пользователя
            val user = auth.currentUser
            if (user != null && user.email != null) {
                val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)

                user.reauthenticate(credential)
                    .addOnSuccessListener {
                        // Смена пароля после успешной повторной аутентификации
                        user.updatePassword(newPassword)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Пароль успешно изменен", Toast.LENGTH_SHORT).show()
                                onPasswordChanged()
                                dismiss()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Ошибка при смене пароля: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Ошибка аутентификации: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        // Обработчик кнопки отмены
        binding.buttonCancel.setOnClickListener {
            dismiss()
        }

        // Обработчик кнопки "Забыли пароль?"
        binding.textViewForgotPassword.setOnClickListener {
            context.startActivity(Intent(context, PasswordRecoveryActivity::class.java))
            dismiss()
        }
    }
}