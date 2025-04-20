// Путь: app/src/main/java/com/example/driftnotes/fishing/FishingNoteDetailActivity.kt
package com.example.driftnotes.fishing

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.driftnotes.R
import com.example.driftnotes.databinding.ActivityFishingNoteDetailBinding
import com.example.driftnotes.fishing.markermap.MarkerMapActivity
import com.example.driftnotes.maps.MapActivity
import com.example.driftnotes.models.BiteRecord
import com.example.driftnotes.models.FishingNote
import com.example.driftnotes.utils.AnimationHelper
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class FishingNoteDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFishingNoteDetailBinding
    private lateinit var auth: FirebaseAuth
    private val firestore = FirebaseFirestore.getInstance()
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    private var noteId: String? = null
    private var currentNote: FishingNote? = null
    private lateinit var photoAdapter: PhotoPagerAdapter

    // Для работы с поклевками
    private val biteRecords = mutableListOf<BiteRecord>()
    private lateinit var biteAdapter: BiteRecordAdapter
    private lateinit var biteChart: BarChart

    // Для многодневной рыбалки
    private var currentDayIndex: Int = 0
    private var dayCount: Int = 1
    private lateinit var daySpinner: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFishingNoteDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Получаем ID записи из интента
        noteId = intent.getStringExtra("note_id")

        if (noteId == null) {
            Toast.makeText(this, "Ошибка: запись не найдена", Toast.LENGTH_SHORT).show()
            AnimationHelper.finishWithAnimation(this)
            return
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Настраиваем адаптер для фотографий
        photoAdapter = PhotoPagerAdapter(emptyList())
        binding.viewPagerPhotos.adapter = photoAdapter

        // Настраиваем индикатор страниц
        binding.dotsIndicator.attachTo(binding.viewPagerPhotos)

        // Устанавливаем обработчик для кнопки просмотра на карте
        binding.buttonViewOnMap.setOnClickListener {
            openLocationOnMap()
        }

        // Инициализация RecyclerView для поклевок
        biteAdapter = BiteRecordAdapter(
            bites = biteRecords,
            onBiteClick = { bite -> showEditBiteDialog(bite) },
            onBiteDeleteClick = { bite -> showDeleteBiteConfirmation(bite) }
        )
        binding.recyclerViewBites.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewBites.adapter = biteAdapter

        // Получаем ссылку на BarChart
        biteChart = binding.biteChart

        // Настраиваем спиннер для выбора дня
        daySpinner = binding.spinnerDay
        daySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (currentDayIndex != position) {
                    currentDayIndex = position
                    updateBitesForSelectedDay()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Ничего не делаем
            }
        }

        // Обработчик кнопки добавления поклевки
        binding.buttonAddBite.setOnClickListener {
            currentNote?.let { note ->
                // Получаем дату для выбранного дня
                val selectedDate = getDateForDayIndex(note.date, note.endDate, currentDayIndex)
                showAddBiteDialog(selectedDate)
            }
        }

        // Обработчик редактирования снастей
        binding.buttonEditTackle.setOnClickListener {
            showEditTackleDialog()
        }

        // Обработчик редактирования заметок
        binding.buttonEditNotes.setOnClickListener {
            showEditNotesDialog()
        }

        // Загружаем данные записи
        loadNoteData()
    }

    /**
     * Получает дату для указанного индекса дня в многодневной рыбалке
     */
    private fun getDateForDayIndex(startDate: Date, endDate: Date?, dayIndex: Int): Date {
        if (endDate == null || dayIndex == 0) return startDate

        val calendar = Calendar.getInstance()
        calendar.time = startDate
        calendar.add(Calendar.DAY_OF_MONTH, dayIndex)

        // Проверяем, что не вышли за конечную дату
        return if (calendar.time.after(endDate)) endDate else calendar.time
    }

    /**
     * Обновляет список поклевок для выбранного дня
     */
    private fun updateBitesForSelectedDay() {
        currentNote?.let { note ->
            // Фильтруем поклевки по выбранному дню
            val filteredBites = note.biteRecords.filter { it.dayIndex == currentDayIndex }

            // Обновляем список
            biteRecords.clear()
            biteRecords.addAll(filteredBites)
            biteAdapter.updateBites(biteRecords)

            // Обновляем график
            updateBiteChart()

            // Обновляем UI в зависимости от наличия поклевок
            if (filteredBites.isEmpty()) {
                binding.textViewNoBites.visibility = View.VISIBLE
                binding.chartScrollView.visibility = View.GONE
                binding.recyclerViewBites.visibility = View.GONE
            } else {
                binding.textViewNoBites.visibility = View.GONE
                binding.chartScrollView.visibility = View.VISIBLE
                binding.recyclerViewBites.visibility = View.VISIBLE
            }
        }
    }

    /**
     * Показывает диалог редактирования снастей
     */
    private fun showEditTackleDialog() {
        currentNote?.let { note ->
            EditTextDialog(
                context = this,
                title = "Редактировать снасти",
                hint = "Введите информацию о снастях",
                initialText = note.tackle
            ) { newText ->
                // Обновляем текст снастей
                updateTackle(newText)
            }.show()
        }
    }

    /**
     * Показывает диалог редактирования заметок
     */
    private fun showEditNotesDialog() {
        currentNote?.let { note ->
            EditTextDialog(
                context = this,
                title = "Редактировать заметки",
                hint = "Введите заметки",
                initialText = note.notes
            ) { newText ->
                // Обновляем текст заметок
                updateNotes(newText)
            }.show()
        }
    }

    /**
     * Обновляет информацию о снастях
     */
    private fun updateTackle(newTackle: String) {
        noteId?.let { id ->
            // Обновляем отображение
            binding.textViewTackle.text = newTackle

            // Обновляем объект заметки
            currentNote = currentNote?.copy(tackle = newTackle)

            // Обновляем в Firestore
            firestore.collection("fishing_notes")
                .document(id)
                .update("tackle", newTackle)
                .addOnSuccessListener {
                    Toast.makeText(this, "Информация о снастях обновлена", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Ошибка при обновлении: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    /**
     * Обновляет заметки
     */
    private fun updateNotes(newNotes: String) {
        noteId?.let { id ->
            // Обновляем отображение
            binding.textViewNotes.text = newNotes

            // Обновляем объект заметки
            currentNote = currentNote?.copy(notes = newNotes)

            // Обновляем в Firestore
            firestore.collection("fishing_notes")
                .document(id)
                .update("notes", newNotes)
                .addOnSuccessListener {
                    Toast.makeText(this, "Заметки обновлены", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Ошибка при обновлении: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun openLocationOnMap() {
        currentNote?.let { note ->
            if (note.latitude != 0.0 && note.longitude != 0.0) {
                val intent = Intent(this, MapActivity::class.java)
                intent.putExtra("view_only", true)
                intent.putExtra("latitude", note.latitude)
                intent.putExtra("longitude", note.longitude)
                intent.putExtra("title", note.location)
                AnimationHelper.startActivityWithAnimation(this, intent)
            } else {
                Toast.makeText(this, R.string.no_location_data, Toast.LENGTH_SHORT).show()
            }
        } ?: Toast.makeText(this, R.string.no_location_data, Toast.LENGTH_SHORT).show()
    }

    private fun loadNoteData() {
        noteId?.let { id ->
            firestore.collection("fishing_notes")
                .document(id)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        currentNote = document.toObject(FishingNote::class.java)?.copy(id = id)
                        displayNoteData()
                    } else {
                        Toast.makeText(this, "Запись не найдена", Toast.LENGTH_SHORT).show()
                        AnimationHelper.finishWithAnimation(this)
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Ошибка загрузки: ${e.message}", Toast.LENGTH_SHORT).show()
                    AnimationHelper.finishWithAnimation(this)
                }
        }
    }

    private fun displayNoteData() {
        currentNote?.let { note ->
            // Отображаем тип рыбалки
            if (note.fishingType.isNotEmpty()) {
                binding.textViewFishingTypeLabel.visibility = View.VISIBLE
                binding.textViewFishingType.visibility = View.VISIBLE
                binding.textViewFishingType.text = note.fishingType
            } else {
                binding.textViewFishingTypeLabel.visibility = View.GONE
                binding.textViewFishingType.visibility = View.GONE
            }

            binding.textViewLocation.text = note.location

            // Отображаем дату или диапазон дат
            if (note.isMultiDay && note.endDate != null) {
                val startDateStr = dateFormat.format(note.date)
                val endDateStr = dateFormat.format(note.endDate)
                binding.textViewDate.text = "$startDateStr - $endDateStr"

                // Вычисляем количество дней
                val diffInMillis = note.endDate.time - note.date.time
                dayCount = (TimeUnit.MILLISECONDS.toDays(diffInMillis) + 1).toInt()

                // Настраиваем спиннер для выбора дня
                setupDaySpinner(dayCount)
                binding.spinnerDayContainer.visibility = View.VISIBLE
            } else {
                binding.textViewDate.text = dateFormat.format(note.date)
                dayCount = 1
                binding.spinnerDayContainer.visibility = View.GONE
            }

// Отображаем снасти и заметки (если они есть)
            if (note.notes.isNotEmpty()) {
                binding.textViewNotesLabel.visibility = View.VISIBLE
                binding.textViewNotes.visibility = View.VISIBLE
                binding.textViewNotes.text = note.notes
            } else {
                binding.textViewNotesLabel.visibility = View.GONE
                binding.textViewNotes.visibility = View.GONE
            }

            // Включаем/отключаем кнопку просмотра на карте в зависимости от наличия координат
            binding.buttonViewOnMap.isEnabled = note.latitude != 0.0 && note.longitude != 0.0

            // Отображаем погоду, если она доступна
            if (note.weather != null) {
                binding.textViewWeatherLabel.visibility = View.VISIBLE
                binding.textViewWeather.visibility = View.VISIBLE
                binding.textViewWeather.text = note.weather.weatherDescription
            } else {
                binding.textViewWeatherLabel.visibility = View.GONE
                binding.textViewWeather.visibility = View.GONE
            }

            // Настраиваем ViewPager для фотографий
            if (note.photoUrls.isNotEmpty()) {
                photoAdapter.updatePhotos(note.photoUrls)
                binding.viewPagerPhotos.visibility = View.VISIBLE
                binding.dotsIndicator.visibility = if (note.photoUrls.size > 1) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            } else {
                binding.viewPagerPhotos.visibility = View.GONE
                binding.dotsIndicator.visibility = View.GONE
            }

            // Отображаем кнопку просмотра маркерной карты, если она есть
            if (note.markerMapId.isNotEmpty() && note.fishingType == getString(R.string.fishing_type_carp)) {
                binding.buttonViewMarkerMap.visibility = View.VISIBLE
                binding.buttonViewMarkerMap.setOnClickListener {
                    val intent = Intent(this, MarkerMapActivity::class.java)
                    intent.putExtra(MarkerMapActivity.EXTRA_MAP_ID, note.markerMapId)
                    intent.putExtra(MarkerMapActivity.EXTRA_MAP_NAME, note.location)
                    AnimationHelper.startActivityWithAnimation(this, intent)
                }
            } else {
                binding.buttonViewMarkerMap.visibility = View.GONE
            }

            // Отображаем секцию поклевок только для карповой рыбалки
            if (note.fishingType == getString(R.string.fishing_type_carp)) {
                binding.textViewBitesLabel.visibility = View.VISIBLE
                binding.buttonAddBite.visibility = View.VISIBLE

                // Загружаем все поклевки
                biteRecords.clear()
                if (note.biteRecords.isNotEmpty()) {
                    // Если есть поклевки, показываем их для текущего дня
                    updateBitesForSelectedDay()
                } else {
                    // Если поклевок нет, показываем сообщение
                    binding.textViewNoBites.visibility = View.VISIBLE
                    binding.chartScrollView.visibility = View.GONE
                    binding.recyclerViewBites.visibility = View.GONE
                }
            } else {
                binding.textViewBitesLabel.visibility = View.GONE
                binding.buttonAddBite.visibility = View.GONE
                binding.textViewNoBites.visibility = View.GONE
                binding.chartScrollView.visibility = View.GONE
                binding.recyclerViewBites.visibility = View.GONE
                binding.spinnerDayContainer.visibility = View.GONE
            }
        }
    }

    /**
     * Настраивает спиннер для выбора дня
     */
    private fun setupDaySpinner(dayCount: Int) {
        // Создаем список дней
        val days = ArrayList<String>()
        for (i in 0 until dayCount) {
            days.add("День ${i + 1}")
        }

        // Настраиваем адаптер
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, days)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        daySpinner.adapter = adapter

        // Выбираем первый день по умолчанию
        daySpinner.setSelection(0)
        currentDayIndex = 0
    }

    /**
     * Показывает диалог добавления поклевки
     */
    private fun showAddBiteDialog(date: Date) {
        BiteDialog(this, date, null) { newBite ->
            // Создаем новую поклевку с указанием дня
            val biteWithDay = newBite.copy(dayIndex = currentDayIndex)
            // Добавляем новую поклевку
            addBiteRecord(biteWithDay)
        }.show()
    }

    /**
     * Показывает диалог редактирования поклевки
     */
    private fun showEditBiteDialog(bite: BiteRecord) {
        currentNote?.let { note ->
            // Получаем дату для текущего дня
            val dayDate = getDateForDayIndex(note.date, note.endDate, bite.dayIndex)
            BiteDialog(this, dayDate, bite) { updatedBite ->
                // Сохраняем индекс дня
                val biteWithDay = updatedBite.copy(dayIndex = bite.dayIndex)
                // Обновляем поклевку
                updateBiteRecord(biteWithDay)
            }.show()
        }
    }

    /**
     * Показывает подтверждение удаления поклевки
     */
    private fun showDeleteBiteConfirmation(bite: BiteRecord) {
        AlertDialog.Builder(this)
            .setTitle("Удаление поклевки")
            .setMessage("Вы уверены, что хотите удалить эту запись о поклевке?")
            .setPositiveButton("Удалить") { _, _ ->
                deleteBiteRecord(bite)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    /**
     * Добавляет новую запись о поклевке
     */
    private fun addBiteRecord(bite: BiteRecord) {
        currentNote?.let { note ->
            // Получаем текущий список поклевок
            val currentBites = note.biteRecords.toMutableList()

            // Добавляем новую поклевку
            currentBites.add(bite)

            // Сортируем по времени в пределах дня
            val sortedBites = currentBites.sortedWith(compareBy({ it.dayIndex }, { it.time }))

            // Обновляем список для текущего дня
            val currentDayBites = sortedBites.filter { it.dayIndex == currentDayIndex }
            biteRecords.clear()
            biteRecords.addAll(currentDayBites)

            // Обновляем адаптер
            biteAdapter.updateBites(biteRecords)

            // Обновляем график
            updateBiteChart()

            // Обновляем интерфейс
            binding.textViewNoBites.visibility = View.GONE
            binding.chartScrollView.visibility = View.VISIBLE
            binding.recyclerViewBites.visibility = View.VISIBLE

            // Обновляем объект заметки
            currentNote = note.copy(biteRecords = sortedBites)

            // Сохраняем изменения в Firestore
            saveBitesToFirestore(sortedBites)

            // Показываем сообщение
            Toast.makeText(this, "Поклевка добавлена", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Обновляет существующую запись о поклевке
     */
    private fun updateBiteRecord(bite: BiteRecord) {
        currentNote?.let { note ->
            // Получаем текущий список поклевок
            val currentBites = note.biteRecords.toMutableList()

            // Находим индекс обновляемой поклевки
            val index = currentBites.indexOfFirst { it.id == bite.id }
            if (index != -1) {
                // Обновляем поклевку в списке
                currentBites[index] = bite

                // Сортируем по времени в пределах дня
                val sortedBites = currentBites.sortedWith(compareBy({ it.dayIndex }, { it.time }))

                // Обновляем список для текущего дня
                val currentDayBites = sortedBites.filter { it.dayIndex == currentDayIndex }
                biteRecords.clear()
                biteRecords.addAll(currentDayBites)

                // Обновляем адаптер
                biteAdapter.updateBites(biteRecords)

                // Обновляем график
                updateBiteChart()

                // Обновляем объект заметки
                currentNote = note.copy(biteRecords = sortedBites)

                // Сохраняем изменения в Firestore
                saveBitesToFirestore(sortedBites)

                // Показываем сообщение
                Toast.makeText(this, "Поклевка обновлена", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Удаляет запись о поклевке
     */
    private fun deleteBiteRecord(bite: BiteRecord) {
        currentNote?.let { note ->
            // Получаем текущий список поклевок
            val currentBites = note.biteRecords.toMutableList()

            // Удаляем поклевку
            currentBites.removeAll { it.id == bite.id }

            // Обновляем список для текущего дня
            val currentDayBites = currentBites.filter { it.dayIndex == currentDayIndex }
            biteRecords.clear()
            biteRecords.addAll(currentDayBites)

            // Обновляем адаптер
            biteAdapter.updateBites(biteRecords)

            // Обновляем график
            updateBiteChart()

            // Обновляем интерфейс, если список пуст
            if (currentDayBites.isEmpty()) {
                binding.textViewNoBites.visibility = View.VISIBLE
                binding.chartScrollView.visibility = View.GONE
                binding.recyclerViewBites.visibility = View.GONE
            }

            // Обновляем объект заметки
            currentNote = note.copy(biteRecords = currentBites)

            // Сохраняем изменения в Firestore
            saveBitesToFirestore(currentBites)

            // Показываем сообщение
            Toast.makeText(this, "Поклевка удалена", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Сохраняет список поклевок в Firestore
     */
    private fun saveBitesToFirestore(bites: List<BiteRecord>) {
        noteId?.let { id ->
            // Подготавливаем данные для обновления
            val updateData = mapOf(
                "biteRecords" to bites
            )

            // Обновляем документ
            firestore.collection("fishing_notes")
                .document(id)
                .update(updateData)
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this,
                        "Ошибка при сохранении поклевок: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    /**
     * Обновляет график поклевок
     */
    private fun updateBiteChart() {
        if (biteRecords.isEmpty()) return

        // Добавляем рамку для графика
        val borderColor = ContextCompat.getColor(this, R.color.primary)
        val border = GradientDrawable()
        border.setColor(Color.WHITE)
        border.setStroke(2, borderColor)
        border.cornerRadius = 8f
        binding.biteChart.setBackground(border)

        // Уменьшаем ширину графика в два раза
        val layoutParams = binding.biteChart.layoutParams
        layoutParams.width = 1200 // Уменьшено в 2 раза с 2400px до 1200px
        binding.biteChart.layoutParams = layoutParams

        // Подготавливаем данные для графика
        val entries = ArrayList<BarEntry>()

        // Группируем поклевки по получасам (30-минутным интервалам)
        val bitesByHalfHour = mutableMapOf<Float, Int>()

        // Инициализируем все получасовые интервалы нулями (0.0, 0.5, 1.0, 1.5, ... 23.5)
        for (hour in 0..23) {
            bitesByHalfHour[hour.toFloat()] = 0
            bitesByHalfHour[hour.toFloat() + 0.5f] = 0
        }

        // Считаем поклевки по получасам
        for (bite in biteRecords) {
            val calendar = Calendar.getInstance().apply { time = bite.time }
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            // Определяем, в какой получасовой интервал попадает время поклевки
            val halfHour = if (minute < 30) hour.toFloat() else hour.toFloat() + 0.5f

            bitesByHalfHour[halfHour] = (bitesByHalfHour[halfHour] ?: 0) + 1
        }

        // Создаем записи для графика - отображаем все получасовые интервалы
        // Сортируем по времени, чтобы интервалы шли в правильном порядке
        bitesByHalfHour.entries
            .sortedBy { it.key }
            .forEach { (time, count) ->
                entries.add(BarEntry(time, count.toFloat()))
            }

        // Цвет для столбцов графика - фирменный цвет приложения
        val barColor = ContextCompat.getColor(this, R.color.primary)

        // Создаем датасет
        val dataSet = BarDataSet(entries, "Поклевки")
        dataSet.color = barColor
        dataSet.setDrawValues(true)  // Показываем значения над столбцами только для ненулевых значений
        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return if (value > 0) value.toInt().toString() else ""
            }
        }

        // Цвет текста значений
        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTextSize = 12f

        // Создаем данные графика
        val barData = BarData(dataSet)
        barData.barWidth = 0.4f  // Уменьшаем ширину столбцов для лучшей видимости

        // Настраиваем форматер для оси X (чтобы отображать время вертикально)
        val xAxis = biteChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 0.5f // Шаг для получасовых интервалов
        xAxis.labelCount = 48  // 24 часа * 2 (получасовые интервалы)
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val hour = value.toInt()
                val minute = if ((value - hour) >= 0.5f) "30" else "00"
                return "${hour}:${minute}"
            }
        }

        // Отображаем метки оси X вертикально
        xAxis.labelRotationAngle = 90f

        // Увеличиваем отступы для осей X, чтобы значения по краям не обрезались
        xAxis.setDrawAxisLine(true)
        xAxis.setDrawGridLines(false)
        xAxis.setAvoidFirstLastClipping(true)
        xAxis.spaceMin = 0.5f
        xAxis.spaceMax = 0.5f

        // Убираем значения с оси Y слева
        val leftAxis = biteChart.axisLeft
        leftAxis.axisMinimum = 0f  // Минимальное значение 0
        leftAxis.setDrawZeroLine(true)  // Рисуем линию нуля
        leftAxis.setDrawLabels(false)  // Скрываем значения слева

        // Настраиваем внешний вид графика
        biteChart.data = barData
        biteChart.description.isEnabled = false  // Убираем описание
        biteChart.legend.isEnabled = false  // Убираем легенду
        biteChart.axisRight.isEnabled = false  // Убираем правую ось Y

        // Отключаем масштабирование и скроллинг в самом графике
        // (скроллинг будет через HorizontalScrollView)
        biteChart.setScaleEnabled(false)
        biteChart.setPinchZoom(false)
        biteChart.setTouchEnabled(false)

        // Устанавливаем видимую область графика
        biteChart.setVisibleXRangeMinimum(5f)   // минимум 5 часов видимо
        biteChart.setVisibleXRangeMaximum(24f)  // максимум 24 часа видимо

        // Устанавливаем отступы для графика внутри chart view
        biteChart.setExtraOffsets(5f, 10f, 20f, 70f) // Увеличиваем нижний отступ для вертикальных меток времени

        // Анимация
        biteChart.animateY(500)

        // Обновляем график
        biteChart.invalidate()

        // Прокручиваем HorizontalScrollView к началу
        binding.chartScrollView.post {
            binding.chartScrollView.fullScroll(View.FOCUS_LEFT)

            // Затем прокручиваем к текущему времени с небольшой задержкой
            binding.chartScrollView.postDelayed({
                val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val scrollPosition = (currentHour * 50).toInt() // 50 пикселей на час, с учетом уменьшенной ширины
                binding.chartScrollView.smoothScrollTo(scrollPosition, 0)
            }, 300)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.detail_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                AnimationHelper.finishWithAnimation(this)
                true
            }
            R.id.menu_delete -> {
                confirmDelete()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle("Удаление записи")
            .setMessage("Вы уверены, что хотите удалить эту запись?")
            .setPositiveButton("Удалить") { _, _ ->
                deleteNote()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun deleteNote() {
        noteId?.let { id ->
            // Получаем ID маркерной карты (если есть)
            val markerMapId = currentNote?.markerMapId ?: ""

            // Удаляем запись
            firestore.collection("fishing_notes")
                .document(id)
                .delete()
                .addOnSuccessListener {
                    // Если есть маркерная карта, предлагаем удалить и её
                    if (markerMapId.isNotEmpty()) {
                        showDeleteMarkerMapDialog(markerMapId)
                    } else {
                        Toast.makeText(this, R.string.note_deleted, Toast.LENGTH_SHORT).show()
                        AnimationHelper.finishWithAnimation(this)
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this,
                        "Ошибка при удалении: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    /**
     * Показывает диалог подтверждения удаления маркерной карты
     */
    private fun showDeleteMarkerMapDialog(markerMapId: String) {
        AlertDialog.Builder(this)
            .setTitle("Удаление маркерной карты")
            .setMessage("Хотите также удалить маркерную карту дна?")
            .setPositiveButton("Да") { _, _ ->
                deleteMarkerMap(markerMapId)
            }
            .setNegativeButton("Нет") { _, _ ->
                Toast.makeText(this, R.string.note_deleted, Toast.LENGTH_SHORT).show()
                AnimationHelper.finishWithAnimation(this)
            }
            .show()
    }

    /**
     * Удаляет маркерную карту
     */
    private fun deleteMarkerMap(markerMapId: String) {
        try {
            // Удаляем маркеры
            firestore.collection("marker_maps")
                .document(markerMapId)
                .collection("markers")
                .get()
                .addOnSuccessListener { markersSnapshot ->
                    val batch = firestore.batch()

                    // Удаляем все маркеры
                    for (doc in markersSnapshot.documents) {
                        batch.delete(doc.reference)
                    }

                    // Получаем соединения для удаления
                    firestore.collection("marker_maps")
                        .document(markerMapId)
                        .collection("connections")
                        .get()
                        .addOnSuccessListener { connectionsSnapshot ->
                            // Удаляем все соединения
                            for (doc in connectionsSnapshot.documents) {
                                batch.delete(doc.reference)
                            }

                            // Удаляем саму карту
                            batch.delete(firestore.collection("marker_maps").document(markerMapId))

                            // Выполняем пакетную операцию
                            batch.commit()
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Запись и карта удалены", Toast.LENGTH_SHORT).show()
                                    AnimationHelper.finishWithAnimation(this)
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(
                                        this,
                                        "Ошибка при удалении карты: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    AnimationHelper.finishWithAnimation(this)
                                }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                this,
                                "Ошибка при удалении соединений: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                            AnimationHelper.finishWithAnimation(this)
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this,
                        "Ошибка при удалении маркеров: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    AnimationHelper.finishWithAnimation(this)
                }
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Ошибка при удалении карты: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
            AnimationHelper.finishWithAnimation(this)
        }
    }

    override fun onBackPressed() {
        AnimationHelper.finishWithAnimation(this)
    }
}