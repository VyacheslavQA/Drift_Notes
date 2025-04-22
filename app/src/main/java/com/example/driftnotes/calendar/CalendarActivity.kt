package com.example.driftnotes.calendar

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.driftnotes.R
import com.example.driftnotes.databinding.ActivityCalendarBinding
import com.example.driftnotes.fishing.FishingNoteDetailActivity
import com.example.driftnotes.models.CalendarDay
import com.example.driftnotes.repository.CalendarRepository
import com.example.driftnotes.utils.AnimationHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CalendarActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalendarBinding
    private val calendarRepository = CalendarRepository()
    private val TAG = "CalendarActivity"

    // Текущий отображаемый месяц и год
    private var currentYear: Int = 0
    private var currentMonth: Int = 0

    // Словарь с данными календаря
    private var calendarData: Map<Int, CalendarDay> = mapOf()

    // Форматтер для отображения месяца и года
    private val monthYearFormatter = SimpleDateFormat("MMMM yyyy", Locale("ru"))

    // Календарь для вычислений дат
    private val calendar = Calendar.getInstance()

    // День, который сейчас выбран
    private var selectedDay: Int = -1

    // Сегодняшний день, месяц и год
    private val todayCalendar = Calendar.getInstance()
    private val todayDay = todayCalendar.get(Calendar.DAY_OF_MONTH)
    private val todayMonth = todayCalendar.get(Calendar.MONTH)
    private val todayYear = todayCalendar.get(Calendar.YEAR)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCalendarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // УБИРАЕМ эту строку, которая вызывает ошибку
        // setSupportActionBar(binding.calendarToolbar)

        // Просто настраиваем заголовок в Toolbar и кнопку "Назад"
        binding.calendarToolbar.title = getString(R.string.calendar_title)
        binding.calendarToolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        binding.calendarToolbar.setNavigationOnClickListener {
            AnimationHelper.finishWithAnimation(this)
        }

        // Инициализация текущего месяца и года
        calendar.time = Date()
        currentYear = calendar.get(Calendar.YEAR)
        currentMonth = calendar.get(Calendar.MONTH)

        // Настройка слушателей на кнопках
        setupButtons()

        // Обновляем отображение календаря
        updateCalendarView()
    }

    /**
     * Настраивает слушателей для кнопок
     */
    private fun setupButtons() {
        // Кнопка предыдущего месяца
        binding.buttonPrevMonth.setOnClickListener {
            goToPreviousMonth()
        }

        // Кнопка следующего месяца
        binding.buttonNextMonth.setOnClickListener {
            goToNextMonth()
        }

        // Кнопка добавления запланированной рыбалки
        binding.buttonAddPlannedTrip.setOnClickListener {
            showPlannedTripDialog()
        }
    }

    /**
     * Переход к предыдущему месяцу
     */
    private fun goToPreviousMonth() {
        if (currentMonth == 0) {
            currentMonth = 11
            currentYear--
        } else {
            currentMonth--
        }

        updateCalendarView()
    }

    /**
     * Переход к следующему месяцу
     */
    private fun goToNextMonth() {
        if (currentMonth == 11) {
            currentMonth = 0
            currentYear++
        } else {
            currentMonth++
        }

        updateCalendarView()
    }

    /**
     * Обновляет отображение календаря
     */
    private fun updateCalendarView() {
        binding.progressBar.visibility = View.VISIBLE

        // Устанавливаем заголовок с месяцем и годом
        calendar.set(currentYear, currentMonth, 1)
        val monthYearText = monthYearFormatter.format(calendar.time)
        binding.textViewMonthYear.text = monthYearText.capitalizeFirstLetter()

        // Загружаем данные для календаря
        lifecycleScope.launch {
            try {
                val result = calendarRepository.getCalendarData(currentYear, currentMonth)

                if (result.isSuccess) {
                    calendarData = result.getOrDefault(mapOf())
                    Log.d(TAG, "Загружено ${calendarData.size} дней для календаря")
                    renderCalendar()
                } else {
                    val exception = result.exceptionOrNull()
                    Log.e(TAG, "Ошибка при загрузке данных календаря: ${exception?.message}")
                    Toast.makeText(
                        this@CalendarActivity,
                        "Ошибка загрузки: ${exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Исключение при загрузке данных календаря: ${e.message}", e)
                Toast.makeText(
                    this@CalendarActivity,
                    "Ошибка: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    /**
     * Создает сетку календаря и отображает данные
     */
    private fun renderCalendar() {
        // Очищаем сетку календаря
        binding.calendarGrid.removeAllViews()

        // Устанавливаем первый день месяца
        calendar.set(currentYear, currentMonth, 1)

        // Определяем день недели для первого дня месяца (0 - воскресенье, 1 - понедельник, и т.д.)
        var dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        // Преобразуем к нашему формату (понедельник - первый день)
        dayOfWeek = if (dayOfWeek == Calendar.SUNDAY) 7 else dayOfWeek - 1

        // Количество дней в месяце
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        Log.d(TAG, "Рендеринг календаря: $currentYear-$currentMonth, дней: $daysInMonth, первый день: $dayOfWeek")

        // Добавляем пустые ячейки перед первым днем месяца
        for (i in 1 until dayOfWeek) {
            addEmptyDay()
        }

        // Добавляем дни месяца
        for (day in 1..daysInMonth) {
            addDay(day)
        }

        // Определяем, сколько ячеек уже добавлено
        val cellsAdded = dayOfWeek - 1 + daysInMonth

        // Добавляем оставшиеся пустые ячейки, чтобы заполнить сетку
        val totalCells = 6 * 7 // 6 недель по 7 дней
        for (i in cellsAdded until totalCells) {
            addEmptyDay()
        }
    }

    /**
     * Добавляет пустую ячейку в календарь
     */
    private fun addEmptyDay() {
        val emptyDay = TextView(this)
        emptyDay.text = ""
        val params = GridLayout.LayoutParams()
        params.width = 0
        params.height = 0
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f)
        params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f)
        params.setMargins(4, 4, 4, 4)
        binding.calendarGrid.addView(emptyDay, params)
    }

    /**
     * Добавляет день в календарь
     */
    private fun addDay(day: Int) {
        // Создаем ячейку для дня
        val dayView = layoutInflater.inflate(R.layout.item_calendar_day, null) as FrameLayout

        // Получаем контейнер дня и текстовое поле
        val dayContainer = dayView.findViewById<ConstraintLayout>(R.id.dayContainer)
        val textViewDay = dayView.findViewById<TextView>(R.id.textViewDay)

        // Устанавливаем номер дня
        textViewDay.text = day.toString()

        // Получаем данные для этого дня
        val calendarDay = calendarData[day]

        // Определяем, является ли этот день сегодняшним
        val isToday = currentYear == todayYear && currentMonth == todayMonth && day == todayDay

        // Устанавливаем фон для ячейки
        val backgroundRes = when {
            day == selectedDay -> R.drawable.calendar_selected_day_background
            isToday -> R.drawable.calendar_today_background
            else -> R.drawable.calendar_day_background
        }
        dayContainer.background = ContextCompat.getDrawable(this, backgroundRes)

        // Если это выходной, меняем цвет текста
        val dayOfWeek = getDayOfWeek(day)
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            textViewDay.setTextColor(Color.parseColor("#FF8F00"))
        }

        // Если у нас есть данные для этого дня, показываем маркеры
        if (calendarDay != null) {
            // Маркер для прошедшей рыбалки
            val pastFishingMarker = dayView.findViewById<View>(R.id.markerPastFishing)
            pastFishingMarker.visibility = if (calendarDay.hasFishingNote) View.VISIBLE else View.GONE

            // Маркер для запланированной рыбалки
            val plannedFishingMarker = dayView.findViewById<View>(R.id.markerPlannedFishing)
            plannedFishingMarker.visibility = if (calendarDay.hasPlannedTrip) View.VISIBLE else View.GONE

            // Маркер для хорошего клёва (если рейтинг 4 или 5)
            val goodBiteMarker = dayView.findViewById<View>(R.id.markerGoodBite)
            goodBiteMarker.visibility = if (calendarDay.hasBiteForecast && calendarDay.biteRating >= 4) View.VISIBLE else View.GONE
        }

        // Устанавливаем обработчик нажатия
        dayView.setOnClickListener {
            onDayClick(day)
        }

        // Настраиваем параметры макета
        val params = GridLayout.LayoutParams()
        params.width = 0
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f)
        params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f)
        params.setMargins(4, 4, 4, 4)

        // Добавляем в сетку календаря
        binding.calendarGrid.addView(dayView, params)
    }

    /**
     * Определяет день недели для конкретного дня
     */
    private fun getDayOfWeek(day: Int): Int {
        calendar.set(currentYear, currentMonth, day)
        return calendar.get(Calendar.DAY_OF_WEEK)
    }

    /**
     * Обработчик нажатия на день календаря
     */
    private fun onDayClick(day: Int) {
        selectedDay = day

        // Перерисовываем календарь, чтобы обновить выделение
        renderCalendar()

        // Получаем данные для выбранного дня
        val calendarDay = calendarData[day]

        if (calendarDay != null) {
            showDayDetails(calendarDay)
        } else {
            // Просто показываем месяц и день
            calendar.set(currentYear, currentMonth, day)
            val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale("ru"))
            val dateString = dateFormat.format(calendar.time)

            Toast.makeText(this, "Выбрано: $dateString", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Показывает детали для выбранного дня
     */
    private fun showDayDetails(calendarDay: CalendarDay) {
        val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale("ru"))
        val dateString = dateFormat.format(calendarDay.date)

        val hasEvents = calendarDay.hasFishingNote || calendarDay.hasPlannedTrip || calendarDay.hasBiteForecast

        if (hasEvents) {
            // Создаем диалог с деталями и действиями
            val options = mutableListOf<String>()

            if (calendarDay.hasFishingNote) {
                options.add("Просмотреть заметку о рыбалке")
            }

            if (calendarDay.hasPlannedTrip) {
                options.add("Просмотреть запланированную рыбалку")
                options.add("Редактировать план")
                options.add("Удалить запланированную рыбалку")
            }

            if (calendarDay.hasBiteForecast && calendarDay.biteRating > 0) {
                options.add("Прогноз клёва: ${calendarDay.biteRating} из 5")
            }

            // Если нет опций, просто показываем дату
            if (options.isEmpty()) {
                Toast.makeText(this, "Выбрано: $dateString", Toast.LENGTH_SHORT).show()
                return
            }

            val optionsArray = options.toTypedArray()

            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(dateString)
                .setItems(optionsArray) { _, which ->
                    handleDayOptionSelected(calendarDay, options[which])
                }
                .show()
        } else {
            // Если нет событий, предлагаем запланировать рыбалку
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(dateString)
                .setMessage("Нет событий на этот день")
                .setPositiveButton("Запланировать рыбалку") { _, _ ->
                    showPlannedTripDialog(calendarDay.date)
                }
                .setNegativeButton("Отмена", null)
                .show()
        }
    }

    /**
     * Обрабатывает выбор опции из диалога дня
     */
    private fun handleDayOptionSelected(calendarDay: CalendarDay, option: String) {
        when {
            option.startsWith("Просмотреть заметку") -> {
                // Открываем детали заметки о рыбалке
                if (calendarDay.fishingNoteId.isNotEmpty()) {
                    val intent = Intent(this, FishingNoteDetailActivity::class.java)
                    intent.putExtra("note_id", calendarDay.fishingNoteId)
                    AnimationHelper.startActivityWithAnimation(this, intent)
                }
            }
            option.startsWith("Просмотреть запланированную") -> {
                // Показываем детали запланированной рыбалки
                if (calendarDay.plannedTripId.isNotEmpty()) {
                    showPlannedTripDetails(calendarDay.plannedTripId)
                }
            }
            option.startsWith("Редактировать план") -> {
                // Открываем диалог редактирования плана
                if (calendarDay.plannedTripId.isNotEmpty()) {
                    editPlannedTrip(calendarDay.plannedTripId)
                }
            }
            option.startsWith("Удалить запланированную") -> {
                // Показываем подтверждение удаления
                if (calendarDay.plannedTripId.isNotEmpty()) {
                    confirmDeletePlannedTrip(calendarDay.plannedTripId)
                }
            }
            option.startsWith("Прогноз клёва") -> {
                // Показываем информацию о прогнозе клёва
                val rating = calendarDay.biteRating
                val message = when (rating) {
                    5 -> "Исключительные условия для клёва. Рыба очень активна."
                    4 -> "Хорошие условия для клёва. Рыба активна."
                    3 -> "Средние условия для клёва. Умеренная активность."
                    2 -> "Ниже среднего. Клёв слабый."
                    1 -> "Плохие условия для клёва. Рыба малоактивна."
                    else -> "Нет данных о прогнозе клёва."
                }

                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Прогноз клёва: $rating из 5")
                    .setMessage(message)
                    .setPositiveButton("ОК", null)
                    .show()
            }
        }
    }

    /**
     * Показывает диалог с деталями запланированной рыбалки
     */
    private fun showPlannedTripDetails(plannedTripId: String) {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val result = calendarRepository.getPlannedTripById(plannedTripId)

                if (result.isSuccess) {
                    val trip = result.getOrNull()

                    if (trip != null) {
                        // Форматируем дату
                        val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale("ru"))
                        val dateText = if (trip.isMultiDay && trip.endDate != null) {
                            "${dateFormat.format(trip.date)} - ${dateFormat.format(trip.endDate)}"
                        } else {
                            dateFormat.format(trip.date)
                        }

                        androidx.appcompat.app.AlertDialog.Builder(this@CalendarActivity)
                            .setTitle("Запланированная рыбалка")
                            .setMessage(
                                "Дата: $dateText\n" +
                                        "Место: ${trip.location}\n" +
                                        (if (trip.fishingType.isNotEmpty()) "Тип: ${trip.fishingType}\n" else "") +
                                        (if (trip.note.isNotEmpty()) "Заметка: ${trip.note}" else "")
                            )
                            .setPositiveButton("Редактировать") { _, _ ->
                                editPlannedTrip(plannedTripId)
                            }
                            .setNegativeButton("Закрыть", null)
                            .setNeutralButton("Удалить") { _, _ ->
                                confirmDeletePlannedTrip(plannedTripId)
                            }
                            .show()
                    } else {
                        Toast.makeText(
                            this@CalendarActivity,
                            "Запланированная рыбалка не найдена",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    val exception = result.exceptionOrNull()
                    Toast.makeText(
                        this@CalendarActivity,
                        "Ошибка: ${exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@CalendarActivity,
                    "Ошибка: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    /**
     * Открывает диалог редактирования запланированной рыбалки
     */
    private fun editPlannedTrip(plannedTripId: String) {
        // Загружаем данные запланированной рыбалки
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val result = calendarRepository.getPlannedTripById(plannedTripId)

                if (result.isSuccess) {
                    val trip = result.getOrNull()

                    if (trip != null) {
                        // Показываем диалог редактирования с предварительно заполненными данными
                        val dialog = PlannedTripDialog(
                            context = this@CalendarActivity,
                            initialTrip = trip
                        ) { updatedTrip ->
                            // Сохраняем обновленную запланированную рыбалку
                            savePlannedTrip(updatedTrip)
                        }
                        dialog.show()
                    } else {
                        Toast.makeText(
                            this@CalendarActivity,
                            "Запланированная рыбалка не найдена",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    val exception = result.exceptionOrNull()
                    Toast.makeText(
                        this@CalendarActivity,
                        "Ошибка: ${exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@CalendarActivity,
                    "Ошибка: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    /**
     * Показывает диалог подтверждения удаления запланированной рыбалки
     */
    private fun confirmDeletePlannedTrip(plannedTripId: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Удаление")
            .setMessage("Вы уверены, что хотите удалить эту запланированную рыбалку?")
            .setPositiveButton("Удалить") { _, _ ->
                deletePlannedTrip(plannedTripId)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    /**
     * Удаляет запланированную рыбалку
     */
    private fun deletePlannedTrip(plannedTripId: String) {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val result = calendarRepository.deletePlannedTrip(plannedTripId)

                if (result.isSuccess) {
                    Toast.makeText(
                        this@CalendarActivity,
                        "Запланированная рыбалка удалена",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Обновляем календарь
                    updateCalendarView()
                } else {
                    val exception = result.exceptionOrNull()
                    Toast.makeText(
                        this@CalendarActivity,
                        "Ошибка: ${exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@CalendarActivity,
                    "Ошибка: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    /**
     * Показывает диалог добавления запланированной рыбалки
     */
    private fun showPlannedTripDialog(initialDate: Date? = null) {
        // Если дата не указана, используем выбранный день или сегодня
        val date = when {
            initialDate != null -> initialDate
            selectedDay > 0 -> {
                calendar.set(currentYear, currentMonth, selectedDay)
                calendar.time
            }
            else -> Date()
        }

        val dialog = PlannedTripDialog(
            context = this,
            initialDate = date
        ) { trip ->
            savePlannedTrip(trip)
        }
        dialog.show()
    }

    /**
     * Сохраняет запланированную рыбалку
     */
    private fun savePlannedTrip(trip: com.example.driftnotes.models.PlannedTrip) {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val result = if (trip.id.isEmpty()) {
                    // Добавляем новый план
                    calendarRepository.addPlannedTrip(trip)
                } else {
                    // Обновляем существующий план
                    calendarRepository.updatePlannedTrip(trip).map { trip.id }
                }

                if (result.isSuccess) {
                    val action = if (trip.id.isEmpty()) "добавлена" else "обновлена"
                    Toast.makeText(
                        this@CalendarActivity,
                        "Запланированная рыбалка $action",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Обновляем календарь
                    updateCalendarView()

                    // Если это был новый план, переходим к месяцу плана
                    if (trip.id.isEmpty()) {
                        calendar.time = trip.date
                        val tripYear = calendar.get(Calendar.YEAR)
                        val tripMonth = calendar.get(Calendar.MONTH)

                        if (tripYear != currentYear || tripMonth != currentMonth) {
                            currentYear = tripYear
                            currentMonth = tripMonth
                            updateCalendarView()
                        }
                    }
                } else {
                    val exception = result.exceptionOrNull()
                    Toast.makeText(
                        this@CalendarActivity,
                        "Ошибка: ${exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@CalendarActivity,
                    "Ошибка: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            AnimationHelper.finishWithAnimation(this)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        AnimationHelper.finishWithAnimation(this)
    }

    /**
     * Делает первую букву строки заглавной (для русского языка)
     */
    private fun String.capitalizeFirstLetter(): String {
        if (this.isEmpty()) return this
        return this.substring(0, 1).uppercase() + this.substring(1)
    }
}