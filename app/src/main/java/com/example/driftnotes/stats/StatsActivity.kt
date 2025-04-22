package com.example.driftnotes.stats

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.driftnotes.R
import com.example.driftnotes.databinding.ActivityStatsBinding
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Настраиваем ActionBar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.statistics_title)

        // Загружаем данные
        loadStatistics()
    }

    /**
     * Загружает статистику рыбалок
     */
    private fun loadStatistics() {
        // Показываем индикатор загрузки
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val result = statsRepository.getFishingStats()

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
    private fun updateUI(stats: com.example.driftnotes.models.FishingStats) {
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

                // Если есть фото, загружаем его
                if (biggestFish.photoUrl.isNotEmpty()) {
                    loadImageWithGlide(biggestFish.photoUrl, binding.imageBigFish)
                }
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