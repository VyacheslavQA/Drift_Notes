package com.example.driftnotes.fishing

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.driftnotes.R
import com.example.driftnotes.databinding.ActivityAddFishingNoteBinding
import com.example.driftnotes.maps.MapActivity
import com.example.driftnotes.models.FishingNote
import com.example.driftnotes.models.FishingWeather
import com.example.driftnotes.repository.WeatherRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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

    private var selectedLatitude: Double = 0.0
    private var selectedLongitude: Double = 0.0
    private var selectedFishingType: String = ""

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

        checkAndRequestPermissions()

        // Установка начальной даты (сегодня)
        updateDateDisplay()

        // Инициализация выпадающего списка типов рыбалки
        setupFishingTypeDropdown()

        // Скрываем текст с координатами, пока они не выбраны
        binding.textViewSelectedCoordinates.visibility = View.GONE

        // Обработчик выбора даты
        binding.editTextDate.setOnClickListener {
            showDatePicker()
        }

        // Обработчик добавления фото
        binding.buttonAddPhoto.setOnClickListener {
            showPhotoSourceDialog()
        }

        // Обработчик сохранения
        binding.buttonSave.setOnClickListener {
            saveFishingNote()
        }

        // Обработчик отмены
        binding.buttonCancel.setOnClickListener {
            finish()
        }

        // Обработчик нажатия на кнопку открытия карты
        binding.buttonOpenMap.setOnClickListener {
            openMap()
        }

        // Установка системной иконки для кнопки загрузки погоды
        binding.buttonLoadWeather.setCompoundDrawablesWithIntrinsicBounds(
            android.R.drawable.ic_menu_compass, 0, 0, 0
        )

        // Обработчик загрузки погоды
        binding.buttonLoadWeather.setOnClickListener {
            loadWeatherData()
        }
    }

    private fun setupFishingTypeDropdown() {
        // Создаем список типов рыбалки из строковых ресурсов
        val fishingTypes = listOf(
            getString(R.string.fishing_type_carp),    // Добавлена карповая рыбалка
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
        }

        // Устанавливаем значение по умолчанию
        binding.dropdownFishingType.setText(fishingTypes[0], false)
        selectedFishingType = fishingTypes[0]
    }

    private fun openMap() {
        val intent = Intent(this, MapActivity::class.java)
        startActivityForResult(intent, MAP_REQUEST_CODE)
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
        binding.editTextDate.setText(dateFormat.format(calendar.time))
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
        binding.textViewPhotoCount.text = resources.getQuantityString(
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
            Toast.makeText(this, "Координаты: $selectedLatitude, $selectedLongitude", Toast.LENGTH_SHORT).show()
        }

        binding.progressBarWeather.visibility = View.VISIBLE
        binding.textViewWeatherStatus.text = getString(R.string.weather_loading)
        binding.buttonLoadWeather.isEnabled = false

        lifecycleScope.launch {
            try {
                val weather = weatherRepository.getWeatherForLocation(selectedLatitude, selectedLongitude)

                if (weather != null) {
                    weatherData = weather
                    binding.textViewWeatherStatus.text = weather.weatherDescription
                    Toast.makeText(this@AddFishingNoteActivity, R.string.weather_success, Toast.LENGTH_SHORT).show()
                } else {
                    binding.textViewWeatherStatus.text = getString(R.string.weather_error, "Не удалось получить данные")
                }
            } catch (e: Exception) {
                binding.textViewWeatherStatus.text = getString(R.string.weather_error, e.message)
            } finally {
                binding.progressBarWeather.visibility = View.INVISIBLE
                binding.buttonLoadWeather.isEnabled = true
            }
        }
    }

    private fun saveFishingNote() {
        val location = binding.editTextLocation.text.toString().trim()
        val tackle = binding.editTextTackle.text.toString().trim()
        val notes = binding.editTextNotes.text.toString().trim()

        if (location.isEmpty() || tackle.isEmpty()) {
            Toast.makeText(this, "Пожалуйста, заполните обязательные поля", Toast.LENGTH_SHORT).show()
            return
        }

        binding.buttonSave.isEnabled = false

        if (selectedPhotos.isEmpty()) {
            saveNoteToFirestore(emptyList())
        } else {
            uploadPhotosAndSaveNote()
        }
    }

    private fun uploadPhotosAndSaveNote() {
        val photoUrls = mutableListOf<String>()
        var uploadedCount = 0

        for (i in selectedPhotos.indices) {
            val photoUri = selectedPhotos[i]
            val fileName = "fishing_photo_${System.currentTimeMillis()}_$i.jpg"
            val photoRef = storage.reference.child("fishing_photos/${auth.currentUser?.uid}/$fileName")

            photoRef.putFile(photoUri)
                .addOnSuccessListener {
                    photoRef.downloadUrl.addOnSuccessListener { uri ->
                        photoUrls.add(uri.toString())
                        uploadedCount++

                        if (uploadedCount == selectedPhotos.size) {
                            saveNoteToFirestore(photoUrls)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    uploadedCount++
                    Toast.makeText(this,
                        "Ошибка при загрузке фото: ${e.message}",
                        Toast.LENGTH_SHORT).show()

                    if (uploadedCount == selectedPhotos.size) {
                        saveNoteToFirestore(photoUrls)
                    }
                }
        }
    }

    private fun saveNoteToFirestore(photoUrls: List<String>) {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            Toast.makeText(this, "Ошибка авторизации, попробуйте войти заново", Toast.LENGTH_SHORT).show()
            binding.buttonSave.isEnabled = true
            return
        }

        // Создаем объект записи о рыбалке с координатами, погодой и типом рыбалки
        val fishingNote = FishingNote(
            userId = userId,
            location = binding.editTextLocation.text.toString().trim(),
            latitude = selectedLatitude,
            longitude = selectedLongitude,
            date = calendar.time,
            tackle = binding.editTextTackle.text.toString().trim(),
            notes = binding.editTextNotes.text.toString().trim(),
            photoUrls = photoUrls,
            fishingType = selectedFishingType,  // Сохраняем выбранный тип рыбалки
            weather = weatherData // Добавляем погодные данные
        )

        // Сохраняем запись в Firestore
        firestore.collection("fishing_notes")
            .add(fishingNote)
            .addOnSuccessListener {
                Toast.makeText(this, R.string.note_saved, Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                binding.buttonSave.isEnabled = true
                Toast.makeText(
                    this,
                    getString(R.string.error_saving, e.message),
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == MAP_REQUEST_CODE && resultCode == RESULT_OK) {
            data?.let {
                val locationName = it.getStringExtra("location_name") ?: ""
                val latitude = it.getDoubleExtra("latitude", 0.0)
                val longitude = it.getDoubleExtra("longitude", 0.0)

                // Показываем и заполняем текстовое поле с координатами
                binding.textViewSelectedCoordinates.visibility = View.VISIBLE
                binding.textViewSelectedCoordinates.text = getString(
                    R.string.coordinates_format,
                    latitude,
                    longitude
                )

                // Теперь пользователь может ввести название места сам
                // или использовать предложенное с карты
                if (binding.editTextLocation.text.toString().isEmpty()) {
                    binding.editTextLocation.setText(locationName)
                }

                selectedLatitude = latitude
                selectedLongitude = longitude

                // Сбрасываем погодные данные при изменении локации
                weatherData = null
                binding.textViewWeatherStatus.text = getString(R.string.weather_not_loaded)
            }
        }
    }
}