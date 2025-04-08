package com.example.driftnotes.fishing

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.driftnotes.R
import com.example.driftnotes.databinding.ActivityAddFishingNoteBinding
import com.example.driftnotes.maps.MapActivity
import com.example.driftnotes.models.FishingNote
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
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

    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    private val selectedPhotos = mutableListOf<Uri>()
    private var currentPhotoUri: Uri? = null

    private val PERMISSIONS_REQUEST_CODE = 101
    // Добавляем константу для идентификации запроса карты
    private val MAP_REQUEST_CODE = 1002

    // Добавляем переменные для хранения координат
    private var selectedLatitude: Double = 0.0
    private var selectedLongitude: Double = 0.0

    // Обработчики результатов выбора фото
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

        // Проверка и запрос разрешений
        checkAndRequestPermissions()

        // Установка начальной даты (сегодня)
        updateDateDisplay()

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

        // Добавляем слушатель для выбора местоположения
        binding.editTextLocation.setOnClickListener {
            // Запускаем активность карты для выбора местоположения
            val intent = Intent(this, MapActivity::class.java)
            startActivityForResult(intent, MAP_REQUEST_CODE)
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        // Проверяем разрешения в зависимости от версии Android
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+) использует READ_MEDIA_IMAGES вместо READ_EXTERNAL_STORAGE
            if (checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.S_V2) {
            // Android 12 и ниже используют READ_EXTERNAL_STORAGE
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        // Проверка разрешения CAMERA для всех версий
        if (checkSelfPermission(android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(android.Manifest.permission.CAMERA)
        }

        // Если есть разрешения для запроса, запрашиваем их
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
            // Проверяем, все ли разрешения предоставлены
            val allPermissionsGranted = grantResults.all { it == android.content.pm.PackageManager.PERMISSION_GRANTED }

            if (!allPermissionsGranted) {
                // Если не все разрешения предоставлены, показываем сообщение
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
            // Если нет фото, сразу сохраняем запись
            saveNoteToFirestore(emptyList())
        } else {
            // Если есть фото, сначала загружаем их в Storage
            uploadPhotosAndSaveNote()
        }
    }

    private fun uploadPhotosAndSaveNote() {
        val photoUrls = mutableListOf<String>()
        var uploadedCount = 0

        // Показываем индикатор загрузки
        // (можно добавить ProgressBar в layout и управлять его видимостью здесь)

        for (i in selectedPhotos.indices) {
            val photoUri = selectedPhotos[i]
            val fileName = "fishing_photo_${System.currentTimeMillis()}_$i.jpg"
            val photoRef = storage.reference.child("fishing_photos/${auth.currentUser?.uid}/$fileName")

            photoRef.putFile(photoUri)
                .addOnSuccessListener {
                    // Получаем URL загруженного фото
                    photoRef.downloadUrl.addOnSuccessListener { uri ->
                        photoUrls.add(uri.toString())
                        uploadedCount++

                        // Когда все фото загружены, сохраняем запись
                        if (uploadedCount == selectedPhotos.size) {
                            saveNoteToFirestore(photoUrls)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    // В случае ошибки загрузки фото
                    uploadedCount++
                    Toast.makeText(this,
                        "Ошибка при загрузке фото: ${e.message}",
                        Toast.LENGTH_SHORT).show()

                    // Если все остальные фото обработаны, сохраняем запись с теми фото, что удалось загрузить
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

        // Создаем объект записи о рыбалке с координатами
        val fishingNote = FishingNote(
            userId = userId,
            location = binding.editTextLocation.text.toString().trim(),
            latitude = selectedLatitude,  // Добавляем широту
            longitude = selectedLongitude, // Добавляем долготу
            date = calendar.time,
            tackle = binding.editTextTackle.text.toString().trim(),
            notes = binding.editTextNotes.text.toString().trim(),
            photoUrls = photoUrls
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

    // Добавляем метод для обработки результата выбора местоположения
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == MAP_REQUEST_CODE && resultCode == RESULT_OK) {
            data?.let {
                val locationName = it.getStringExtra("location_name") ?: ""
                val latitude = it.getDoubleExtra("latitude", 0.0)
                val longitude = it.getDoubleExtra("longitude", 0.0)

                // Устанавливаем выбранное место в поле ввода
                binding.editTextLocation.setText(locationName)

                // Сохраняем координаты для использования при сохранении записи
                selectedLatitude = latitude
                selectedLongitude = longitude
            }
        }
    }
}