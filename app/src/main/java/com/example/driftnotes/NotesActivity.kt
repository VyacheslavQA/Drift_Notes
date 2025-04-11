package com.example.driftnotes

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.driftnotes.databinding.ActivityNotesBinding
import com.example.driftnotes.fishing.AddFishingNoteActivity
import com.example.driftnotes.fishing.FishingNoteAdapter
import com.example.driftnotes.fishing.FishingNoteDetailActivity
import com.example.driftnotes.models.FishingNote
import com.example.driftnotes.utils.FirebaseManager
import com.google.firebase.firestore.Query

class NotesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotesBinding
    private val fishingNotes = mutableListOf<FishingNote>()
    private lateinit var adapter: FishingNoteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Устанавливаем заголовок и кнопку "Назад" в ActionBar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.my_notes)

        // Настраиваем RecyclerView
        adapter = FishingNoteAdapter(fishingNotes) { note ->
            val intent = Intent(this, FishingNoteDetailActivity::class.java)
            intent.putExtra("note_id", note.id)
            startActivity(intent)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        // Настраиваем FAB для добавления новой записи
        binding.fabAddNote.setOnClickListener {
            startActivity(Intent(this, AddFishingNoteActivity::class.java))
        }

        // Загружаем записи из Firestore
        loadFishingNotes()
    }

    override fun onResume() {
        super.onResume()
        loadFishingNotes()
    }

    private fun loadFishingNotes() {
        val userId = FirebaseManager.getCurrentUserId() ?: return

        // Показываем индикатор загрузки
        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.textNoNotes.visibility = android.view.View.GONE

        FirebaseManager.firestore.collection("fishing_notes")
            .whereEqualTo("userId", userId)
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                // Скрываем индикатор загрузки
                binding.progressBar.visibility = android.view.View.GONE

                fishingNotes.clear()
                for (document in documents) {
                    val note = document.toObject(FishingNote::class.java).copy(id = document.id)
                    fishingNotes.add(note)
                }
                adapter.notifyDataSetChanged()

                // Показываем сообщение, если нет записей
                if (fishingNotes.isEmpty()) {
                    binding.textNoNotes.visibility = android.view.View.VISIBLE
                } else {
                    binding.textNoNotes.visibility = android.view.View.GONE
                }
            }
            .addOnFailureListener { e ->
                // Скрываем индикатор загрузки
                binding.progressBar.visibility = android.view.View.GONE

                // Обработка ошибки - показываем сообщение пользователю
                Toast.makeText(
                    this,
                    getString(R.string.error_loading_notes, e.message),
                    Toast.LENGTH_SHORT
                ).show()

                // Логирование ошибки
                Log.e("NotesActivity", "Error loading fishing notes", e)
            }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}