package com.example.driftnotes.profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.example.driftnotes.R
import com.example.driftnotes.auth.PasswordRecoveryActivity
import com.example.driftnotes.databinding.ActivityProfileBinding
import com.example.driftnotes.utils.AnimationHelper
import com.example.driftnotes.utils.FirebaseManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var auth: FirebaseAuth
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance().reference

    // URI для выбранного изображения аватара
    private var selectedAvatarUri: Uri? = null
    private var currentPhotoUri: Uri? = null

    // Список стран/городов для выпадающего списка
    private val countriesAndCities = listOf(
        "Россия - Москва", "Россия - Санкт-Петербург", "Россия - Новосибирск",
        "Россия - Екатеринбург", "Россия - Казань", "Россия - Нижний Новгород",
        "Беларусь - Минск", "Беларусь - Гомель", "Беларусь - Витебск",
        "Украина - Киев", "Украина - Харьков", "Украина - Одесса",
        "Казахстан - Алматы", "Казахстан - Нур-Султан", "Узбекистан - Ташкент"
    )

    // Список уровней опыта
    private val experienceLevels = listOf("Новичок", "Любитель", "Продвинутый", "Профи", "Эксперт")

    // Лаунчеры для выбора изображения
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedAvatarUri = uri
                // Отображаем выбранное изображение
                Glide.with(this)
                    .load(selectedAvatarUri)
                    .circleCrop()
                    .into(binding.imageViewAvatar)
            }
        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            currentPhotoUri?.let { uri ->
                selectedAvatarUri = uri
                // Отображаем сделанное фото
                Glide.with(this)
                    .load(selectedAvatarUri)
                    .circleCrop()
                    .into(binding.imageViewAvatar)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Инициализация Firebase
        auth = FirebaseAuth.getInstance()

        // Настройка ActionBar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Личный кабинет"

        // Инициализация выпадающих списков
        setupDropdowns()

        // Загрузка данных пользователя
        loadUserData()

        // Настройка обработчиков событий
        setupClickListeners()
    }

    private fun setupDropdowns() {
        // Настройка выпадающего списка стран/городов
        val countryCityAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, countriesAndCities)
        (binding.dropdownCountryCity as? AutoCompleteTextView)?.setAdapter(countryCityAdapter)

        // Настройка выпадающего списка опыта
        val experienceAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, experienceLevels)
        (binding.dropdownExperience as? AutoCompleteTextView)?.setAdapter(experienceAdapter)
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Загрузка имени пользователя и email
            binding.editTextUsername.setText(currentUser.displayName ?: "")
            binding.editTextEmail.setText(currentUser.email ?: "")

            // Загрузка аватара пользователя, если есть
            currentUser.photoUrl?.let { photoUrl ->
                Glide.with(this)
                    .load(photoUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_fishing_hook)
                    .into(binding.imageViewAvatar)
            }

            // Загрузка дополнительных данных из Firestore
            firestore.collection("users")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        // Загрузка региона
                        binding.editTextRegion.setText(document.getString("region") ?: "")

                        // Загрузка выбранной страны/города
                        val countryCity = document.getString("countryCity") ?: ""
                        binding.dropdownCountryCity.setText(countryCity, false)

                        // Загрузка уровня опыта
                        val experience = document.getString("experience") ?: ""
                        binding.dropdownExperience.setText(experience, false)

                        // Загрузка любимых видов рыбалки
                        val fishingTypes = document.get("fishingTypes") as? List<String> ?: listOf()

                        // Установка чекбоксов
                        binding.checkboxCarp.isChecked = fishingTypes.contains("Карповая")
                        binding.checkboxSpinning.isChecked = fishingTypes.contains("Спиннинг")
                        binding.checkboxFeeder.isChecked = fishingTypes.contains("Фидер")
                        binding.checkboxFloat.isChecked = fishingTypes.contains("Поплавочная")
                        binding.checkboxWinter.isChecked = fishingTypes.contains("Зимняя рыбалка")
                        binding.checkboxFlyFishing.isChecked = fishingTypes.contains("Нахлыст")
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Ошибка при загрузке профиля: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun setupClickListeners() {
        // Обработчик нажатия на кнопку изменения аватара
        binding.buttonChangeAvatar.setOnClickListener {
            showImageSourceDialog()
        }

        // Обработчик нажатия на кнопку смены пароля
        binding.buttonChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        // Обработчик нажатия на кнопку сохранения профиля
        binding.buttonSaveProfile.setOnClickListener {
            saveUserProfile()
        }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Сделать фото", "Выбрать из галереи")

        AlertDialog.Builder(this)
            .setTitle("Выберите источник изображения")
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
        val imageFileName = "AVATAR_" + timeStamp + "_"
        val storageDir = getExternalFilesDir("Pictures")
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }

    private fun showChangePasswordDialog() {
        // Проверяем, вошел ли пользователь с помощью email/пароля
        val user = auth.currentUser
        if (user != null) {
            // Проверяем, может ли пользователь сменить пароль
            val providerData = user.providerData
            val isEmailProvider = providerData.any { it.providerId == "password" }

            if (isEmailProvider) {
                // Показываем диалог смены пароля
                ChangePasswordDialog(this) {
                    // Callback после успешной смены пароля
                    Toast.makeText(this, "Пароль успешно изменен", Toast.LENGTH_SHORT).show()
                }.show()
            } else {
                // Для пользователей, вошедших через Google или другие методы
                AlertDialog.Builder(this)
                    .setTitle("Смена пароля недоступна")
                    .setMessage("Вы вошли через внешний провайдер аутентификации (Google, Facebook и т.д.). Смена пароля в приложении недоступна.")
                    .setPositiveButton("Понятно", null)
                    .show()
            }
        } else {
            // Если пользователь не аутентифицирован
            Toast.makeText(this, "Вы не авторизованы", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveUserProfile() {
        val currentUser = auth.currentUser ?: return

        // Сохранение имени пользователя
        val newUsername = binding.editTextUsername.text.toString().trim()
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(newUsername)
            .build()

        // Обновляем профиль в Firebase Auth
        currentUser.updateProfile(profileUpdates)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Toast.makeText(this, "Ошибка при обновлении имени: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }

        // Если email изменился, обновляем его
        val newEmail = binding.editTextEmail.text.toString().trim()
        if (newEmail != currentUser.email && newEmail.isNotEmpty()) {
            currentUser.updateEmail(newEmail)
                .addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        Toast.makeText(this, "Ошибка при обновлении email: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        // Если выбрано новое изображение профиля, загружаем его
        selectedAvatarUri?.let { uri ->
            uploadProfileImage(uri, currentUser.uid)
        } ?: saveUserDataToFirestore(currentUser.uid, null)
    }

    private fun uploadProfileImage(uri: Uri, userId: String) {
        val profileImagesRef = storage.child("profile_images/${userId}.jpg")

        // Загрузка изображения в Firebase Storage
        profileImagesRef.putFile(uri)
            .addOnSuccessListener {
                // Получение URL загруженного изображения
                profileImagesRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    // Обновление URL фото в профиле пользователя
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setPhotoUri(downloadUri)
                        .build()

                    auth.currentUser?.updateProfile(profileUpdates)
                        ?.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                // После успешной загрузки изображения сохраняем остальные данные
                                saveUserDataToFirestore(userId, downloadUri.toString())
                            } else {
                                Toast.makeText(this, "Ошибка при обновлении фото профиля", Toast.LENGTH_SHORT).show()
                                // Сохраняем остальные данные даже при ошибке с фото
                                saveUserDataToFirestore(userId, null)
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка при загрузке изображения: ${e.message}", Toast.LENGTH_SHORT).show()
                // Сохраняем остальные данные даже при ошибке с фото
                saveUserDataToFirestore(userId, null)
            }
    }

    private fun saveUserDataToFirestore(userId: String, photoUrl: String?) {
        // Собираем список выбранных видов рыбалки
        val selectedFishingTypes = mutableListOf<String>()
        if (binding.checkboxCarp.isChecked) selectedFishingTypes.add("Карповая")
        if (binding.checkboxSpinning.isChecked) selectedFishingTypes.add("Спиннинг")
        if (binding.checkboxFeeder.isChecked) selectedFishingTypes.add("Фидер")
        if (binding.checkboxFloat.isChecked) selectedFishingTypes.add("Поплавочная")
        if (binding.checkboxWinter.isChecked) selectedFishingTypes.add("Зимняя рыбалка")
        if (binding.checkboxFlyFishing.isChecked) selectedFishingTypes.add("Нахлыст")

        // Создаем документ с данными пользователя
        val userData = hashMapOf(
            "username" to binding.editTextUsername.text.toString().trim(),
            "email" to binding.editTextEmail.text.toString().trim(),
            "region" to binding.editTextRegion.text.toString().trim(),
            "countryCity" to binding.dropdownCountryCity.text.toString(),
            "experience" to binding.dropdownExperience.text.toString(),
            "fishingTypes" to selectedFishingTypes
        )

        // Если есть URL фото, добавляем его в данные
        photoUrl?.let {
            userData["photoUrl"] = it
        }

        // Сохраняем данные в Firestore
        firestore.collection("users")
            .document(userId)
            .set(userData)
            .addOnSuccessListener {
                Toast.makeText(this, "Профиль успешно обновлен", Toast.LENGTH_SHORT).show()
                // Возвращаемся на предыдущий экран с анимацией
                AnimationHelper.finishWithAnimation(this)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка при сохранении профиля: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // Возврат на предыдущий экран с анимацией
                AnimationHelper.finishWithAnimation(this)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        // Используем анимацию при возврате назад
        AnimationHelper.finishWithAnimation(this)
    }
}