package com.example.driftnotes.stats

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.driftnotes.R
import com.example.driftnotes.databinding.ActivityStatsBinding
import com.example.driftnotes.models.FishingStats
import com.example.driftnotes.repository.StatsRepository
import com.example.driftnotes.utils.AnimationHelper
import com.example.driftnotes.utils.DateFormatter
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class StatsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatsBinding
    private val statsRepository = StatsRepository()
    private val TAG = "StatsActivity"

    // Форматтер для отображения десятичных чисел
    private val decimalFormat = DecimalFormat("#0.0")

    // Форматтер для отображения дат
    private val dateFormat = SimpleDateFormat("d MMMM", Locale("ru"))
    private val monthFormat = SimpleDateFormat("MMMM", Locale("ru"))
    private val simpleDateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("ru"))

    // Календари для хранения диапазона дат
    private val startDateCalendar = Calendar.getInstance()
    private val endDateCalendar = Calendar.getInstance()

    // Текущий тип фильтра
    private var currentFilterType = "all_time"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Настраиваем ActionBar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.statistics_title)

        // Инициализируем даты фильтрации
        initDateRange()

        // Настраиваем обработчики нажатий на фильтры
        setupFilterListeners()

        // Загружаем данные
        loadStatistics()
    }

    /**
     * Инициализирует начальный диапазон дат
     */
    private fun initDateRange() {
        // По умолчанию, настраиваем диапазон "Всё время"
        // Для начальной даты берем 1 января 2000 года
        startDateCalendar.set(2000, 0, 1, 0, 0, 0)

        // Для конечной даты устанавливаем текущую дату
        endDateCalendar.time = Date()

        // Устанавливаем выбранный фильтр "Всё время" по умолчанию
        binding.chipAllTime.isChecked = true

        // Обновляем текст на кнопках
        updateDateButtonsText()
    }

    /**
     * Настраивает обработчики нажатий на кнопки фильтров
     */
    private fun setupFilterListeners() {
        // Настраиваем обработчик для кнопки применения фильтра
        binding.buttonApplyFilter.setOnClickListener {
            loadStatistics()
        }

        // Обработчики для чипов с периодами
        binding.chipWeek.setOnClickListener { applyWeekFilter() }
        binding.chipMonth.setOnClickListener { applyMonthFilter() }
        binding.chipYear.setOnClickListener { applyYearFilter() }
        binding.chipAllTime.setOnClickListener { applyAllTimeFilter() }

        // Обработчики для кнопок выбора даты
        binding.buttonStartDate.setOnClickListener { showDateFilterDialog() }
        binding.buttonEndDate.setOnClickListener { showDateFilterDialog() }
    }

    /**
     * Применяет фильтр "Неделя"
     */
    private fun applyWeekFilter() {
        currentFilterType = "week"
        updateSelectedChips()

        // Устанавливаем конец недели на текущую дату
        endDateCalendar.time = Date()

        // Устанавливаем начало недели на 7 дней назад
        startDateCalendar.time = Date()
        startDateCalendar.add(Calendar.DAY_OF_YEAR, -7)

        // Обновляем текст на кнопках
        updateDateButtonsText()

        // Загружаем статистику с новым фильтром
        loadStatistics()
    }

    /**
     * Применяет фильтр "Месяц"
     */
    private fun applyMonthFilter() {
        currentFilterType = "month"
        updateSelectedChips()

        // Устанавливаем конец месяца на текущую дату
        endDateCalendar.time = Date()

        // Устанавливаем начало месяца на 30 дней назад
        startDateCalendar.time = Date()
        startDateCalendar.add(Calendar.DAY_OF_YEAR, -30)

        // Обновляем текст на кнопках
        updateDateButtonsText()

        // Загружаем статистику с новым фильтром
        loadStatistics()
    }

    /**
     * Применяет фильтр "Год"
     */
    private fun applyYearFilter() {
        currentFilterType = "year"
        updateSelectedChips()

        // Устанавливаем конец года на текущую дату
        endDateCalendar.time = Date()

        // Устанавливаем начало года на 365 дней назад
        startDateCalendar.time = Date()
        startDateCalendar.add(Calendar.DAY_OF_YEAR, -365)

        // Обновляем текст на кнопках
        updateDateButtonsText()

        // Загружаем статистику с новым фильтром
        loadStatistics()
    }

    /**
     * Применяет фильтр "Всё время"
     */
    private fun applyAllTimeFilter() {
        currentFilterType = "all_time"
        updateSelectedChips()

        // Устанавливаем начало на 1 января 2000 года
        startDateCalendar.set(2000, 0, 1, 0, 0, 0)

        // Устанавливаем конец на текущую дату
        endDateCalendar.time = Date()

        // Обновляем текст на кнопках
        updateDateButtonsText()

        // Загружаем статистику с новым фильтром
        loadStatistics()
    }

    /**
     * Обновляет выделение чипов фильтров
     */
    private fun updateSelectedChips() {
        // Сбрасываем выделение у всех чипов
        binding.chipWeek.isChecked = false
        binding.chipMonth.isChecked = false
        binding.chipYear.isChecked = false
        binding.chipAllTime.isChecked = false

        // Устанавливаем выделение у активного фильтра
        when (currentFilterType) {
            "week" -> binding.chipWeek.isChecked = true
            "month" -> binding.chipMonth.isChecked = true
            "year" -> binding.chipYear.isChecked = true
            "all_time" -> binding.chipAllTime.isChecked = true
            "custom" -> {
                // Для пользовательского диапазона ничего не выделяем
            }
        }
    }

    /**
     * Обновляет текст на кнопках выбора даты
     */
    private fun updateDateButtonsText() {
        // Форматируем даты в строки для кнопок
        val startDateText = simpleDateFormat.format(startDateCalendar.time)
        val endDateText = simpleDateFormat.format(endDateCalendar.time)

        // Обновляем текст на кнопках
        binding.buttonStartDate.text = "C: $startDateText"
        binding.buttonEndDate.text = "По: $endDateText"
    }

    /**
     * Показывает диалог фильтрации по датам
     */
    private fun showDateFilterDialog() {
        val dialog = DateFilterDialog(
            this,
            startDateCalendar.time,
            endDateCalendar.time
        ) { startDate, endDate, filterType ->
            // Обновляем даты
            startDateCalendar.time = startDate
            endDateCalendar.time = endDate

            // Обновляем тип фильтра
            currentFilterType = filterType

            // Обновляем выделение чипов
            updateSelectedChips()

            // Обновляем текст на кнопках
            updateDateButtonsText()

            // Загружаем статистику с новым фильтром
            loadStatistics()
        }

        dialog.show()
    }

    /**
     * Загружает статистику рыбалок с применением текущего фильтра
     */
    private fun loadStatistics() {
        // Показываем индикатор загрузки
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                // Получаем даты из календарей
                val startDate = startDateCalendar.time
                val endDate = endDateCalendar.time

                // Запрашиваем статистику с фильтром по датам
                val result = statsRepository.getFishingStats(startDate, endDate)

                if (result.isSuccess) {
                    val stats = result.getOrNull()
                    stats?.let {
                        updateUI(it)
                    } ?: run {
                        Toast.makeText(
                            this@StatsActivity,
                            "Не удалось загрузить статистику",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    val exception = result.exceptionOrNull()
                    Toast.makeText(
                        this@StatsActivity,
                        "Ошибка: ${exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке статистики: ${e.message}", e)
                Toast.makeText(
                    this@StatsActivity,
                    "Ошибка: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                // Скрываем индикатор загрузки
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    /**
     * Обновляет UI с данными статистики
     */
    private fun updateUI(stats: FishingStats) {
        try {
            // Блок "Всего рыбалок"
            binding.textViewTotalTripsValue.text = stats.totalFishingTrips.toString()

            // Блок "Поймано рыб и среднее"
            binding.textViewTotalFishValue.text = stats.totalFishCaught.toString()
            binding.textViewAverageFishValue.text = decimalFormat.format(stats.averageFishPerTrip)

            // Прогресс бар
            // Устанавливаем прогресс на 70% от максимального значения для визуального эффекта
            val maxProgress = 100
            val progress = if (stats.totalFishCaught > 0) {
                minOf((stats.totalFishCaught / 2.0).toInt(), maxProgress)
            } else {
                0
            }
            binding.progressBarFish.max = maxProgress
            binding.progressBarFish.progress = progress

            // Блок "Самая большая рыба"
            stats.biggestFish?.let { biggestFish ->
                binding.textViewBiggestFishValue.text = decimalFormat.format(biggestFish.weight)
                binding.textViewBiggestFishDate.text = formatDate(biggestFish.date)
                binding.textViewBiggestFishLocation.text = biggestFish.location
            } ?: run {
                // Если нет данных о самой большой рыбе
                binding.textViewBiggestFishValue.text = "0,0"
                binding.textViewBiggestFishDate.text = "Нет данных"
                binding.textViewBiggestFishLocation.text = ""
            }

            // Блок "Самая долгая рыбалка"
            stats.longestTrip?.let { longestTrip ->
                binding.textViewLongestTripValue.text = longestTrip.durationDays.toString()

                // Формат: "12-15 августа"
                val dateRangeText = DateFormatter.formatDateRange(longestTrip.startDate, longestTrip.endDate)
                binding.textViewLongestTripDate.text = dateRangeText

                binding.textViewLongestTripLocation.text = longestTrip.location

                // Склонение слова "день"
                binding.textViewLongestTripDays.text = getDaysText(longestTrip.durationDays)
            } ?: run {
                // Если нет данных о самой долгой рыбалке
                binding.textViewLongestTripValue.text = "0"
                binding.textViewLongestTripDate.text = "Нет данных"
                binding.textViewLongestTripLocation.text = ""
                binding.textViewLongestTripDays.text = "дней"
            }

            // Блок "Лучший месяц"
            stats.bestMonth?.let { bestMonth ->
                // Получаем название месяца
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.MONTH, bestMonth.month - 1) // -1 т.к. месяц начинается с 0
                val monthName = monthFormat.format(calendar.time).capitalize()

                binding.textViewBestMonthValue.text = monthName
                binding.textViewBestMonthCount.text = "${bestMonth.fishCount} ${getFishText(bestMonth.fishCount)}"
            } ?: run {
                // Если нет данных о лучшем месяце
                binding.textViewBestMonthValue.text = "Нет данных"
                binding.textViewBestMonthCount.text = "0 рыб"
            }

            // Блок "Последний выезд"
            stats.lastTrip?.let { lastTrip ->
                binding.textViewLastTripLocation.text = lastTrip.location
                binding.textViewLastTripDate.text = formatDate(lastTrip.date)
            } ?: run {
                // Если нет данных о последней рыбалке
                binding.textViewLastTripLocation.text = "Нет данных"
                binding.textViewLastTripDate.text = ""
            }

            // Блок "Последние трофеи (фото)"
            handleTrophies(stats.lastTrophies)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при обновлении UI: ${e.message}", e)
            Toast.makeText(
                this,
                "Ошибка отображения: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
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
     * Обрабатывает отображение трофеев
     */
    private fun handleTrophies(trophies: List<com.example.driftnotes.models.TrophyInfo>) {
        // Массивы для удобства работы с трофеями
        val trophyImages = arrayOf(
            binding.imageTrophy1,
            binding.imageTrophy2,
            binding.imageTrophy3
        )

        val trophyDateTexts = arrayOf(
            binding.textTrophy1Date,
            binding.textTrophy2Date,
            binding.textTrophy3Date
        )

        val trophyCards = arrayOf(
            binding.cardTrophy1,
            binding.cardTrophy2,
            binding.cardTrophy3
        )

        // Скрываем все карточки трофеев по умолчанию
        trophyCards.forEach { it.visibility = View.GONE }

        // Если список трофеев пуст, скрываем весь блок
        if (trophies.isEmpty()) {
            binding.textViewTrophiesTitle.visibility = View.GONE
            binding.scrollTrophies.visibility = View.GONE
            return
        }

        // Показываем блок трофеев
        binding.textViewTrophiesTitle.visibility = View.VISIBLE
        binding.scrollTrophies.visibility = View.VISIBLE

        // Заполняем данные о трофеях
        trophies.forEachIndexed { index, trophy ->
            if (index < 3) { // Максимум 3 трофея
                // Показываем карточку трофея
                trophyCards[index].visibility = View.VISIBLE

                // Устанавливаем дату поимки
                val dateStr = formatShortDate(trophy.date)
                trophyDateTexts[index].text = dateStr

                // Загружаем изображение
                if (trophy.photoUrl.isNotEmpty()) {
                    loadImageWithGlide(trophy.photoUrl, trophyImages[index])
                } else {
                    // Если нет фото, показываем иконку рыбы
                    trophyImages[index].setImageResource(R.drawable.ic_fish)
                }
            }
        }
    }

    /**
     * Форматирует дату в коротком формате "1 сен."
     */
    private fun formatShortDate(date: Date): String {
        val calendar = Calendar.getInstance()
        calendar.time = date

        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // Получаем первые три буквы месяца
        val month = monthFormat.format(date).substring(0, 3)

        return "$day $month."
    }

    /**
     * Загружает изображение с помощью Glide
     */
    private fun loadImageWithGlide(url: String, imageView: ImageView) {
        Glide.with(this)
            .load(url)
            .placeholder(R.drawable.ic_fish) // Плейсхолдер, пока загружается
            .error(R.drawable.ic_fish) // Изображение при ошибке загрузки
            .centerCrop()
            .into(imageView)
    }

    /**
     * Расширение для капитализации первой буквы строки
     */
    private fun String.capitalize(): String {
        return if (this.isNotEmpty()) {
            this.substring(0, 1).uppercase() + this.substring(1)
        } else {
            this
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            // Возвращаемся назад с анимацией
            AnimationHelper.finishWithAnimation(this)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        // Переопределяем стандартное поведение кнопки "Назад"
        AnimationHelper.finishWithAnimation(this)
    }
}