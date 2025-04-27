package com.example.driftnotes.fishing

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.driftnotes.R
import com.example.driftnotes.databinding.ActivityFishingNoteDetailBinding
import com.example.driftnotes.fishing.markermap.MarkerMapActivity
import com.example.driftnotes.maps.MapActivity
import com.example.driftnotes.models.BiteRecord
import com.example.driftnotes.models.FishingNote
import com.example.driftnotes.utils.AnimationHelper
import com.example.driftnotes.utils.DateFormatter
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
import android.widget.HorizontalScrollView

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

    // Для работы с несколькими графиками поклевок
    private var fishingSpots = listOf("Основная точка")
    private var currentSpotIndex: Int = 0
    private lateinit var spotSpinner: Spinner
    private var chartsContainer: LinearLayout? = null
    private var biteCharts = mutableListOf<BarChart>()

    // Константа для логирования
    private val TAG = "FishingNoteDetail"

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
        photoAdapter = PhotoPagerAdapter(
            emptyList(),
            false,
            { position -> openPhotoFullscreen(position) }
        )
        binding.viewPagerPhotos.adapter = photoAdapter

        // Устанавливаем слушателя для изменения страницы
        binding.viewPagerPhotos.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                Log.d(TAG, "Выбрана фотография на позиции: $position")
                // Принудительно обновляем текущее фото при его отображении
                photoAdapter.notifyItemChanged(position)
            }
        })

        // Настраиваем индикатор страниц
        binding.dotsIndicator.attachTo(binding.viewPagerPhotos)

        // Заменяем текстовые кнопки на иконки карандаша
        setupEditButtons()

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

        // Инициализируем контейнер для графиков
        chartsContainer = binding.chartsContainer

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

        // Настраиваем спиннер для выбора точки ловли
        spotSpinner = binding.spinnerSpot
        spotSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (currentSpotIndex != position) {
                    currentSpotIndex = position
                    updateBitesForSelectedSpot()
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
                showAddBiteDialog(selectedDate, currentSpotIndex)
            }
        }

        // Обработчик для кнопки добавления новой точки ловли
        binding.buttonAddSpot.setOnClickListener {
            showAddSpotDialog()
        }

        // Загружаем данные записи
        loadNoteData()
    }

    /**
     * Открывает фотографию в полноэкранном режиме
     */
    private fun openPhotoFullscreen(position: Int) {
        currentNote?.let { note ->
            if (note.photoUrls.isNotEmpty()) {
                // Добавляем логирование для отладки
                Log.d(TAG, "Открываем фото на позиции $position из ${note.photoUrls.size} фото")
                Log.d(TAG, "URLs фотографий: ${note.photoUrls}")

                val intent = Intent(this, FullscreenPhotoActivity::class.java)
                intent.putStringArrayListExtra("photos", ArrayList(note.photoUrls))
                intent.putExtra("position", position)
                startActivity(intent)
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            }
        }
    }

    /**
     * Настраивает кнопки редактирования снастей и заметок
     */
    private fun setupEditButtons() {
        // Скрываем текстовые кнопки
        binding.buttonEditTackle.visibility = View.GONE
        binding.buttonEditNotes.visibility = View.GONE

        // Показываем иконки карандаша и настраиваем их слушателей
        binding.btnEditTackle.visibility = View.VISIBLE
        binding.btnEditNotes.visibility = View.VISIBLE

        binding.btnEditTackle.setOnClickListener {
            showEditTackleDialog()
        }

        binding.btnEditNotes.setOnClickListener {
            showEditNotesDialog()
        }
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
     * Обновляет список поклевок для выбранной точки ловли
     */
    private fun updateBitesForSelectedSpot() {
        updateBitesForSelectedDay() // Переиспользуем тот же код, так как он фильтрует и по дню, и по точке
    }

    /**
     * Обновляет список поклевок для выбранного дня
     */
    private fun updateBitesForSelectedDay() {
        currentNote?.let { note ->
            // Фильтруем поклевки по выбранному дню и точке ловли
            val filteredBites = note.biteRecords.filter {
                it.dayIndex == currentDayIndex && it.spotIndex == currentSpotIndex
            }

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
     * Показывает диалог добавления новой точки ловли
     */
    private fun showAddSpotDialog() {
        EditTextDialog(
            context = this,
            title = "Добавить точку ловли",
            hint = "Введите название точки",
            initialText = ""
        ) { newSpotName ->
            if (newSpotName.isNotEmpty()) {
                addNewFishingSpot(newSpotName)
            }
        }.show()
    }
    /**
     * Добавляет новую точку ловли
     */
    private fun addNewFishingSpot(spotName: String) {
        // Добавляем новую точку ловли в список
        val updatedSpots = fishingSpots.toMutableList()
        updatedSpots.add(spotName)
        fishingSpots = updatedSpots.toList()

        // Обновляем спиннер
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, fishingSpots)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spotSpinner.adapter = adapter

        // Выбираем новую точку
        spotSpinner.setSelection(fishingSpots.size - 1)
        currentSpotIndex = fishingSpots.size - 1

        // Сохраняем изменения в Firestore
        currentNote?.let { note ->
            noteId?.let { id ->
                val updatedNote = note.copy(fishingSpots = fishingSpots)
                currentNote = updatedNote

                firestore.collection("fishing_notes")
                    .document(id)
                    .update("fishingSpots", fishingSpots)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Точка ловли добавлена", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Ошибка при сохранении: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
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
            Log.d(TAG, "Загрузка заметки с ID: $id")

            firestore.collection("fishing_notes")
                .document(id)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        try {
                            currentNote = document.toObject(FishingNote::class.java)?.copy(id = id)

                            // Добавляем отладочную информацию о датах
                            Log.d(TAG, "Заметка загружена успешно: ${currentNote?.location}")
                            Log.d(TAG, "Дата начала: ${currentNote?.date}")
                            Log.d(TAG, "Дата окончания: ${currentNote?.endDate}")
                            Log.d(TAG, "Многодневная рыбалка: ${currentNote?.isMultiDay}")
                            Log.d(TAG, "Фотографии в заметке: ${currentNote?.photoUrls?.size}")

                            displayNoteData()
                        } catch (e: Exception) {
                            Log.e(TAG, "Ошибка при парсинге заметки: ${e.message}", e)
                            Toast.makeText(this, "Ошибка при загрузке заметки: ${e.message}", Toast.LENGTH_SHORT).show()
                            AnimationHelper.finishWithAnimation(this)
                        }
                    } else {
                        Log.e(TAG, "Заметка не найдена")
                        Toast.makeText(this, "Запись не найдена", Toast.LENGTH_SHORT).show()
                        AnimationHelper.finishWithAnimation(this)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Ошибка при загрузке заметки: ${e.message}", e)
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

            // Отображаем дату или диапазон дат в новом формате
            if (note.isMultiDay && note.endDate != null) {
                // Для многодневной рыбалки отображаем диапазон в формате "дд.мм.гггг — дд.мм.гггг"
                val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                val startDateStr = dateFormat.format(note.date)
                val endDateStr = dateFormat.format(note.endDate)
                binding.textViewDate.text = "$startDateStr — $endDateStr"
            } else {
                // Для однодневной рыбалки отображаем просто дату в формате "дд.мм.гггг"
                val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                binding.textViewDate.text = dateFormat.format(note.date)
            }

            // Отображаем снасти и заметки (если они есть)
            if (note.notes.isNotEmpty()) {
                binding.textViewNotesLabel.visibility = View.VISIBLE
                binding.textViewNotes.visibility = View.VISIBLE
                binding.textViewNotes.text = note.notes
                binding.btnEditNotes.visibility = View.VISIBLE
            } else {
                binding.textViewNotesLabel.visibility = View.GONE
                binding.textViewNotes.visibility = View.GONE
                binding.btnEditNotes.visibility = View.GONE
            }

            // Отображаем снасти (если они есть)
            if (note.tackle.isNotEmpty()) {
                binding.textViewTackleLabel.visibility = View.VISIBLE
                binding.textViewTackle.visibility = View.VISIBLE
                binding.textViewTackle.text = note.tackle
                binding.btnEditTackle.visibility = View.VISIBLE
            } else {
                binding.textViewTackleLabel.visibility = View.GONE
                binding.textViewTackle.visibility = View.GONE
                binding.btnEditTackle.visibility = View.GONE
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
                Log.d(TAG, "Количество фотографий: ${note.photoUrls.size}")
                for (url in note.photoUrls) {
                    Log.d(TAG, "URL фото: $url")
                }

                try {
                    // Обновляем адаптер с новыми фотографиями
                    photoAdapter = PhotoPagerAdapter(
                        note.photoUrls,
                        false,
                        { position -> openPhotoFullscreen(position) }
                    )
                    binding.viewPagerPhotos.adapter = photoAdapter

                    // Перезагружаем индикатор
                    binding.dotsIndicator.attachTo(binding.viewPagerPhotos)

                    // Показываем ViewPager
                    binding.viewPagerPhotos.visibility = View.VISIBLE
                    binding.dotsIndicator.visibility = if (note.photoUrls.size > 1) View.VISIBLE else View.GONE
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка при настройке отображения фотографий", e)
                    binding.viewPagerPhotos.visibility = View.GONE
                    binding.dotsIndicator.visibility = View.GONE
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

            // Загружаем список точек ловли
            fishingSpots = if (note.fishingSpots.isNotEmpty()) {
                note.fishingSpots
            } else {
                listOf("Основная точка")
            }

            // Настраиваем спиннер для выбора точки ловли
            setupSpotSpinner()

            // Отображаем секцию поклевок только для карповой рыбалки
            if (note.fishingType == getString(R.string.fishing_type_carp)) {
                binding.textViewBitesLabel.visibility = View.VISIBLE
                binding.buttonAddBite.visibility = View.VISIBLE
                binding.spinnerSpotContainer.visibility = View.VISIBLE
                binding.buttonAddBite.visibility = View.VISIBLE

                // Загружаем все поклевки
                biteRecords.clear()
                if (note.biteRecords.isNotEmpty()) {
                    // Если есть поклевки, показываем их для текущего дня и точки
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
                binding.spinnerSpotContainer.visibility = View.GONE
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
     * Настраивает спиннер для выбора точки ловли
     */
    private fun setupSpotSpinner() {
        // Настраиваем адаптер
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, fishingSpots)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spotSpinner.adapter = adapter

        // Выбираем первую точку по умолчанию
        spotSpinner.setSelection(0)
        currentSpotIndex = 0
    }

    /**
     * Показывает диалог добавления поклевки
     */
    private fun showAddBiteDialog(date: Date, spotIndex: Int = 0) {
        BiteDialog(this, date, null, spotIndex) { newBite ->
            // Создаем новую поклевку с указанием дня и точки
            val biteWithDayAndSpot = newBite.copy(dayIndex = currentDayIndex, spotIndex = spotIndex)
            // Добавляем новую поклевку
            addBiteRecord(biteWithDayAndSpot)
        }.show()
    }

    /**
     * Показывает диалог редактирования поклевки
     */
    private fun showEditBiteDialog(bite: BiteRecord) {
        currentNote?.let { note ->
            // Получаем дату для текущего дня
            val dayDate = getDateForDayIndex(note.date, note.endDate, bite.dayIndex)
            BiteDialog(this, dayDate, bite, bite.spotIndex) { updatedBite ->
                // Сохраняем индекс дня и точки
                val biteWithDayAndSpot = updatedBite.copy(
                    dayIndex = bite.dayIndex,
                    spotIndex = bite.spotIndex
                )
                // Обновляем поклевку
                updateBiteRecord(biteWithDayAndSpot)
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

            // Сортируем по времени в пределах дня и точки
            val sortedBites = currentBites.sortedWith(
                compareBy({ it.dayIndex }, { it.spotIndex }, { it.time })
            )

            // Обновляем список для текущего дня и точки
            val filteredBites = sortedBites.filter {
                it.dayIndex == currentDayIndex && it.spotIndex == currentSpotIndex
            }
            biteRecords.clear()
            biteRecords.addAll(filteredBites)

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

                // Сортируем по времени в пределах дня и точки
                val sortedBites = currentBites.sortedWith(
                    compareBy({ it.dayIndex }, { it.spotIndex }, { it.time })
                )

                // Обновляем список для текущего дня и точки
                val filteredBites = sortedBites.filter {
                    it.dayIndex == currentDayIndex && it.spotIndex == currentSpotIndex
                }
                biteRecords.clear()
                biteRecords.addAll(filteredBites)

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

            // Обновляем список для текущего дня и точки
            val filteredBites = currentBites.filter {
                it.dayIndex == currentDayIndex && it.spotIndex == currentSpotIndex
            }
            biteRecords.clear()
            biteRecords.addAll(filteredBites)

            // Обновляем адаптер
            biteAdapter.updateBites(biteRecords)

            // Обновляем график
            updateBiteChart()

            // Обновляем интерфейс, если список пуст
            if (filteredBites.isEmpty()) {
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
    private fun updateBiteChart() {
        if (biteRecords.isEmpty()) {
            binding.chartScrollView.visibility = View.GONE
            binding.chartTitle.visibility = View.GONE
            return
        }

        binding.chartScrollView.visibility = View.VISIBLE
        binding.chartTitle.visibility = View.VISIBLE

        // Добавляем рамку для графика
        val borderColor = ContextCompat.getColor(this, R.color.primary)
        val border = GradientDrawable()
        border.setColor(Color.BLACK) // Используем чёрный фон для тёмной темы
        border.setStroke(2, borderColor)
        border.cornerRadius = 8f
        binding.biteChart.background = border

        // Настраиваем размеры графика - ИСПРАВЛЕНО: отцентрировано и не сдвинуто влево
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels

        val layoutParams = binding.biteChart.layoutParams
        // Делаем график шириной 1.6 экрана для лучшего отображения и центрирования
        layoutParams.width = (screenWidth * 1.6).toInt()
        binding.biteChart.layoutParams = layoutParams

        // Подготавливаем данные для графика
        val entries = ArrayList<BarEntry>()

        // Группируем поклевки по получасам
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

        // Создаем записи для графика
        bitesByHalfHour.entries
            .sortedBy { it.key }
            .forEach { (time, count) ->
                if (count > 0) { // Добавляем только интервалы с поклёвками
                    entries.add(BarEntry(time, count.toFloat()))
                }
            }

        // Цвет для столбцов - используем основной цвет приложения
        val barColor = ContextCompat.getColor(this, R.color.primary)

        // Создаем датасет
        val dataSet = BarDataSet(entries, "Поклевки")
        dataSet.color = barColor
        dataSet.valueTextColor = Color.WHITE // Белый текст для тёмной темы
        dataSet.valueTextSize = 12f
        dataSet.setDrawValues(true)

        // Настройка отображения значений
        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return if (value > 0) value.toInt().toString() else ""
            }
        }

        // Создаем данные графика
        val barData = BarData(dataSet)
        barData.barWidth = 0.4f // Уже полосы для лучшей читаемости

        // Настраиваем ось X (время)
        val xAxis = binding.biteChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM // Метки внизу
        xAxis.textColor = Color.WHITE // Белый цвет текста для тёмной темы
        xAxis.granularity = 0.5f // Шаг для получасовых интервалов
        xAxis.setDrawGridLines(true)
        xAxis.gridColor = Color.DKGRAY // Тёмно-серая сетка для тёмной темы
        xAxis.axisLineColor = Color.WHITE
        xAxis.axisLineWidth = 1.5f
        // Форматирование меток времени
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                if (value < 0f || value > 24f) return ""
                val hour = value.toInt()
                val minute = if ((value - hour) >= 0.5f) "30" else "00"
                return String.format("%02d:%s", hour, minute)
            }
        }
        xAxis.labelRotationAngle = 90f // Вертикальные метки для лучшей читаемости

        // Настраиваем ось Y (количество поклёвок)
        val leftAxis = binding.biteChart.axisLeft
        leftAxis.textColor = Color.WHITE // Белый цвет текста для тёмной темы
        leftAxis.setDrawGridLines(true)
        leftAxis.gridColor = Color.DKGRAY // Тёмно-серая сетка
        leftAxis.axisLineColor = Color.WHITE
        leftAxis.axisMinimum = 0f

        // Находим максимальное значение для масштаба оси Y
        val maxValue = bitesByHalfHour.values.maxOrNull() ?: 0
        leftAxis.axisMaximum = (maxValue * 1.2f).coerceAtLeast(1f) // Минимум 1, максимум на 20% больше

        // Устанавливаем метки только на целые значения
        leftAxis.granularity = 1f
        leftAxis.isGranularityEnabled = true

        // Отключаем правую ось Y
        binding.biteChart.axisRight.isEnabled = false

        // Общие настройки графика
        binding.biteChart.setDrawBorders(false)
        binding.biteChart.description.isEnabled = false
        binding.biteChart.legend.isEnabled = false
        binding.biteChart.setTouchEnabled(false)
        binding.biteChart.setScaleEnabled(false)
        binding.biteChart.data = barData

        // Обновляем название графика
        val spotName = fishingSpots[currentSpotIndex]
        val title = if (dayCount > 1) {
            "День ${currentDayIndex + 1}, $spotName"
        } else {
            spotName
        }
        binding.chartTitle.text = title

        // Обновляем график с анимацией
        binding.biteChart.animateY(500)
        binding.biteChart.invalidate()

        // Центрируем график по умолчанию
        binding.chartScrollView.post {
            // Находим первый час с поклёвками или используем текущее время
            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val firstBiteHour = bitesByHalfHour.entries
                .filter { it.value > 0 }
                .minByOrNull { it.key }?.key?.toInt() ?: currentHour

            // Вычисляем позицию для скролла, чтобы показать первую поклевку в начале экрана
            val totalWidth = layoutParams.width
            val hourWidth = totalWidth / 24.0

            // Находим центр скроллвью
            val scrollCenter = screenWidth / 2

            // Позиция прокрутки должна быть такой, чтобы столбец с первой поклевкой был в центре
            val scrollPosition = (firstBiteHour * hourWidth - scrollCenter + hourWidth/2).toInt()

            // Ограничиваем scrollPosition, чтобы она не выходила за границы
            val clampedScrollPosition = scrollPosition.coerceIn(0, totalWidth - screenWidth)

            // Плавный скролл к рассчитанной позиции
            binding.chartScrollView.smoothScrollTo(clampedScrollPosition.coerceAtLeast(0), 0)
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