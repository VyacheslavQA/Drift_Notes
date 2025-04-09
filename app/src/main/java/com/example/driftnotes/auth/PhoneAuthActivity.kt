package com.example.driftnotes.auth

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.driftnotes.MainActivity
import com.example.driftnotes.R
import com.example.driftnotes.databinding.ActivityPhoneAuthBinding
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class PhoneAuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhoneAuthBinding
    private lateinit var auth: FirebaseAuth

    private var verificationId: String = ""
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var phoneNumber: String = ""

    // Режимы отображения - ввод телефона или ввод кода
    private enum class ViewMode {
        PHONE_INPUT, CODE_VERIFICATION
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhoneAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Инициализация UI в режиме ввода телефона
        updateUI(ViewMode.PHONE_INPUT)

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

        // Обработчик кнопки "Подтвердить код"
        binding.buttonVerifyCode.setOnClickListener {
            val code = binding.editTextCode.text.toString().trim()

            if (TextUtils.isEmpty(code) || code.length < 6) {
                Toast.makeText(this, R.string.code_required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.progressBar.visibility = View.VISIBLE
            binding.buttonVerifyCode.isEnabled = false

            verifyPhoneNumberWithCode(verificationId, code)
        }

        // Автоматическая проверка кода, когда введены все 6 цифр
        binding.editTextCode.setOnCodeCompleteListener { code ->
            binding.progressBar.visibility = View.VISIBLE
            binding.buttonVerifyCode.isEnabled = false

            verifyPhoneNumberWithCode(verificationId, code)
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

        // Кнопка возврата к вводу телефона
        binding.textBackToPhone.setOnClickListener {
            updateUI(ViewMode.PHONE_INPUT)
        }
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

    private fun verifyPhoneNumberWithCode(verificationId: String, code: String) {
        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        signInWithPhoneAuthCredential(credential)
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

            // Автоматически получен код верификации (например, через SMS Retriever API)
            binding.progressBar.visibility = View.VISIBLE
            binding.buttonVerifyCode.isEnabled = false

            signInWithPhoneAuthCredential(credential)
        }

        override fun onVerificationFailed(e: FirebaseException) {
            Log.w(TAG, "onVerificationFailed", e)

            binding.progressBar.visibility = View.GONE
            binding.buttonSendCode.isEnabled = true
            binding.buttonVerifyCode.isEnabled = true

            // Обработка ошибок верификации
            Toast.makeText(
                this@PhoneAuthActivity,
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
            this@PhoneAuthActivity.verificationId = verificationId
            this@PhoneAuthActivity.resendToken = token

            Toast.makeText(
                this@PhoneAuthActivity,
                R.string.code_sent,
                Toast.LENGTH_SHORT
            ).show()

            binding.progressBar.visibility = View.GONE

            // Переходим к экрану ввода кода
            updateUI(ViewMode.CODE_VERIFICATION)

            // Запускаем таймер для повторной отправки
            startResendTimer()
        }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                binding.progressBar.visibility = View.GONE

                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithCredential:success")

                    Toast.makeText(
                        this,
                        R.string.login_success,
                        Toast.LENGTH_SHORT
                    ).show()

                    // Переходим на главный экран
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)

                    // Включаем кнопку
                    binding.buttonVerifyCode.isEnabled = true

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

    private fun updateUI(mode: ViewMode) {
        when (mode) {
            ViewMode.PHONE_INPUT -> {
                binding.phoneInputLayout.visibility = View.VISIBLE
                binding.codeVerificationLayout.visibility = View.GONE
            }
            ViewMode.CODE_VERIFICATION -> {
                binding.phoneInputLayout.visibility = View.GONE
                binding.codeVerificationLayout.visibility = View.VISIBLE

                // Показываем номер телефона, на который отправлен код
                binding.textPhoneNumber.text = getString(R.string.code_sent_to, phoneNumber)
            }
        }
    }

    private fun startResendTimer() {
        val countDownTimer = object : CountDownTimer(60000, 1000) {
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
        countDownTimer.start()
    }

    companion object {
        private const val TAG = "PhoneAuthActivity"
    }
}