package com.example.driftnotes

import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.example.driftnotes.databinding.ActivityMainBinding
import com.example.driftnotes.utils.FirebaseManager
import com.example.driftnotes.auth.LoginActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Инициализируем Firebase сервисы
        FirebaseManager.initialize(this)

        // Проверяем, авторизован ли пользователь
        if (!FirebaseManager.isUserLoggedIn()) {
            // Если нет, перенаправляем на экран входа
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Настраиваем основной экран приложения
        setupUI()
    }

    private fun setupUI() {
        // Здесь можно добавить элементы главного экрана (например, карту активности,
        // последние записи, статистику и т.д.)

        // Если в макете есть кнопка, настраиваем её
        binding.root.findViewById<android.widget.Button>(R.id.buttonMyNotes)?.setOnClickListener {
            startActivity(Intent(this, NotesActivity::class.java))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_my_notes -> {
                startActivity(Intent(this, NotesActivity::class.java))
                true
            }
            R.id.menu_logout -> {
                FirebaseManager.auth.signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}