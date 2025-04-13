package com.example.driftnotes.fishing

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class AddFishingNoteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddFishingNoteBinding
    private lateinit var auth: FirebaseAuth
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val weatherRepository = WeatherRepository()

    private val calendar = Calendar.getInstance()
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

        // Добавляем кнопку "назад" в ActionBar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.add_note)

        checkAndRequestPermissions()

        // Скрываем основную форму до выбора типа рыбалки
        binding.formContent.visibility = View.GONE
        binding.buttonInitialCancel.visibility = View.VISIBLE

        // Находим элементы внутри formContent через findViewById
        val formContentTitle = findViewById<android.widget.TextView>(R.id.formContentTitle)
        val textViewSelectedCoordinates = findViewById<android.widget.TextView>(R.id.textViewSelectedCoordinates)
        val editTextDate = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editTextDate)
        val buttonAddPhoto = findViewById<android.widget.Button>(R.id.buttonAddPhoto)
        val buttonSave = findViewById<android.widget.Button>(R.id.buttonSave)
        val buttonCancel = findViewById<android.widget.Button>(R.id.buttonCancel)
        val buttonOpenMap = findViewById<android.widget.Button>(R.id.buttonOpenMap)
        val buttonOpenMarkerMap = findViewById<android.widget.Button>(R.id.buttonOpenMarkerMap)
        val buttonLoadWeather = findViewById<android.widget.Button>(R.id.buttonLoadWeather)
        val progressBarWeather = findViewById<android.widget.ProgressBar>(R.id.progressBarWeather)
        val textViewWeatherStatus = findViewById<android.widget.TextView>(R.id.textViewWeatherStatus)
        val editTextLocation = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editTextLocation)
        val editTextTackle = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editTextTackle)
        val editTextNotes = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editTextNotes)
        val textViewPhotoCount = findViewById<android.widget.TextView>(R.id.textViewPhotoCount)

        // Скрываем текст с координатами, пока они не выбраны
        textViewSelectedCoordinates?.visibility = View.GONE

        // Установка начальной даты (сегодня)
        updateDateDisplay()

        // Инициализация выпадающего списка типов рыбалки
        setupFishingTypeDropdown()

        // Обработчик выбора даты
        editTextDate?.setOnClickListener {
            showDatePicker()
        }

        // Обработчик добавления фото
        buttonAddPhoto?.setOnClickListener {
            showPhotoSourceDialog()
        }

        // Обработчик сохранения
        buttonSave?.setOnClickListener {
            saveFishingNote()
        }

        // Обработчик отмены (обе кнопки отмены)
        buttonCancel?.setOnClickListener {
            AnimationHelper.finishWithAnimation(this)
        }

        binding.buttonInitialCancel.setOnClickListener {
            AnimationHelper.finishWithAnimation(this)
        }

        // Обработчик нажатия на кнопку открытия карты
        buttonOpenMap?.setOnClickListener {
            openMap()
        }

        // Обработчик нажатия на кнопку открытия маркерной карты
        buttonOpenMarkerMap?.setOnClickListener {
            openMarkerMap()
        }

        // Установка системной иконки для кнопки загрузки погоды
        buttonLoadWeather?.setCompoundDrawablesWithIntrinsicBounds(
            android.R.drawable.ic_menu_compass, 0, 0, 0
        )

        // Обработчик загрузки погоды
        buttonLoadWeather?.setOnClickListener {
            loadWeatherData()
        }
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
        binding.dropdownFishingType.setAdapter(adapter)

        // Устанавливаем слушатель выбора
        binding.dropdownFishingType.setOnItemClickListener { _, _, position, _ ->
            selectedFishingType = fishingTypes[position]

            // Показываем форму после выбора типа рыбалки
            binding.formContent.visibility = View.VISIBLE
            binding.buttonInitialCancel.visibility = View.GONE

            val formContentTitle = findViewById<android.widget.TextView>(R.id.formContentTitle)
            formContentTitle?.text = getString(R.string.fishing_details_for, selectedFishingType)

            // Показываем кнопку маркерной карты только для карповой рыбалки
            val buttonOpenMarkerMap = findViewById<android.widget.Button>(R.id.buttonOpenMarkerMap)
            if (selectedFishingType == getString(R.string.fishing_type_carp)) {
                buttonOpenMarkerMap?.visibility = View.VISIBLE
            } else {
                buttonOpenMarkerMap?.visibility = View.GONE
            }

            // Прокручиваем до начала формы
            binding.scrollView.post {
                binding.scrollView.smoothScrollTo(0, binding.formContent.top)
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
        val editTextLocation = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editTextLocation)
        val locationName = editTextLocation?.text.toString().trim()
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
            val allPermissionsGranted = grantResults.all { it == android.content.pm.PackageManager.PERMISSION_GRANTED }

            if (!allPermissionsGranted) {
                Toast.makeText(
                    this,
                    "Для полной функциональности приложению требуются разрешения на доступ к камере и галерее",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun updateDateDisplay() {
        val editTextDate = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editTextDate)
        editTextDate?.setText(dateFormat.format(calendar.time))
    }

    private fun showDatePicker() {
        DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, day)
                updateDateDisplay()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
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
        val textViewPhotoCount = findViewById<android.widget.TextView>(R.id.textViewPhotoCount)
        textViewPhotoCount?.text = resources.getQuantityString(
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

        val progressBarWeather = findViewById<android.widget.ProgressBar>(R.id.progressBarWeather)
        val textViewWeatherStatus = findViewById<android.widget.TextView>(R.id.textViewWeatherStatus)
        val buttonLoadWeather = findViewById<android.widget.Button>(R.id.buttonLoadWeather)

        progressBarWeather?.visibility = View.VISIBLE
        textViewWeatherStatus?.text = getString(R.string.weather_loading)
        buttonLoadWeather?.isEnabled = false

        lifecycleScope.launch {
            try {
                val weather = weatherRepository.getWeatherForLocation(selectedLatitude, selectedLongitude)

                if (weather != null) {
                    weatherData = weather
                    textViewWeatherStatus?.text = weather.weatherDescription
                    Toast.makeText(this@AddFishingNoteActivity, R.string.weather_success, Toast.LENGTH_SHORT).show()
                } else {
                    textViewWeatherStatus?.text = getString(R.string.weather_error, "Не удалось получить данные")
                }
            } catch (e: Exception) {
                textViewWeatherStatus?.text = getString(R.string.weather_error, e.message)
            } finally {
                progressBarWeather?.visibility = View.INVISIBLE
                buttonLoadWeather?.isEnabled = true
            }
        }
    }

    private fun saveFishingNote() {
        // Проверяем, что пользователь выбрал тип рыбалки
        if (selectedFishingType.isEmpty()) {
            Toast.makeText(this, "Выберите тип рыбалки", Toast.LENGTH_SHORT).show()
            return
        }

        val editTextLocation = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editTextLocation)
        val editTextTackle = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editTextTackle)
        val editTextNotes = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editTextNotes)

        val location = editTextLocation?.text.toString().trim()
        val tackle = editTextTackle?.text.toString().trim()
        val notes = editTextNotes?.text.toString().trim()

        if (location.isEmpty() || tackle.isEmpty()) {
            Toast.makeText(this, "Пожалуйста, заполните обязательные поля", Toast.LENGTH_SHORT).show()
            return
        }

        val buttonSave = findViewById<android.widget.Button>(R.id.buttonSave)
        buttonSave?.isEnabled = false

        if (selectedPhotos.isEmpty()) {
            saveNoteToFirestore(emptyList())
        } else {
            uploadPhotosAndSaveNote()
        }
    }

    private fun uploadPhotosAndSaveNote() {
        val photoUrls = mutableListOf<String>()
        var uploadedCount = 0
        var errorCount = 0

        // Показываем индикатор загрузки - создаем его программно
        val progressBar = ProgressBar(this).apply {
            id = View.generateViewId()
            visibility = View.VISIBLE
        }

        val rootView = findViewById<View>(android.R.id.content)
        (rootView as? ViewGroup)?.addView(progressBar,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                if (this is FrameLayout.LayoutParams) {
                    gravity = Gravity.CENTER
                }
            }
        )

        for (i in selectedPhotos.indices) {
            val photoUri = selectedPhotos[i]

            // Создаем уникальный идентификатор для файла
            val fileUUID = UUID.randomUUID().toString()
            val fileName = "fishing_photo_${fileUUID}.jpg"

            // Полный путь к файлу в Storage
            val userId = auth.currentUser?.uid ?: "anonymous"
            val photoRef = storage.reference.child("users/$userId/photos/$fileName")

            try {
                Log.d("AddFishingNote", "Начинаем загрузку фото $i: $photoUri в $fileName")

                // Получаем входной поток из Uri
                val inputStream = contentResolver.openInputStream(photoUri)

                if (inputStream != null) {
                    // Загружаем фото из потока
                    val uploadTask = photoRef.putStream(inputStream)

                    uploadTask.addOnSuccessListener {
                        Log.d("AddFishingNote", "Фото $i загружено успешно")

                        // Получаем URL для загруженного файла
                        photoRef.downloadUrl.addOnSuccessListener { downloadUri ->
                            photoUrls.add(downloadUri.toString())
                            uploadedCount++

                            Log.d("AddFishingNote", "Получен URL $downloadUri для фото $i")

                            // Если все фотографии обработаны, сохраняем запись
                            if (uploadedCount + errorCount == selectedPhotos.size) {
                                Log.d("AddFishingNote", "Все фото обработаны. Сохраняем запись.")
                                saveNoteToFirestore(photoUrls)
                            }
                        }.addOnFailureListener { e ->
                            Log.e("AddFishingNote", "Ошибка получения URL для фото $i: ${e.message}", e)
                            errorCount++

                            if (uploadedCount + errorCount == selectedPhotos.size) {
                                saveNoteToFirestore(photoUrls)
                            }
                        }
                    }.addOnFailureListener { e ->
                        Log.e("AddFishingNote", "Ошибка загрузки фото $i: ${e.message}", e)
                        errorCount++

                        if (uploadedCount + errorCount == selectedPhotos.size) {
                            saveNoteToFirestore(photoUrls)
                        }
                    }

                    // Закрываем поток
                    inputStream.close()
                } else {
                    Log.e("AddFishingNote", "Не удалось открыть поток для фото $i")
                    errorCount++

                    if (uploadedCount + errorCount == selectedPhotos.size) {
                        saveNoteToFirestore(photoUrls)
                    }
                }
            } catch (e: Exception) {
                Log.e("AddFishingNote", "Исключение при обработке фото $i: ${e.message}", e)
                errorCount++

                if (uploadedCount + errorCount == selectedPhotos.size) {
                    saveNoteToFirestore(photoUrls)
                }
            }
        }
    }

    private fun saveNoteToFirestore(photoUrls: List<String>) {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            Toast.makeText(this, "Ошибка авторизации, попробуйте войти заново", Toast.LENGTH_SHORT).show()

            val buttonSave = findViewById<android.widget.Button>(R.id.buttonSave)
            buttonSave?.isEnabled = true

            // Скрываем индикатор загрузки
            val progressBar = findViewById<ProgressBar>(View.generateViewId())
            if (progressBar != null) {
                (progressBar.parent as? ViewGroup)?.removeView(progressBar)
            }
            return
        }

        val editTextLocation = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editTextLocation)
        val editTextTackle = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editTextTackle)
        val editTextNotes = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editTextNotes)

        // Создаем объект записи о рыбалке с координатами, погодой, типом рыбалки и ID маркерной карты
        val fishingNote = FishingNote(
            userId = userId,
            location = editTextLocation?.text.toString().trim() ?: "",
            latitude = selectedLatitude,
            longitude = selectedLongitude,
            date = calendar.time,
            tackle = editTextTackle?.text.toString().trim() ?: "",
            notes = editTextNotes?.text.toString().trim() ?: "",
            photoUrls = photoUrls,
            fishingType = selectedFishingType,
            weather = weatherData,
            markerMapId = markerMapId // Добавляем ID маркерной карты
        )

        // Сохраняем запись в Firestore
        firestore.collection("fishing_notes")
            .add(fishingNote)
            .addOnSuccessListener {
                Toast.makeText(this, R.string.note_saved, Toast.LENGTH_SHORT).show()

                // Скрываем индикатор загрузки
                val progressBar = findViewById<ProgressBar>(View.generateViewId())
                if (progressBar != null) {
                    (progressBar.parent as? ViewGroup)?.removeView(progressBar)
                }

                AnimationHelper.finishWithAnimation(this)
            }
            .addOnFailureListener { e ->
                val buttonSave = findViewById<android.widget.Button>(R.id.buttonSave)
                buttonSave?.isEnabled = true

                // Скрываем индикатор загрузки
                val progressBar = findViewById<ProgressBar>(View.generateViewId())
                if (progressBar != null) {
                    (progressBar.parent as? ViewGroup)?.removeView(progressBar)
                }

                Toast.makeText(
                    this,
                    getString(R.string.error_saving, e.message),
                    Toast.LENGTH_SHORT
                ).show()
            }
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

                        // Обновляем View с координатами
                        val textViewSelectedCoordinates = findViewById<android.widget.TextView>(R.id.textViewSelectedCoordinates)
                        val editTextLocation = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editTextLocation)
                        val textViewWeatherStatus = findViewById<android.widget.TextView>(R.id.textViewWeatherStatus)

                        // Показываем и заполняем текстовое поле с координатами
                        textViewSelectedCoordinates?.visibility = View.VISIBLE
                        textViewSelectedCoordinates?.text = getString(
                            R.string.coordinates_format,
                            latitude,
                            longitude
                        )

                        // Теперь пользователь может ввести название места сам
                        // или использовать предложенное с карты
                        if (editTextLocation?.text.toString().isEmpty()) {
                            editTextLocation?.setText(locationName)
                        }

                        selectedLatitude = latitude
                        selectedLongitude = longitude

                        // Сбрасываем погодные данные при изменении локации
                        weatherData = null
                        textViewWeatherStatus?.text = getString(R.string.weather_not_loaded)
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
                        val buttonOpenMarkerMap = findViewById<android.widget.Button>(R.id.buttonOpenMarkerMap)
                        buttonOpenMarkerMap?.text = getString(R.string.edit_marker_map)

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

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    // Используем диспетчер вместо переопределения
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        // Вызываем диспетчер
        onBackPressedDispatcher.onBackPressed()
    }
}