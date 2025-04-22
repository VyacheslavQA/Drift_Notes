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
import com.example.driftnotes.profile.ProfileActivity
import com.example.driftnotes.timer.TimerActivity
import com.example.driftnotes.calendar.CalendarActivity
import com.example.driftnotes.stats.StatsActivity

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

        // После проверки авторизации сразу открываем экран статистики
        openStatsScreen()

        // Настраиваем боковое меню
        setupDrawerMenu()

        // Настраиваем основной экран приложения
        setupUI()

        // Настраиваем Bottom Navigation
        setupBottomNavigation()
    }

    /**
     * Открывает экран статистики
     */
    private fun openStatsScreen() {
        val intent = Intent(this, StatsActivity::class.java)
        startActivity(intent)
        // Не используем finish(), чтобы MainActivity оставалась в стеке
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
        // Для обработки RecyclerView и загрузки заметок
        loadFishingNotes()
    }

    private fun loadFishingNotes() {
        val userId = FirebaseManager.getCurrentUserId() ?: return

        // Показываем индикатор загрузки
        binding.progressBar?.visibility = android.view.View.VISIBLE
        binding.textNoNotes?.visibility = android.view.View.GONE

        FirebaseManager.firestore.collection("fishing_notes")
            .whereEqualTo("userId", userId)
            .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                // Скрываем индикатор загрузки
                binding.progressBar?.visibility = android.view.View.GONE

                val fishingNotes = mutableListOf<com.example.driftnotes.models.FishingNote>()
                for (document in documents) {
                    val note = document.toObject(com.example.driftnotes.models.FishingNote::class.java)
                        .copy(id = document.id)
                    fishingNotes.add(note)
                }

                // Создаем и настраиваем адаптер
                val adapter = com.example.driftnotes.fishing.FishingNoteAdapter(fishingNotes) { note ->
                    val intent = Intent(this, com.example.driftnotes.fishing.FishingNoteDetailActivity::class.java)
                    intent.putExtra("note_id", note.id)
                    AnimationHelper.startActivityWithAnimation(this, intent)
                }
                binding.recyclerView?.adapter = adapter

                // Показываем сообщение, если нет записей
                if (fishingNotes.isEmpty()) {
                    binding.textNoNotes?.visibility = android.view.View.VISIBLE
                } else {
                    binding.textNoNotes?.visibility = android.view.View.GONE
                }
            }
            .addOnFailureListener { e ->
                // Скрываем индикатор загрузки
                binding.progressBar?.visibility = android.view.View.GONE

                // Обработка ошибки - показываем сообщение пользователю
                Toast.makeText(
                    this,
                    getString(R.string.error_loading_notes, e.message),
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavView?.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_timer -> {
                    // Кнопка "Таймер" - открываем TimerActivity
                    val intent = Intent(this, TimerActivity::class.java)
                    AnimationHelper.startActivityWithAnimation(this, intent)
                    true
                }
                R.id.navigation_weather -> {
                    // Обработка нажатия на "Погода"
                    Toast.makeText(this, "Раздел Погода находится в разработке", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.navigation_add -> {
                    // Кнопка с крючком по центру - открываем экран добавления заметки
                    val intent = Intent(this, AddFishingNoteActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(R.anim.slide_in_bottom, R.anim.slide_out_top)
                    true
                }
                R.id.navigation_calendar -> {
                    // Кнопка "Календарь" - открываем CalendarActivity
                    val intent = Intent(this, CalendarActivity::class.java)
                    AnimationHelper.startActivityWithAnimation(this, intent)
                    true
                }
                R.id.navigation_notifications -> {
                    // Обработка нажатия на "Уведомления"
                    Toast.makeText(this, "Раздел Уведомления находится в разработке", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }

        // Важно! Не отключаем центральную кнопку, иначе не сможем перейти к добавлению заметки
        // Исправляем эту ошибку из предыдущей версии:
        // bottomNav?.menu?.getItem(2)?.isEnabled = false
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
                // Открываем экран статистики
                val intent = Intent(this, StatsActivity::class.java)
                AnimationHelper.startActivityWithAnimation(this, intent)
            }
            R.id.nav_settings -> {
                // Обработка нажатия на пункт "Настройки"
                Toast.makeText(this, "Раздел Настройки находится в разработке", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_help -> {
                // Обработка нажатия на пункт "Помощь/Связь"
                Toast.makeText(this, "Раздел Помощь/Связь находится в разработке", Toast.LENGTH_SHORT).show()
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

    // Метод для обновления данных при возвращении к активности
    override fun onResume() {
        super.onResume()
        // Перезагружаем данные при возвращении на экран
        loadFishingNotes()
    }
}