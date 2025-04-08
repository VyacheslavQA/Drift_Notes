package com.example.driftnotes

import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.driftnotes.databinding.ActivityMainBinding
import com.google.firebase.firestore.Query
import com.example.driftnotes.utils.FirebaseManager
import com.example.driftnotes.models.FishingNote
import com.example.driftnotes.fishing.FishingNoteAdapter
import com.example.driftnotes.auth.LoginActivity
import com.example.driftnotes.fishing.FishingNoteDetailActivity
import com.example.driftnotes.fishing.AddFishingNoteActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val fishingNotes = mutableListOf<FishingNote>()
    private lateinit var adapter: FishingNoteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Инициализируем Firebase сервисы
        FirebaseManager.initialize(this)

        // Проверяем, авторизован ли пользователь
        if (!FirebaseManager.isUserLoggedIn()) {
            // Если нет, перенаправляем на экран входа
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Настраиваем RecyclerView
        adapter = FishingNoteAdapter(fishingNotes) { note ->
            val intent = Intent(this, FishingNoteDetailActivity::class.java)
            intent.putExtra("note_id", note.id)
            startActivity(intent)
        }

        binding.recyclerView?.layoutManager = LinearLayoutManager(this)
        binding.recyclerView?.adapter = adapter

        // Настраиваем FAB для добавления новой записи
        binding.fabAddNote?.setOnClickListener {
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

        // Показываем индикатор загрузки (с проверкой на null)
        binding.progressBar?.visibility = android.view.View.VISIBLE
        binding.textNoNotes?.visibility = android.view.View.GONE

        FirebaseManager.firestore.collection("fishing_notes")
            .whereEqualTo("userId", userId)
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                // Скрываем индикатор загрузки (с проверкой на null)
                binding.progressBar?.visibility = android.view.View.GONE

                fishingNotes.clear()
                for (document in documents) {
                    val note = document.toObject(FishingNote::class.java).copy(id = document.id)
                    fishingNotes.add(note)
                }
                adapter.notifyDataSetChanged()

                // Показываем сообщение, если нет записей (с проверкой на null)
                if (fishingNotes.isEmpty()) {
                    binding.textNoNotes?.visibility = android.view.View.VISIBLE
                } else {
                    binding.textNoNotes?.visibility = android.view.View.GONE
                }
            }
            .addOnFailureListener { e ->
                // Скрываем индикатор загрузки (с проверкой на null)
                binding.progressBar?.visibility = android.view.View.GONE

                // Обработка ошибки - показываем сообщение пользователю
                Toast.makeText(
                    this,
                    getString(R.string.error_loading_notes, e.message),
                    Toast.LENGTH_SHORT
                ).show()

                // Логирование ошибки
                Log.e("MainActivity", "Error loading fishing notes", e)
            }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_logout -> {
                FirebaseManager.auth.signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}