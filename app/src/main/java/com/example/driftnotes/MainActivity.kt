package com.example.driftnotes

import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import com.example.driftnotes.databinding.ActivityMainBinding
import com.example.driftnotes.utils.FirebaseManager
import com.example.driftnotes.fishing.AddFishingNoteActivity
import com.example.driftnotes.utils.AnimationHelper
import com.google.android.material.navigation.NavigationView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.example.driftnotes.profile.ProfileActivity
import com.example.driftnotes.timer.TimerActivity

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

        // Настраиваем Bottom Navigation
        setupBottomNavigation()
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
        // Находим кнопку добавления заметки напрямую
        val fabAddNote = findViewById<FloatingActionButton>(R.id.fabAddNote)

        // Устанавливаем обработчик клика с новой анимацией снизу вверх
        fabAddNote.setOnClickListener {
            // Открываем экран добавления заметки
            val intent = Intent(this, AddFishingNoteActivity::class.java)
            // Используем анимацию снизу-вверх для открытия экрана создания заметки
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_bottom, R.anim.slide_out_top)
        }
    }

    private fun setupBottomNavigation() {
        // Используем безопасный вызов
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavView)
        bottomNav?.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_timer -> {
                    // Запускаем активность таймеров
                    val intent = Intent(this, TimerActivity::class.java)
                    AnimationHelper.startActivityWithAnimation(this, intent)
                    true
                }
                R.id.navigation_weather -> {
                    // Обработка нажатия на "Погода"
                    Toast.makeText(this, "Погода", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.navigation_add -> {
                    // Этот пункт не должен быть выбираемым
                    false
                }
                R.id.navigation_calendar -> {
                    // Обработка нажатия на "Календарь"
                    Toast.makeText(this, "Календарь", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.navigation_notifications -> {
                    // Обработка нажатия на "Уведомления"
                    Toast.makeText(this, "Уведомления", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }

        // Отключаем центральный элемент меню с проверкой на null
        bottomNav?.menu?.getItem(2)?.isEnabled = false
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_my_notes -> {
                val intent = Intent(this, NotesActivity::class.java)
                AnimationHelper.startActivityWithAnimation(this, intent)
            }
            R.id.nav_profile -> {
                val intent = Intent(this, ProfileActivity::class.java)
                AnimationHelper.startActivityWithAnimation(this, intent)
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