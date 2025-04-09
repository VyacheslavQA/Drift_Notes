package com.example.driftnotes.auth

import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.driftnotes.R
import com.example.driftnotes.databinding.ActivityPhoneRecoveryBinding
import com.example.driftnotes.utils.PasswordValidator
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

/**
 * Активность для восстановления пароля через телефон
 */
class PhoneRecoveryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhoneRecoveryBinding
    private lateinit var auth: FirebaseAuth

    private var verificationId: String = ""
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var phoneNumber: String = ""
    private var countDownTimer: CountDownTimer? = null

    // Режимы отображения - ввод телефона или ввод кода
    private enum class ViewMode {
        PHONE_INPUT, CODE_VERIFICATION
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhoneRecoveryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Инициализация UI в режиме ввода телефона
        updateUI(ViewMode.PHONE_INPUT)

        // Настройка требований к паролю
        setupPasswordRequirements()

        // Обработчик кнопки "Получить код"
        binding.buttonSendCode.setOnClickListener {
            val phoneNumber = binding.editTextPhone.text.toString().trim()

            if (TextUtils.isEmpty(phoneNumber)) {
                binding.editTextPhone.error = getString(R.string.phone_required)
                return@setOnClickListener
            }

            // Форматируем номер, добавляя "+" если его нет
            this.phoneNumber = if (!phoneNumber.startsWith("+")) "+$phoneNumber" else phoneNumber

            // Показываем прогресс и блокируем кнопку
            binding.progressBar.visibility = View.VISIBLE
            binding.buttonSendCode.isEnabled = false

            startPhoneNumberVerification(this.phoneNumber)
        }

        // Обработчик TextWatcher для валидации пароля
        binding.editTextNewPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validatePassword(s.toString())
            }
        })

        // Обработчик кнопки сброса пароля
        binding.buttonResetPassword.setOnClickListener {
            val code = binding.editTextCode.text.toString().trim()
            val newPassword = binding.editTextNewPassword.text.toString().trim()

            if (TextUtils.isEmpty(code) || code.length < 6) {
                Toast.makeText(this, R.string.code_required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (TextUtils.isEmpty(newPassword)) {
                binding.textInputLayoutNewPassword.error = "Введите новый пароль"
                return@setOnClickListener
            }

            // Проверяем пароль на соответствие требованиям
            val validationResult = PasswordValidator.validate(newPassword)
            if (validationResult != PasswordValidator.PasswordValidationResult.Valid) {
                val errorMessageId = PasswordValidator.getErrorMessageResId(validationResult)
                binding.textInputLayoutNewPassword.error = getString(errorMessageId)
                return@setOnClickListener
            }

            binding.progressBar.visibility = View.VISIBLE
            binding.buttonResetPassword.isEnabled = false

            verifyPhoneNumberWithCode(verificationId, code, newPassword)
        }

        // Автоматическая проверка кода, когда введены все 6 цифр
        binding.editTextCode.setOnCodeCompleteListener { code ->
            if (binding.editTextNewPassword.text.toString().isNotEmpty()) {
                val newPassword = binding.editTextNewPassword.text.toString().trim()
                val validationResult = PasswordValidator.validate(newPassword)
                if (validationResult == PasswordValidator.PasswordValidationResult.Valid) {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.buttonResetPassword.isEnabled = false
                    verifyPhoneNumberWithCode(verificationId, code, newPassword)
                }
            }
        }

        // Обработчик для повторной отправки кода
        binding.textResendCode.setOnClickListener {
            if (phoneNumber.isNotEmpty() && resendToken != null) {
                binding.progressBar.visibility = View.VISIBLE
                binding.textResendCode.isEnabled = false

                resendVerificationCode(phoneNumber, resendToken!!)
                // Запускаем таймер повторно
                startResendTimer()
            }
        }

        // Кнопка возврата на экран выбора способа восстановления
        binding.textViewBack.setOnClickListener {
            finish()
        }
    }

    private fun validatePassword(password: String) {
        if (password.isEmpty()) {
            binding.textInputLayoutNewPassword.error = null
            return
        }

        val validationResult = PasswordValidator.validate(password)
        if (validationResult == PasswordValidator.PasswordValidationResult.Valid) {
            binding.textInputLayoutNewPassword.error = null
            binding.textInputLayoutNewPassword.helperText = "Пароль соответствует требованиям"
        } else {
            val errorMessageId = PasswordValidator.getErrorMessageResId(validationResult)
            binding.textInputLayoutNewPassword.error = getString(errorMessageId)
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

    private fun startPhoneNumberVerification(phoneNumber: String) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun verifyPhoneNumberWithCode(verificationId: String, code: String, newPassword: String) {
        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        signInWithPhoneAuthCredentialAndResetPassword(credential, newPassword)
    }

    private fun resendVerificationCode(
        phoneNumber: String,
        token: PhoneAuthProvider.ForceResendingToken
    ) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(callbacks)
            .setForceResendingToken(token)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            Log.d(TAG, "onVerificationCompleted:$credential")

            // Для восстановления пароля требуется ввод нового пароля, поэтому
            // мы не можем автоматически установить пароль без его ввода
            binding.progressBar.visibility = View.GONE

            // Но можем автоматически заполнить поле кода
            val code = credential.smsCode
            if (!code.isNullOrEmpty()) {
                binding.editTextCode.setText(code)
                // Фокус на поле ввода пароля
                binding.editTextNewPassword.requestFocus()
            }
        }

        override fun onVerificationFailed(e: FirebaseException) {
            Log.w(TAG, "onVerificationFailed", e)

            binding.progressBar.visibility = View.GONE
            binding.buttonSendCode.isEnabled = true

            // Обработка ошибок верификации
            Toast.makeText(
                this@PhoneRecoveryActivity,
                getString(R.string.verification_failed, e.message),
                Toast.LENGTH_LONG
            ).show()

            updateUI(ViewMode.PHONE_INPUT)
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            Log.d(TAG, "onCodeSent:$verificationId")

            // Код отправлен, сохраняем verificationId и token для последующего использования
            this@PhoneRecoveryActivity.verificationId = verificationId
            this@PhoneRecoveryActivity.resendToken = token

            Toast.makeText(
                this@PhoneRecoveryActivity,
                R.string.code_sent,
                Toast.LENGTH_SHORT
            ).show()

            binding.progressBar.visibility = View.GONE
            binding.buttonSendCode.isEnabled = true

            // Переходим к экрану ввода кода
            updateUI(ViewMode.CODE_VERIFICATION)

            // Запускаем таймер для повторной отправки
            startResendTimer()
        }
    }

    private fun signInWithPhoneAuthCredentialAndResetPassword(credential: PhoneAuthCredential, newPassword: String) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithCredential:success")

                    // После успешной аутентификации меняем пароль
                    changePassword(newPassword)
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)

                    binding.progressBar.visibility = View.GONE
                    binding.buttonResetPassword.isEnabled = true

                    // Обработка ошибок
                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        binding.editTextCode.error = getString(R.string.invalid_code)
                    } else {
                        Toast.makeText(
                            this,
                            getString(R.string.auth_failed, task.exception?.message),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
    }

    private fun changePassword(newPassword: String) {
        val user = auth.currentUser
        user?.updatePassword(newPassword)
            ?.addOnCompleteListener { task ->
                binding.progressBar.visibility = View.GONE

                if (task.isSuccessful) {
                    Toast.makeText(
                        this,
                        R.string.password_reset_success,
                        Toast.LENGTH_SHORT
                    ).show()

                    // Выходим из аккаунта (т.к. это восстановление пароля)
                    auth.signOut()

                    // Возвращаемся к экрану входа
                    finish()
                } else {
                    binding.buttonResetPassword.isEnabled = true

                    val message = task.exception?.message ?: "Неизвестная ошибка"
                    Toast.makeText(
                        this,
                        getString(R.string.password_reset_error, message),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun updateUI(mode: ViewMode) {
        when (mode) {
            ViewMode.PHONE_INPUT -> {
                binding.phoneInputLayout.visibility = View.VISIBLE
                binding.codeVerificationLayout.visibility = View.GONE
                binding.textViewBack.visibility = View.VISIBLE
            }
            ViewMode.CODE_VERIFICATION -> {
                binding.phoneInputLayout.visibility = View.GONE
                binding.codeVerificationLayout.visibility = View.VISIBLE
                binding.textViewBack.visibility = View.GONE

                // Показываем номер телефона, на который отправлен код
                binding.textPhoneNumber.text = getString(R.string.code_sent_to, phoneNumber)
            }
        }
    }

    private fun startResendTimer() {
        // Сначала отменим предыдущий таймер, если он существует
        countDownTimer?.cancel()

        countDownTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = millisUntilFinished / 1000
                binding.textResendCode.isEnabled = false
                binding.textResendCode.text = getString(R.string.resend_code_timer, secondsRemaining)
            }

            override fun onFinish() {
                binding.textResendCode.isEnabled = true
                binding.textResendCode.text = getString(R.string.resend_code)
            }
        }
        countDownTimer?.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }

    companion object {
        private const val TAG = "PhoneRecoveryActivity"
    }
}