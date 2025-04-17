package com.example.driftnotes

import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.driftnotes.databinding.ActivityMainBinding
import com.example.driftnotes.utils.FirebaseManager
import com.example.driftnotes.auth.LoginActivity
import com.example.driftnotes.fishing.AddFishingNoteActivity
import com.example.driftnotes.fishing.FishingNoteAdapter
import com.example.driftnotes.fishing.FishingNoteDetailActivity
import com.example.driftnotes.models.FishingNote
import com.example.driftnotes.utils.AnimationHelper
import com.google.firebase.firestore.Query

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
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        // Настраиваем RecyclerView
        adapter = FishingNoteAdapter(fishingNotes) { note ->
            val intent = Intent(this, FishingNoteDetailActivity::class.java)
            intent.putExtra("note_id", note.id)
            AnimationHelper.startActivityWithAnimation(this, intent)
        }

        // Безопасно настраиваем RecyclerView
        val recyclerView = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerView)
        recyclerView?.let {
            it.layoutManager = LinearLayoutManager(this)
            it.adapter = adapter
        }

        // Безопасно настраиваем FAB
        val fabAddNote = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabAddNote)
        fabAddNote?.setOnClickListener {
            val intent = Intent(this, AddFishingNoteActivity::class.java)
            AnimationHelper.startActivityWithAnimation(this, intent)
        }

        // Загружаем заметки
        loadFishingNotes()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // Добавляем анимацию при выходе по кнопке "Назад"
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    private fun loadFishingNotes() {
        val userId = FirebaseManager.getCurrentUserId() ?: return

        // Безопасно обращаемся к элементам интерфейса
        val progressBar = findViewById<android.widget.ProgressBar>(R.id.progressBar)
        val textNoNotes = findViewById<android.widget.TextView>(R.id.textNoNotes)

        // Показываем индикатор загрузки
        progressBar?.visibility = View.VISIBLE
        textNoNotes?.visibility = View.GONE

        FirebaseManager.firestore.collection("fishing_notes")
            .whereEqualTo("userId", userId)
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                // Скрываем индикатор загрузки
                progressBar?.visibility = View.GONE

                fishingNotes.clear()
                for (document in documents) {
                    val note = document.toObject(FishingNote::class.java).copy(id = document.id)
                    fishingNotes.add(note)
                }
                adapter.notifyDataSetChanged()

                // Показываем сообщение, если нет записей
                if (fishingNotes.isEmpty()) {
                    textNoNotes?.visibility = View.VISIBLE
                } else {
                    textNoNotes?.visibility = View.GONE
                }
            }
            .addOnFailureListener { e ->
                // Скрываем индикатор загрузки
                progressBar?.visibility = View.GONE

                // Обработка ошибки - показываем сообщение пользователю
                Toast.makeText(
                    this,
                    getString(R.string.error_loading_notes, e.message),
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}
