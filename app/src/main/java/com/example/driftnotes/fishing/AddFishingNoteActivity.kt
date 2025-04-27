package com.example.driftnotes.fishing

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.driftnotes.R
import com.example.driftnotes.databinding.ActivityAddFishingNoteBinding
import com.example.driftnotes.fishing.markermap.MarkerMapActivity
import com.example.driftnotes.maps.MapActivity
import com.example.driftnotes.models.FishingNote
import com.example.driftnotes.models.FishingWeather
import com.example.driftnotes.repository.WeatherRepository
import com.example.driftnotes.utils.AnimationHelper
import com.example.driftnotes.utils.FirebaseManager
import com.example.driftnotes.utils.NetworkUtils
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.TimeUnit
import android.content.DialogInterface
import android.view.ContextThemeWrapper

class AddFishingNoteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddFishingNoteBinding
    private lateinit var auth: FirebaseAuth
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val weatherRepository = WeatherRepository()

    // UI элементы
    private lateinit var formContent: ConstraintLayout
    private lateinit var buttonInitialCancel: Button
    private lateinit var dropdownFishingType: AutoCompleteTextView
    private lateinit var formContentTitle: TextView
    private lateinit var textViewSelectedCoordinates: TextView
    private lateinit var editTextFishingDates: TextInputEditText
    private lateinit var buttonAddPhoto: Button
    private lateinit var buttonSave: Button
    private lateinit var buttonCancel: Button
    private lateinit var buttonOpenMap: Button
    private lateinit var buttonOpenMarkerMap: Button
    private lateinit var buttonLoadWeather: Button
    private lateinit var progressBarWeather: ProgressBar
    private lateinit var textViewWeatherStatus: TextView
    private lateinit var editTextLocation: TextInputEditText
    private lateinit var editTextTackle: TextInputEditText
    private lateinit var editTextNotes: TextInputEditText
    private lateinit var textViewPhotoCount: TextView
    private lateinit var mainProgressBar: ProgressBar

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    private val selectedPhotos = mutableListOf<Uri>()
    private var currentPhotoUri: Uri? = null
    private var weatherData: FishingWeather? = null

    private val PERMISSIONS_REQUEST_CODE = 101
    private val MAP_REQUEST_CODE = 1002
    private val MARKER_MAP_REQUEST_CODE = 1003

    private var selectedLatitude: Double = 0.0
    private var selectedLongitude: Double = 0.0
    private var selectedFishingType: String = ""

    // Добавляем переменные для работы с диапазоном дат
    private var startDate: Long? = null
    private var endDate: Long? = null
    private var isMultiDayFishing: Boolean = false

    // Флаг для отслеживания состояния сети
    private var isOfflineMode = false

    // ID маркерной карты (если создана)
    private var markerMapId: String = ""

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedPhotos.add(uri)
                updatePhotoCounter()
            }
        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            currentPhotoUri?.let { uri ->
                selectedPhotos.add(uri)
                updatePhotoCounter()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddFishingNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Проверяем доступность сети и устанавливаем режим работы
        checkNetworkAndUpdateUI()

        // Добавляем кнопку "назад" в ActionBar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.add_note)

        // Инициализация UI элементов с использованием findViewById
        initViews()

        // Проверка разрешений
        checkAndRequestPermissions()

        // Скрываем основную форму до выбора типа рыбалки
        formContent.visibility = View.GONE
        buttonInitialCancel.visibility = View.VISIBLE

        // Инициализация выпадающего списка типов рыбалки
        setupFishingTypeDropdown()

        // Настройка обработчиков событий
        setupEventListeners()
    }

    /**
     * Проверяет доступность сети и обновляет UI соответственно
     */
    private fun checkNetworkAndUpdateUI() {
        isOfflineMode = !NetworkUtils.isNetworkAvailable(this)

        // Если сеть недоступна, включаем офлайн-режим в FirebaseManager
        FirebaseManager.checkNetworkAndSwitchToOfflineModeIfNeeded(this)

        // Показываем сообщение о режиме работы
        if (isOfflineMode) {
            Toast.makeText(
                this,
                "Приложение работает в офлайн-режиме. Ваши заметки будут синхронизированы позже.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun initViews() {
        // Инициализация всех UI элементов через findViewById
        formContent = findViewById(R.id.formContent)
        buttonInitialCancel = findViewById(R.id.buttonInitialCancel)
        dropdownFishingType = findViewById(R.id.dropdownFishingType)
        formContentTitle = findViewById(R.id.formContentTitle)
        textViewSelectedCoordinates = findViewById(R.id.textViewSelectedCoordinates)
        editTextFishingDates = findViewById(R.id.editTextFishingDates)
        buttonAddPhoto = findViewById(R.id.buttonAddPhoto)
        buttonSave = findViewById(R.id.buttonSave)
        buttonCancel = findViewById(R.id.buttonCancel)
        buttonOpenMap = findViewById(R.id.buttonOpenMap)
        buttonOpenMarkerMap = findViewById(R.id.buttonOpenMarkerMap)
        buttonLoadWeather = findViewById(R.id.buttonLoadWeather)
        progressBarWeather = findViewById(R.id.progressBarWeather)
        textViewWeatherStatus = findViewById(R.id.textViewWeatherStatus)
        editTextLocation = findViewById(R.id.editTextLocation)
        editTextTackle = findViewById(R.id.editTextTackle)
        editTextNotes = findViewById(R.id.editTextNotes)
        textViewPhotoCount = findViewById(R.id.textViewPhotoCount)
        mainProgressBar = findViewById(R.id.progressBar)
    }

    override fun onResume() {
        super.onResume()
        // Проверяем состояние сети при возвращении к активности
        checkNetworkAndUpdateUI()
    }

    private fun setupEventListeners() {
        // Обработчик выбора дат
        editTextFishingDates.setOnClickListener {
            showDateRangePicker()
        }

        // Обработчик добавления фото - отключаем в офлайн-режиме
        buttonAddPhoto.setOnClickListener {
            if (isOfflineMode) {
                showOfflineModeDialog("добавления фотографий")
            } else {
                showPhotoSourceDialog()
            }
        }

        // Обработчик сохранения
        buttonSave.setOnClickListener {
            saveFishingNote()
        }

        // Обработчик отмены (обе кнопки отмены)
        buttonCancel.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_top, R.anim.slide_out_bottom)
        }

        buttonInitialCancel.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_top, R.anim.slide_out_bottom)
        }

        // Обработчик нажатия на кнопку открытия карты
        buttonOpenMap.setOnClickListener {
            openMap()
        }

        // Обработчик нажатия на кнопку открытия маркерной карты
        buttonOpenMarkerMap.setOnClickListener {
            openMarkerMap()
        }

        // Обработчик загрузки погоды - отключаем в офлайн-режиме
        buttonLoadWeather.setOnClickListener {
            if (isOfflineMode) {
                showOfflineModeDialog("загрузки погоды")
            } else {
                loadWeatherData()
            }
        }
    }

    /**
     * Показывает диалог выбора диапазона дат
     */
    private fun showDateRangePicker() {
        // Создаем билдер для выбора диапазона дат
        val builder = MaterialDatePicker.Builder.dateRangePicker()
        builder.setTitleText("Выберите даты рыбалки")

        // Если даты уже были выбраны, устанавливаем их как начальные значения
        if (startDate != null && endDate != null) {
            builder.setSelection(androidx.core.util.Pair(startDate, endDate))
        }

        // Ограничения на выбор дат (опционально, в зависимости от потребностей)
        val constraintsBuilder = CalendarConstraints.Builder()
        // Раскомментируйте следующую строку, если хотите ограничить выбор только будущими датами
        //constraintsBuilder.setStart(MaterialDatePicker.todayInUtcMilliseconds())

        builder.setCalendarConstraints(constraintsBuilder.build())

        // Создаем пикер и настраиваем обработчики
        val picker = builder.build()

        picker.addOnPositiveButtonClickListener { selection ->
            // Получаем выбранный диапазон
            startDate = selection.first
            endDate = selection.second

            // Проверяем, является ли это однодневной или многодневной рыбалкой
            isMultiDayFishing = startDate != endDate

            // Форматируем и отображаем выбранные даты
            updateDateDisplay()
        }

        picker.addOnCancelListener {
            // Ничего не делаем при отмене, оставляем поле как есть
        }

        picker.addOnNegativeButtonClickListener {
            // Ничего не делаем при отмене, оставляем поле как есть
        }

        picker.show(supportFragmentManager, "DATE_RANGE_PICKER")
    }

    /**
     * Обновляет отображение выбранных дат в поле
     */
    private fun updateDateDisplay() {
        if (startDate == null || endDate == null) {
            editTextFishingDates.setText("")
            return
        }

        val startCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        startCalendar.timeInMillis = startDate!!

        val endCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        endCalendar.timeInMillis = endDate!!

        val formattedStartDate = dateFormat.format(startCalendar.time)

        if (isMultiDayFishing) {
            val formattedEndDate = dateFormat.format(endCalendar.time)
            editTextFishingDates.setText("$formattedStartDate — $formattedEndDate")
        } else {
            editTextFishingDates.setText(formattedStartDate)
        }
    }

    /**
     * Показывает диалог о недоступности функции в офлайн-режиме
     */
    private fun showOfflineModeDialog(featureName: String) {
        AlertDialog.Builder(this)
            .setTitle("Офлайн-режим")
            .setMessage("Функция $featureName недоступна в офлайн-режиме. Пожалуйста, подключитесь к интернету.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun setupFishingTypeDropdown() {
        // Создаем список типов рыбалки из строковых ресурсов
        val fishingTypes = listOf(
            getString(R.string.fishing_type_carp),    // Карповая рыбалка
            getString(R.string.fishing_type_spinning),
            getString(R.string.fishing_type_feeder),
            getString(R.string.fishing_type_float),
            getString(R.string.fishing_type_winter),
            getString(R.string.fishing_type_flyfishing),
            getString(R.string.fishing_type_trolling),
            getString(R.string.fishing_type_other)
        )

        // Создаем адаптер для выпадающего списка
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, fishingTypes)
        dropdownFishingType.setAdapter(adapter)

        // Устанавливаем слушатель выбора
        dropdownFishingType.setOnItemClickListener { _, _, position, _ ->
            selectedFishingType = fishingTypes[position]

            // Показываем форму после выбора типа рыбалки
            formContent.visibility = View.VISIBLE
            buttonInitialCancel.visibility = View.GONE

            formContentTitle.text = getString(R.string.fishing_details_for, selectedFishingType)

            // Показываем кнопку маркерной карты только для карповой рыбалки
            if (selectedFishingType == getString(R.string.fishing_type_carp)) {
                buttonOpenMarkerMap.visibility = View.VISIBLE
            } else {
                buttonOpenMarkerMap.visibility = View.GONE
            }

            // Прокручиваем до начала формы
            binding.scrollView.post {
                binding.scrollView.smoothScrollTo(0, formContent.top)
            }
        }
    }

    private fun openMap() {
        val intent = Intent(this, MapActivity::class.java)
        startActivityForResult(intent, MAP_REQUEST_CODE)
    }

    /**
     * Открытие маркерной карты дна
     */
    private fun openMarkerMap() {
        val intent = Intent(this, MarkerMapActivity::class.java)

        // Если уже есть ID карты, передаем его
        if (markerMapId.isNotEmpty()) {
            intent.putExtra(MarkerMapActivity.EXTRA_MAP_ID, markerMapId)
        }

        // Передаем имя карты (название места ловли)
        val locationName = editTextLocation.text.toString().trim()
        val mapName = if (locationName.isNotEmpty()) {
            "Карта $locationName"
        } else {
            "Новая карта"
        }
        intent.putExtra(MarkerMapActivity.EXTRA_MAP_NAME, mapName)

        // Запускаем активность
        startActivityForResult(intent, MARKER_MAP_REQUEST_CODE)
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.S_V2) {
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (checkSelfPermission(android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(android.Manifest.permission.CAMERA)
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissions(permissionsToRequest.toTypedArray(), PERMISSIONS_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            val allPermissionsGranted =
                grantResults.all { it == android.content.pm.PackageManager.PERMISSION_GRANTED }

            if (!allPermissionsGranted) {
                Toast.makeText(
                    this,
                    "Для полной функциональности приложению требуются разрешения на доступ к камере и галерее",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showPhotoSourceDialog() {
        val options = arrayOf(
            getString(R.string.take_photo),
            getString(R.string.choose_photo)
        )

        AlertDialog.Builder(this)
            .setTitle(R.string.add_photo)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> takePhoto()
                    1 -> chooseFromGallery()
                }
            }
            .show()
    }

    private fun takePhoto() {
        val photoFile = createImageFile()
        photoFile?.let {
            currentPhotoUri = FileProvider.getUriForFile(
                this,
                "com.example.driftnotes.fileprovider",
                it
            )

            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri)
            cameraLauncher.launch(intent)
        }
    }

    private fun chooseFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private fun createImageFile(): File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = getExternalFilesDir("Pictures")
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }

    private fun updatePhotoCounter() {
        textViewPhotoCount.text = resources.getQuantityString(
            R.plurals.photo_count,
            selectedPhotos.size,
            selectedPhotos.size
        )
    }

    // Загрузка погоды по координатам выбранного места
    private fun loadWeatherData() {
        if (selectedLatitude == 0.0 && selectedLongitude == 0.0) {
            Toast.makeText(this, R.string.weather_need_location, Toast.LENGTH_SHORT).show()
            return
        }

        // Проверяем наличие сети
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(
                this,
                "Невозможно загрузить погоду: отсутствует подключение к интернету",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        progressBarWeather.visibility = View.VISIBLE
        textViewWeatherStatus.text = getString(R.string.weather_loading)
        buttonLoadWeather.isEnabled = false

        lifecycleScope.launch {
            try {
                val weather =
                    weatherRepository.getWeatherForLocation(selectedLatitude, selectedLongitude)

                if (weather != null) {
                    weatherData = weather
                    textViewWeatherStatus.text = weather.weatherDescription
                    Toast.makeText(
                        this@AddFishingNoteActivity,
                        R.string.weather_success,
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    textViewWeatherStatus.text =
                        getString(R.string.weather_error, "Не удалось получить данные")
                }
            } catch (e: Exception) {
                textViewWeatherStatus.text = getString(R.string.weather_error, e.message)
            } finally {
                progressBarWeather.visibility = View.INVISIBLE
                buttonLoadWeather.isEnabled = true
            }
        }
    }

    private fun saveFishingNote() {
        // Проверяем, что пользователь выбрал тип рыбалки
        if (selectedFishingType.isEmpty()) {
            Toast.makeText(this, "Выберите тип рыбалки", Toast.LENGTH_SHORT).show()
            return
        }

        val location = editTextLocation.text.toString().trim()

        // Проверяем обязательные поля
        if (location.isEmpty()) {
            Toast.makeText(this, "Необходимо указать место рыбалки", Toast.LENGTH_SHORT).show()
            return
        }

        // Проверяем, что дата выбрана
        if (startDate == null || endDate == null) {
            Toast.makeText(this, "Пожалуйста, выберите дату или период рыбалки.", Toast.LENGTH_SHORT).show()
            return
        }

        // Преобразуем выбранные даты из миллисекунд в Date
        val startDateObj = Date(startDate!!)
        val endDateObj = if (isMultiDayFishing) Date(endDate!!) else null

        // Отключаем кнопку сохранения, чтобы избежать двойного нажатия
        buttonSave.isEnabled = false

        // Проверяем режим работы
        if (isOfflineMode && selectedPhotos.isNotEmpty()) {
            // В офлайн-режиме фотографии не доступны
            showOfflinePhotosDialog(startDateObj, endDateObj)
        } else if (selectedPhotos.isEmpty()) {
            // Если нет фотографий, просто сохраняем заметку
            saveNoteToFirestore(emptyList(), startDateObj, endDateObj)
        } else {
            // Если есть фотографии и мы в онлайн-режиме, загружаем их и сохраняем заметку
            uploadPhotosAndSaveNote(startDateObj, endDateObj)
        }
    }

    /**
     * Показывает диалог о невозможности сохранения фотографий в офлайн-режиме
     */
    private fun showOfflinePhotosDialog(startDate: Date, endDate: Date?) {
        AlertDialog.Builder(this)
            .setTitle("Офлайн-режим")
            .setMessage("В офлайн-режиме невозможно загрузить фотографии. Вы хотите сохранить заметку без фотографий?")
            .setPositiveButton("Сохранить без фото") { _, _ ->
                saveNoteToFirestore(emptyList(), startDate, endDate)
            }
            .setNegativeButton("Отмена") { _, _ ->
                buttonSave.isEnabled = true
            }
            .show()
    }

    private fun uploadPhotosAndSaveNote(startDate: Date, endDate: Date?) {
        // Проверяем подключение к сети перед загрузкой
        if (!NetworkUtils.isNetworkAvailable(this)) {
            // Если сети нет, предлагаем сохранить без фото
            showOfflinePhotosDialog(startDate, endDate)
            return
        }

        val photoUrls = mutableListOf<String>()
        var uploadedCount = 0
        var errorCount = 0

        // Показываем основной индикатор загрузки
        mainProgressBar.visibility = View.VISIBLE

        if (selectedPhotos.isEmpty()) {
            // Если нет фотографий, сразу сохраняем запись
            saveNoteToFirestore(photoUrls, startDate, endDate)
            return
        }

        // Устанавливаем максимальное время ожидания
        val maxWaitTime = 30000L // 30 секунд
        val startTime = System.currentTimeMillis()

        // Механизм для отслеживания тайм-аута загрузки
        val timeoutRunnable = Runnable {
            if (uploadedCount + errorCount < selectedPhotos.size) {
                // Если процесс не завершен, считаем это тайм-аутом
                Log.e("AddFishingNote", "Тайм-аут загрузки фотографий")
                runOnUiThread {
                    AlertDialog.Builder(this)
                        .setTitle("Проблема с сетью")
                        .setMessage("Возникли проблемы при загрузке фотографий. Хотите сохранить заметку без фото?")
                        .setPositiveButton("Сохранить без фото") { _, _ ->
                            saveNoteToFirestore(emptyList(), startDate, endDate)
                        }
                        .setNegativeButton("Отмена") { _, _ ->
                            mainProgressBar.visibility = View.GONE
                            buttonSave.isEnabled = true
                        }
                        .show()
                }
            }
        }

        // Планируем проверку тайм-аута
        binding.root.postDelayed(timeoutRunnable, maxWaitTime)

        for (i in selectedPhotos.indices) {
            val photoUri = selectedPhotos[i]

            try {
                Log.d("AddFishingNote", "Начинаем загрузку фото $i: $photoUri")

                // Создаем уникальный идентификатор для файла
                val fileUUID = UUID.randomUUID().toString()
                val fileName = "fishing_photo_${fileUUID}.jpg"

                // Полный путь к файлу в Storage
                val userId = auth.currentUser?.uid ?: "anonymous"
                val photoRef = storage.reference.child("users/$userId/photos/$fileName")

                // Исправление: создаем новый поток для каждой загрузки
                val inputStream = contentResolver.openInputStream(photoUri)
                if (inputStream != null) {
                    try {
                        // Создаем буфер для хранения данных
                        val bytes = inputStream.readBytes()
                        inputStream.close() // Закрываем поток после чтения

                        // Загружаем из буфера, а не из потока
                        val uploadTask = photoRef.putBytes(bytes)

                        uploadTask.addOnSuccessListener {
                            Log.d("AddFishingNote", "Фото $i загружено успешно")

                            // Получаем URL для загруженного файла
                            photoRef.downloadUrl.addOnSuccessListener { downloadUri ->
                                photoUrls.add(downloadUri.toString())
                                uploadedCount++

                                Log.d("AddFishingNote", "Получен URL $downloadUri для фото $i")

                                // Если все фотографии обработаны, сохраняем запись
                                if (uploadedCount + errorCount == selectedPhotos.size) {
                                    // Отменяем проверку тайм-аута
                                    binding.root.removeCallbacks(timeoutRunnable)

                                    Log.d(
                                        "AddFishingNote",
                                        "Все фото обработаны. Сохраняем запись."
                                    )
                                    saveNoteToFirestore(photoUrls, startDate, endDate)
                                }
                            }.addOnFailureListener { e ->
                                Log.e(
                                    "AddFishingNote",
                                    "Ошибка получения URL для фото $i: ${e.message}",
                                    e
                                )
                                errorCount++

                                if (uploadedCount + errorCount == selectedPhotos.size) {
                                    // Отменяем проверку тайм-аута
                                    binding.root.removeCallbacks(timeoutRunnable)

                                    saveNoteToFirestore(photoUrls, startDate, endDate)
                                }
                            }
                        }.addOnFailureListener { e ->
                            Log.e("AddFishingNote", "Ошибка загрузки фото $i: ${e.message}", e)
                            errorCount++

                            // Проверяем, не истекло ли время ожидания
                            if (System.currentTimeMillis() - startTime > maxWaitTime) {
                                // Если время истекло, отменяем загрузку
                                binding.root.removeCallbacks(timeoutRunnable)

                                runOnUiThread {
                                    AlertDialog.Builder(this)
                                        .setTitle("Превышено время ожидания")
                                        .setMessage("Загрузка фотографий занимает слишком много времени. Хотите сохранить заметку без фото?")
                                        .setPositiveButton("Сохранить без фото") { _, _ ->
                                            saveNoteToFirestore(emptyList(), startDate, endDate)
                                        }
                                        .setNegativeButton("Отмена") { _, _ ->
                                            mainProgressBar.visibility = View.GONE
                                            buttonSave.isEnabled = true
                                        }
                                        .show()
                                }
                                return@addOnFailureListener
                            }

                            if (uploadedCount + errorCount == selectedPhotos.size) {
                                // Отменяем проверку тайм-аута
                                binding.root.removeCallbacks(timeoutRunnable)

                                saveNoteToFirestore(photoUrls, startDate, endDate)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(
                            "AddFishingNote",
                            "Исключение при обработке потока для фото $i: ${e.message}",
                            e
                        )
                        errorCount++

                        if (uploadedCount + errorCount == selectedPhotos.size) {
                            // Отменяем проверку тайм-аута
                            binding.root.removeCallbacks(timeoutRunnable)

                            saveNoteToFirestore(photoUrls, startDate, endDate)
                        }
                    }
                } else {
                    Log.e("AddFishingNote", "Не удалось открыть поток для фото $i")
                    errorCount++

                    if (uploadedCount + errorCount == selectedPhotos.size) {
                        // Отменяем проверку тайм-аута
                        binding.root.removeCallbacks(timeoutRunnable)

                        saveNoteToFirestore(photoUrls, startDate, endDate)
                    }
                }
            } catch (e: Exception) {
                Log.e("AddFishingNote", "Исключение при обработке фото $i: ${e.message}", e)
                errorCount++

                if (uploadedCount + errorCount == selectedPhotos.size) {
                    // Отменяем проверку тайм-аута
                    binding.root.removeCallbacks(timeoutRunnable)

                    saveNoteToFirestore(photoUrls, startDate, endDate)
                }
            }
        }

        // Устанавливаем таймер для отслеживания общего времени загрузки
        binding.root.postDelayed({
            if (uploadedCount + errorCount < selectedPhotos.size) {
                // Если не все фотографии обработаны, показываем диалог
                AlertDialog.Builder(this)
                    .setTitle("Проблема с загрузкой")
                    .setMessage("Загрузка фотографий занимает слишком много времени. Хотите продолжить ожидание или сохранить заметку без фото?")
                    .setPositiveButton("Продолжить ожидание", null)
                    .setNegativeButton("Сохранить без фото") { _, _ ->
                        // Отменяем проверку тайм-аута
                        binding.root.removeCallbacks(timeoutRunnable)

                        saveNoteToFirestore(emptyList(), startDate, endDate)
                    }
                    .show()
            }
        }, 15000) // Через 15 секунд показываем диалог
    }

    private fun saveNoteToFirestore(photoUrls: List<String>, startDate: Date, endDate: Date?) {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            Toast.makeText(this, "Ошибка авторизации, попробуйте войти заново", Toast.LENGTH_SHORT)
                .show()
            buttonSave.isEnabled = true
            mainProgressBar.visibility = View.GONE
            return
        }

        // Получаем текст из полей (теперь необязательных)
        val tackle = editTextTackle.text.toString().trim()
        val notes = editTextNotes.text.toString().trim()

        // Определяем, является ли рыбалка многодневной
        val isMultiDay = endDate != null && !isSameDay(startDate, endDate)

        // Создаем объект записи о рыбалке с координатами, погодой, типом рыбалки и ID маркерной карты
        val fishingNote = FishingNote(
            userId = userId,
            location = editTextLocation.text.toString().trim(),
            latitude = selectedLatitude,
            longitude = selectedLongitude,
            date = startDate,
            endDate = endDate,
            isMultiDay = isMultiDay,
            tackle = tackle,
            notes = notes,
            photoUrls = photoUrls,
            fishingType = selectedFishingType,
            weather = weatherData,
            markerMapId = markerMapId // Добавляем ID маркерной карты
        )

        // Проверяем доступность Firestore (мог измениться режим работы)
        if (!NetworkUtils.isNetworkAvailable(this)) {
            // Включаем офлайн-режим в Firebase
            FirebaseManager.checkNetworkAndSwitchToOfflineModeIfNeeded(this)
        }

        // Сохраняем запись в Firestore
        firestore.collection("fishing_notes")
            .add(fishingNote)
            .addOnSuccessListener {
                // Показываем соответствующее сообщение в зависимости от режима работы
                if (NetworkUtils.isNetworkAvailable(this)) {
                    Toast.makeText(this, R.string.note_saved, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        this,
                        "Заметка сохранена локально и будет синхронизирована при подключении к интернету",
                        Toast.LENGTH_LONG
                    ).show()
                }

                mainProgressBar.visibility = View.GONE
                finish()
                overridePendingTransition(R.anim.slide_in_top, R.anim.slide_out_bottom)
            }
            .addOnFailureListener { e ->
                buttonSave.isEnabled = true
                mainProgressBar.visibility = View.GONE

                // Более подробное сообщение об ошибке
                val errorMessage = if (e.message?.contains("network") == true ||
                    e.message?.contains("connect") == true ||
                    e.message?.contains("timeout") == true
                ) {
                    "Ошибка сети при сохранении. " +
                            "Заметка будет сохранена локально и синхронизирована позже."
                } else {
                    getString(R.string.error_saving, e.message)
                }

                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()

                // Если это ошибка сети, но Firestore в офлайн-режиме, то заметка все равно
                // должна быть сохранена локально, поэтому закрываем активность
                if (FirebaseManager.isOfflineModeEnabled() &&
                    (e.message?.contains("network") == true ||
                            e.message?.contains("connect") == true ||
                            e.message?.contains("timeout") == true)
                ) {
                    finish()
                    overridePendingTransition(R.anim.slide_in_top, R.anim.slide_out_bottom)
                }
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            MAP_REQUEST_CODE -> {
                if (resultCode == RESULT_OK) {
                    data?.let {
                        val locationName = it.getStringExtra("location_name") ?: ""
                        val latitude = it.getDoubleExtra("latitude", 0.0)
                        val longitude = it.getDoubleExtra("longitude", 0.0)

                        // Показываем и заполняем текстовое поле с координатами
                        textViewSelectedCoordinates.visibility = View.VISIBLE
                        textViewSelectedCoordinates.text = getString(
                            R.string.coordinates_format,
                            latitude,
                            longitude
                        )

                        // Теперь пользователь может ввести название места сам
                        // или использовать предложенное с карты
                        if (editTextLocation.text.toString().isEmpty()) {
                            editTextLocation.setText(locationName)
                        }

                        selectedLatitude = latitude
                        selectedLongitude = longitude

                        // Сбрасываем погодные данные при изменении локации
                        weatherData = null
                        textViewWeatherStatus.text = getString(R.string.weather_not_loaded)
                    }
                }
            }

            MARKER_MAP_REQUEST_CODE -> {
                if (resultCode == RESULT_OK && data != null) {
                    // Получаем ID маркерной карты и сохраняем его
                    val mapId = data.getStringExtra(MarkerMapActivity.EXTRA_MAP_ID)
                    if (mapId != null) {
                        markerMapId = mapId

                        // Обновляем надпись на кнопке, чтобы показать, что карта создана
                        buttonOpenMarkerMap.text = getString(R.string.edit_marker_map)

                        Toast.makeText(
                            this,
                            getString(R.string.marker_map_created),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            overridePendingTransition(R.anim.slide_in_top, R.anim.slide_out_bottom)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        finish()
        overridePendingTransition(R.anim.slide_in_top, R.anim.slide_out_bottom)
    }
}