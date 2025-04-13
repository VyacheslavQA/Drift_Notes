package com.example.driftnotes

import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.example.driftnotes.databinding.ActivityMainBinding
import com.example.driftnotes.utils.FirebaseManager
import com.example.driftnotes.auth.LoginActivity
import com.example.driftnotes.fishing.AddFishingNoteActivity
import com.example.driftnotes.utils.AnimationHelper

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
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        // Настраиваем основной экран приложения
        setupUI()
    }

    private fun setupUI() {
        // Добавляем плавающую кнопку для быстрого добавления заметки
        val fabAddNote = binding.root.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabAddNote)
        fabAddNote?.setOnClickListener {
            val intent = Intent(this, AddFishingNoteActivity::class.java)
            AnimationHelper.startActivityWithAnimation(this, intent)
        }

        // Если нужна кнопка для перехода к списку заметок, но в разметке её нет,
        // можете добавить её в activity_main.xml и раскомментировать эти строки:

        /*
        val buttonMyNotes = binding.root.findViewById<android.widget.Button>(R.id.buttonMyNotes)
        buttonMyNotes?.setOnClickListener {
            val intent = Intent(this, NotesActivity::class.java)
            AnimationHelper.startActivityWithAnimation(this, intent)
        }
        */
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            // Проверьте, что в вашем main_menu.xml есть пункт с id="menu_my_notes"
            // Если нет, то замените на правильный id или добавьте его в меню
            R.id.menu_my_notes -> {
                val intent = Intent(this, NotesActivity::class.java)
                AnimationHelper.startActivityWithAnimation(this, intent)
                true
            }
            R.id.menu_logout -> {
                FirebaseManager.auth.signOut()
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // Добавляем анимацию при выходе по кнопке "Назад"
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}