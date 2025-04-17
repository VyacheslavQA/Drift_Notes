package com.example.driftnotes

import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import com.example.driftnotes.databinding.ActivityMainBinding
import com.example.driftnotes.utils.FirebaseManager
import com.example.driftnotes.auth.LoginActivity
import com.example.driftnotes.fishing.AddFishingNoteActivity
import com.example.driftnotes.utils.AnimationHelper
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var toggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Инициализируем Firebase сервисы
        FirebaseManager.initialize(this)

        // Проверяем, авторизован ли пользователь
        if (!FirebaseManager.isUserLoggedIn()) {
            // Если нет, перенаправляем на экран приветствия
            val intent = Intent(this, WelcomeActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        // Настраиваем боковое меню
        setupDrawerMenu()

        // Настраиваем основной экран приложения
        setupUI()
    }

    private fun setupDrawerMenu() {
        // Настраиваем иконку "гамбургер" для открытия бокового меню
        toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Показываем кнопку "назад" (которая будет заменена на "гамбургер")
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Настраиваем обработчик нажатий в меню
        binding.navigationView.setNavigationItemSelectedListener(this)

        // Обновляем информацию в шапке меню
        val headerView = binding.navigationView.getHeaderView(0)
        val userEmail = headerView.findViewById<android.widget.TextView>(R.id.textViewUserEmail)
        val userName = headerView.findViewById<android.widget.TextView>(R.id.textViewUsername)

        // Заполняем информацию о пользователе, если он авторизован
        FirebaseManager.auth.currentUser?.let { user ->
            userName.text = user.displayName ?: getString(R.string.app_name)
            userEmail.text = user.email ?: "Журнал рыбалки"
        }
    }

    private fun setupUI() {
        // Добавляем плавающую кнопку для быстрого добавления заметки
        binding.fabAddNote.setOnClickListener {
            val intent = Intent(this, AddFishingNoteActivity::class.java)
            AnimationHelper.startActivityWithAnimation(this, intent)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_my_notes -> {
                val intent = Intent(this, NotesActivity::class.java)
                AnimationHelper.startActivityWithAnimation(this, intent)
            }
            R.id.nav_profile -> {
                // Обработка нажатия на пункт "Личный кабинет"
                // Здесь будет открываться экран профиля, когда он будет создан
            }
            R.id.nav_stats -> {
                // Обработка нажатия на пункт "Статистика"
                // Здесь будет открываться экран статистики, когда он будет создан
            }
            R.id.nav_settings -> {
                // Обработка нажатия на пункт "Настройки"
                // Здесь будет открываться экран настроек, когда он будет создан
            }
            R.id.nav_help -> {
                // Обработка нажатия на пункт "Помощь/Связь"
                // Здесь будет открываться экран поддержки, когда он будет создан
            }
            R.id.nav_logout -> {
                FirebaseManager.auth.signOut()
                val intent = Intent(this, WelcomeActivity::class.java)
                startActivity(intent)
                finish()
            }
        }

        // Закрываем drawer после выбора пункта меню
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    // Обработка нажатия на иконку "гамбургер"
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    // Обработка нажатия на кнопку "назад"
    override fun onBackPressed() {
        // Если drawer открыт, закрываем его
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
            // Добавляем анимацию при выходе по кнопке "Назад"
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }
}