package com.example.driftnotes.fishing

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.driftnotes.R
import com.example.driftnotes.databinding.ActivityFishingNoteDetailBinding
import com.example.driftnotes.maps.MapActivity
import com.example.driftnotes.models.FishingNote
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class FishingNoteDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFishingNoteDetailBinding
    private lateinit var auth: FirebaseAuth
    private val firestore = FirebaseFirestore.getInstance()
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    private var noteId: String? = null
    private var currentNote: FishingNote? = null
    private lateinit var photoAdapter: PhotoPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFishingNoteDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Получаем ID записи из интента
        noteId = intent.getStringExtra("note_id")

        if (noteId == null) {
            Toast.makeText(this, "Ошибка: запись не найдена", Toast.LENGTH_SHORT).show()
            finish()
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

        // Загружаем данные записи
        loadNoteData()
    }

    private fun openLocationOnMap() {
        currentNote?.let { note ->
            if (note.latitude != 0.0 && note.longitude != 0.0) {
                val intent = Intent(this, MapActivity::class.java)
                intent.putExtra("view_only", true)
                intent.putExtra("latitude", note.latitude)
                intent.putExtra("longitude", note.longitude)
                intent.putExtra("title", note.location)
                startActivity(intent)
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
                        finish()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Ошибка загрузки: ${e.message}", Toast.LENGTH_SHORT).show()
                    finish()
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
            binding.textViewDate.text = dateFormat.format(note.date)
            binding.textViewTackle.text = note.tackle
            binding.textViewNotes.text = note.notes

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
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.detail_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
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
            firestore.collection("fishing_notes")
                .document(id)
                .delete()
                .addOnSuccessListener {
                    Toast.makeText(this, R.string.note_deleted, Toast.LENGTH_SHORT).show()
                    finish()
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
}