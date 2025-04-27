package com.example.driftnotes

import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.driftnotes.databinding.ActivityMainBinding
import com.example.driftnotes.models.FishingStats
import com.example.driftnotes.repository.StatsRepository
import com.example.driftnotes.utils.DateFormatter
import com.example.driftnotes.utils.FirebaseManager
import com.example.driftnotes.fishing.AddFishingNoteActivity
import com.example.driftnotes.utils.AnimationHelper
import com.google.android.material.navigation.NavigationView
import com.example.driftnotes.profile.ProfileActivity
import com.example.driftnotes.timer.TimerActivity
import com.example.driftnotes.calendar.CalendarActivity
import com.example.driftnotes.stats.StatsActivity
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var toggle: ActionBarDrawerToggle
    private val statsRepository = StatsRepository()

    // URL для перехода по клику на баннер - замените на свой URL
    private val externalResourceUrl = "https://www.youtube.com/@Carpediem_hunting_fishing"

    // Текст для баннера
    private val bannerTitle = "Посетите наш YouTube канал"

    // Форматтер для отображения десятичных чисел
    private val decimalFormat = DecimalFormat("#0.0")

    // Форматтер для отображения дат
    private val dateFormat = SimpleDateFormat("d MMMM", Locale("ru"))

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

        // Настраиваем баннер со внешней ссылкой
        setupBanner()

        // Настраиваем боковое меню
        setupDrawerMenu()

        // Настраиваем Bottom Navigation
        setupBottomNavigation()

        // Загружаем статистику
        loadStatistics()
    }

    private fun setupBanner() {
        // Устанавливаем текст на баннере
        binding.textBannerTitle?.text = bannerTitle

        // Настраиваем обработчик нажатия на баннер
        binding.cardExternalLink?.setOnClickListener {
            openExternalLink(externalResourceUrl)
        }
        // Также можно добавить обработчик на изображение отдельно
        binding.imageBanner?.setOnClickListener {
            openExternalLink(externalResourceUrl)
        }
    }

    private fun openExternalLink(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Не удалось открыть ссылку: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
            Log.e("MainActivity", "Ошибка при открытии ссылки: ${e.message}", e)
        }
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
        val userEmail = headerView.findViewById<TextView>(R.id.textViewUserEmail)
        val userName = headerView.findViewById<TextView>(R.id.textViewUsername)
        val avatarImageView = headerView.findViewById<ImageView>(R.id.imageViewAvatar)

        // Заполняем информацию о пользователе, если он авторизован
        FirebaseManager.auth.currentUser?.let { user ->
            userName.text = user.displayName ?: getString(R.string.app_name)
            userEmail.text = user.email ?: "Журнал рыбалки"

            // Загружаем аватар пользователя из Firebase
            user.photoUrl?.let { photoUrl ->
                Glide.with(this)
                    .load(photoUrl)
                    .circleCrop() // Делаем аватарку круглой средствами Glide
                    .placeholder(R.drawable.ic_fishing_hook)
                    .error(R.drawable.ic_fishing_hook)
                    .into(avatarImageView)
            } ?: run {
                // Если у пользователя нет аватара, проверяем в Firestore
                FirebaseManager.firestore.collection("users")
                    .document(user.uid)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            val photoUrl = document.getString("photoUrl")
                            if (!photoUrl.isNullOrEmpty()) {
                                Glide.with(this)
                                    .load(photoUrl)
                                    .circleCrop() // Делаем аватарку круглой средствами Glide
                                    .placeholder(R.drawable.ic_fishing_hook)
                                    .error(R.drawable.ic_fishing_hook)
                                    .into(avatarImageView)
                            }
                        }
                    }
            }
        }
    }

    /**
     * Загружает статистику рыбалок без фильтрации
     */
    private fun loadStatistics() {
        // Показываем индикатор загрузки
        binding.progressBar?.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                // Запрашиваем статистику без фильтра - за все время
                val startDate = Date(0) // с начала эпохи
                val endDate = Date() // до текущей даты

                val result = statsRepository.getFishingStats(startDate, endDate)

                if (result.isSuccess) {
                    val stats = result.getOrNull()
                    stats?.let {
                        updateUI(it)
                    } ?: run {
                        Toast.makeText(
                            this@MainActivity,
                            "Не удалось загрузить статистику",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    val exception = result.exceptionOrNull()
                    Toast.makeText(
                        this@MainActivity,
                        "Ошибка: ${exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Ошибка: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                // Скрываем индикатор загрузки
                binding.progressBar?.visibility = View.GONE
            }
        }
    }

    // Фрагмент MainActivity.kt, обновить функцию updateUI

    /**
     * Обновляет UI с данными статистики
     */
    private fun updateUI(stats: FishingStats) {
        try {
            // Блок "Всего рыбалок"
            binding.textViewTotalTripsValue?.text = stats.totalFishingTrips.toString()
            binding.textViewTotalTripsLabel?.text = getFishingTripsText(stats.totalFishingTrips)

            // Блок "Поймано рыб и среднее"
            binding.textViewTotalFishValue?.text = stats.totalFishCaught.toString()
            binding.textViewAverageFishValue?.text = decimalFormat.format(stats.averageFishPerTrip)

            // Прогресс бар
            val maxProgress = 100
            val progress = if (stats.totalFishCaught > 0) {
                minOf((stats.totalFishCaught / 2.0).toInt(), maxProgress)
            } else {
                0
            }
            binding.progressBarFish?.max = maxProgress
            binding.progressBarFish?.progress = progress

            // Блок "Самая большая рыба"
            stats.biggestFish?.let { biggestFish ->
                binding.textViewBiggestFishValue?.text = decimalFormat.format(biggestFish.weight)
                binding.textViewBiggestFishDate?.text = formatDate(biggestFish.date)
            } ?: run {
                // Если нет данных о самой большой рыбе
                binding.textViewBiggestFishValue?.text = "0,0"
                binding.textViewBiggestFishDate?.text = "Нет данных"
            }

            /// Блок "Самая долгая рыбалка"
            stats.longestTrip?.let { longestTrip ->
                // Показываем количество дней
                binding.textViewLongestTripValue?.text = longestTrip.durationDays.toString()

                // Формат даты или диапазона дат
                val dateText = if (longestTrip.endDate == null || isSameDay(longestTrip.startDate, longestTrip.endDate)) {
                    // Для однодневной рыбалки
                    formatDate(longestTrip.startDate)
                } else {
                    // Для многодневной рыбалки - короткий формат
                    "${formatDate(longestTrip.startDate)}..."
                }
                binding.textViewLongestTripDate?.text = dateText

                // Ограничиваем длину текста местоположения
                val locationText = longestTrip.location
                binding.textViewLongestTripLocation?.text =
                    if (locationText.length > 12) locationText.substring(0, 9) + "..." else locationText

                // Склонение слова "день"
                binding.textViewLongestTripDays?.text = getDaysText(longestTrip.durationDays)
            } ?: run {
                // Если нет данных о самой долгой рыбалке
                binding.textViewLongestTripValue?.text = "0"
                binding.textViewLongestTripDate?.text = "Нет данных"
                binding.textViewLongestTripLocation?.text = ""
                binding.textViewLongestTripDays?.text = "дней"
            }


        } catch (e: Exception) {
            Log.e("MainActivity", "Ошибка при обновлении UI: ${e.message}", e)
            Toast.makeText(
                this,
                "Ошибка отображения: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    /**
     * Проверяет, относятся ли две даты к одному и тому же дню
     */
    private fun isSameDay(date1: Date, date2: Date?): Boolean {
        if (date2 == null) return false

        val cal1 = Calendar.getInstance()
        val cal2 = Calendar.getInstance()
        cal1.time = date1
        cal2.time = date2

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
    }

    /**
     * Получает месяц в именительном падеже по индексу
     */
    private fun getMonthInNominative(monthIndex: Int): String {
        val monthsInNominative = mapOf(
            0 to "Январь",
            1 to "Февраль",
            2 to "Март",
            3 to "Апрель",
            4 to "Май",
            5 to "Июнь",
            6 to "Июль",
            7 to "Август",
            8 to "Сентябрь",
            9 to "Октябрь",
            10 to "Ноябрь",
            11 to "Декабрь"
        )
        return monthsInNominative[monthIndex] ?: "Неизвестный месяц"
    }

    /**
     * Форматирует дату в формате "31 декабря"
     */
    private fun formatDate(date: Date): String {
        return dateFormat.format(date)
    }

    /**
     * Возвращает правильную форму слова "день" в зависимости от количества
     */
    private fun getDaysText(days: Int): String {
        return when {
            days % 10 == 1 && days % 100 != 11 -> "день"
            days % 10 in 2..4 && (days % 100 < 10 || days % 100 > 20) -> "дня"
            else -> "дней"
        }
    }

    /**
     * Возвращает правильную форму слова "рыба" в зависимости от количества
     */
    private fun getFishText(count: Int): String {
        return when {
            count % 10 == 1 && count % 100 != 11 -> "рыба"
            count % 10 in 2..4 && (count % 100 < 10 || count % 100 > 20) -> "рыбы"
            else -> "рыб"
        }
    }

    /**
     * Возвращает правильную форму слова "рыбалка" в зависимости от количества
     */
    private fun getFishingTripsText(count: Int): String {
        return when {
            count % 10 == 1 && count % 100 != 11 -> "рыбалка"
            count % 10 in 2..4 && (count % 100 < 10 || count % 100 > 20) -> "рыбалки"
            else -> "рыбалок"
        }
    }

    /**
     * Настраивает нижнюю навигационную панель
     */
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
                    Toast.makeText(this, "Раздел Погода находится в разработке", Toast.LENGTH_SHORT)
                        .show()
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
                    Toast.makeText(
                        this,
                        "Раздел Уведомления находится в разработке",
                        Toast.LENGTH_SHORT
                    ).show()
                    true
                }

                else -> false
            }
        }
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
                Toast.makeText(this, "Раздел Настройки находится в разработке", Toast.LENGTH_SHORT)
                    .show()
            }

            R.id.nav_help -> {
                // Обработка нажатия на пункт "Помощь/Связь"
                Toast.makeText(
                    this,
                    "Раздел Помощь/Связь находится в разработке",
                    Toast.LENGTH_SHORT
                ).show()
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
        // Обновляем шапку бокового меню при возвращении к главному экрану
        // (например, если пользователь изменил свой профиль)
        setupDrawerMenu()
        // Перезагружаем данные при возвращении на экран
        loadStatistics()
    }
}