package com.example.driftnotes.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.driftnotes.MainActivity
import com.example.driftnotes.R
import com.example.driftnotes.databinding.ActivityAuthMethodsBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class AuthMethodsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthMethodsBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.w(TAG, "Google sign in failed", e)
                Toast.makeText(this, R.string.google_auth_cancelled, Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.d(TAG, "Google Sign In cancelled")
            Toast.makeText(this, R.string.google_auth_cancelled, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthMethodsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Добавляем кнопку назад в ActionBar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.choose_login_method)

        auth = FirebaseAuth.getInstance()

        // Настраиваем Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Настраиваем слушатели нажатий
        binding.buttonEmailLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        binding.buttonPhoneLogin.setOnClickListener {
            startActivity(Intent(this, PhoneAuthActivity::class.java))
        }

        binding.buttonGoogleLogin.setOnClickListener {
            signInWithGoogle()
        }

        binding.textViewCreateAccount.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.textViewBack.setOnClickListener {
            finish()
        }

        // Проверяем, нужно ли сразу открыть экран входа по телефону
        if (intent.getBooleanExtra("START_PHONE_AUTH", false)) {
            startActivity(Intent(this, PhoneAuthActivity::class.java))
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Вход успешен, переходим на главный экран
                    Log.d(TAG, "signInWithCredential:success")
                    Toast.makeText(this, R.string.login_success, Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    // Ошибка входа
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    Toast.makeText(
                        this,
                        getString(R.string.social_auth_error, task.exception?.message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    companion object {
        private const val TAG = "AuthMethodsActivity"
    }
}