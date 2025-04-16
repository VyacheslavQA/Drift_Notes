package com.example.driftnotes

import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.driftnotes.databinding.ActivityMainBinding
import com.example.driftnotes.utils.FirebaseManager
import com.example.driftnotes.auth.LoginActivity
import com.example.driftnotes.fishing.AddFishingNoteActivity
import com.example.driftnotes.fishing.FishingNoteAdapter
import com.example.driftnotes.fishing.FishingNoteDetailActivity
import com.example.driftnotes.models.FishingNote
import com.example.driftnotes.utils.AnimationHelper
import com.google.android.material.navigation.NavigationView
import com.google.firebase.firestore.Query

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

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

        // Настраиваем toolbar и drawer
        setSupportActionBar(binding.toolbar)
        setupNavigationDrawer()

        // Настраиваем RecyclerView
        adapter = FishingNoteAdapter(fishingNotes) { note ->
            val intent = Intent(this, FishingNoteDetailActivity::class.java)
            intent.putExtra("note_id", note.id)
            AnimationHelper.startActivityWithAnimation(this, intent)
        }

        binding.recyclerView?.layoutManager = LinearLayoutManager(this)
        binding.recyclerView?.adapter = adapter

        // Добавляем плавающую кнопку для быстрого добавления заметки
        binding.fabAddNote?.setOnClickListener {
            val intent = Intent(this, AddFishingNoteActivity::class.java)
            AnimationHelper.startActivityWithAnimation(this, intent)
        }

        // Загружаем заметки
        loadFishingNotes()
    }

    private fun setupNavigationDrawer() {
        val toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        binding.navView.setNavigationItemSelectedListener(this)

        // Настраиваем информацию о пользователе в шапке меню
        val headerView = binding.navView.getHeaderView(0)
        val usernameTextView = headerView.findViewById<android.widget.TextView>(R.id.textViewUsername)
        val emailTextView = headerView.findViewById<android.widget.TextView>(R.id.textViewUserEmail)

        FirebaseManager.auth.currentUser?.let { user ->
            usernameTextView.text = user.displayName ?: "Пользователь"
            emailTextView.text = user.email ?: "Журнал рыбалки"
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_profile -> {
                Toast.makeText(this, "Личный кабинет - в разработке", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_stats -> {
                Toast.makeText(this, "Статистика - в разработке", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_my_notes -> {
                // Мы уже на этом экране, просто закрываем drawer
            }
            R.id.nav_settings -> {
                Toast.makeText(this, "Настройки - в разработке", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_help -> {
                Toast.makeText(this, "Помощь/Связь - в разработке", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_logout -> {
                FirebaseManager.auth.signOut()
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }
        }

        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
            // Добавляем анимацию при выходе по кнопке "Назад"
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }

    private fun loadFishingNotes() {
        val userId = FirebaseManager.getCurrentUserId() ?: return

        // Показываем индикатор загрузки
        binding.progressBar?.visibility = android.view.View.VISIBLE
        binding.textNoNotes?.visibility = android.view.View.GONE

        FirebaseManager.firestore.collection("fishing_notes")
            .whereEqualTo("userId", userId)
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                // Скрываем индикатор загрузки
                binding.progressBar?.visibility = android.view.View.GONE

                fishingNotes.clear()
                for (document in documents) {
                    val note = document.toObject(FishingNote::class.java).copy(id = document.id)
                    fishingNotes.add(note)
                }
                adapter.notifyDataSetChanged()

                // Показываем сообщение, если нет записей
                if (fishingNotes.isEmpty()) {
                    binding.textNoNotes?.visibility = android.view.View.VISIBLE
                } else {
                    binding.textNoNotes?.visibility = android.view.View.GONE
                }
            }
            .addOnFailureListener { e ->
                // Скрываем индикатор загрузки
                binding.progressBar?.visibility = android.view.View.GONE

                // Обработка ошибки - показываем сообщение пользователю
                Toast.makeText(
                    this,
                    getString(R.string.error_loading_notes, e.message),
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}