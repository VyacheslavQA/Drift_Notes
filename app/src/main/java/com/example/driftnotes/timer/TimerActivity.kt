package com.example.driftnotes.timer

import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.example.driftnotes.R
import com.example.driftnotes.databinding.ActivityTimerBinding
import com.example.driftnotes.utils.AnimationHelper
import java.util.concurrent.TimeUnit

class TimerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTimerBinding
    private var timerService: TimerService? = null
    private var bound = false

    // Связь с сервисом
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            try {
                val binder = service as TimerService.LocalBinder
                timerService = binder.getService()
                bound = true
                updateTimerViews()
            } catch (e: Exception) {
                Log.e("TimerActivity", "Ошибка подключения к сервису: ${e.message}")
                Toast.makeText(
                    this@TimerActivity,
                    "Не удалось подключиться к сервису таймеров",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            timerService = null
            bound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityTimerBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Настройка ActionBar
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = "Таймеры"

            // Запускаем сервис и привязываемся к нему
            val intent = Intent(this, TimerService::class.java)
            startService(intent)
            bindService(intent, connection, Context.BIND_AUTO_CREATE)

            // Настраиваем обработчики кнопок для каждого таймера
            setupTimerButtons()

            // Обработка кнопки "Назад"
            setupBackPressedCallback()
        } catch (e: Exception) {
            Log.e("TimerActivity", "Ошибка в onCreate: ${e.message}")
            Toast.makeText(
                this,
                "Произошла ошибка при запуске активности таймеров",
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }
    }

    // Настройка обработчика кнопки "Назад"
    private fun setupBackPressedCallback() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                try {
                    AnimationHelper.finishWithAnimation(this@TimerActivity)
                } catch (e: Exception) {
                    Log.e("TimerActivity", "Ошибка при обработке нажатия кнопки 'Назад': ${e.message}")
                    finish()
                }
            }
        })
    }

    private fun setupTimerButtons() {
        try {
            // Настройка первого таймера
            setupTimerControls(
                TimerService.TIMER_1,
                binding.cardTimer1,
                binding.textViewTimer1Name,
                binding.textViewTimer1,
                binding.buttonTimer1Start,
                binding.buttonTimer1Stop,
                binding.buttonTimer1Options,
                binding.progressTimer1
            )

            // Настройка второго таймера
            setupTimerControls(
                TimerService.TIMER_2,
                binding.cardTimer2,
                binding.textViewTimer2Name,
                binding.textViewTimer2,
                binding.buttonTimer2Start,
                binding.buttonTimer2Stop,
                binding.buttonTimer2Options,
                binding.progressTimer2
            )

            // Настройка третьего таймера
            setupTimerControls(
                TimerService.TIMER_3,
                binding.cardTimer3,
                binding.textViewTimer3Name,
                binding.textViewTimer3,
                binding.buttonTimer3Start,
                binding.buttonTimer3Stop,
                binding.buttonTimer3Options,
                binding.progressTimer3
            )

            // Настройка четвертого таймера
            setupTimerControls(
                TimerService.TIMER_4,
                binding.cardTimer4,
                binding.textViewTimer4Name,
                binding.textViewTimer4,
                binding.buttonTimer4Start,
                binding.buttonTimer4Stop,
                binding.buttonTimer4Options,
                binding.progressTimer4
            )
        } catch (e: Exception) {
            Log.e("TimerActivity", "Ошибка в setupTimerButtons: ${e.message}")
        }
    }

    private fun setupTimerControls(
        timerId: Int,
        cardView: View,
        nameTextView: androidx.appcompat.widget.AppCompatTextView,
        timerTextView: androidx.appcompat.widget.AppCompatTextView,
        startButton: com.google.android.material.button.MaterialButton,
        stopButton: com.google.android.material.button.MaterialButton,
        optionsButton: com.google.android.material.button.MaterialButton,
        progressBar: android.widget.ProgressBar
    ) {
        try {
            // Обработчик кнопки Старт
            startButton.setOnClickListener {
                showDurationDialog(timerId)
            }

            // Обработчик кнопки Стоп
            stopButton.setOnClickListener {
                timerService?.stopTimer(timerId)
                updateTimerViews()
            }

            // Обработчик кнопки настроек
            optionsButton.setOnClickListener {
                showTimerOptionsDialog(timerId)
            }
        } catch (e: Exception) {
            Log.e("TimerActivity", "Ошибка в setupTimerControls для таймера $timerId: ${e.message}")
        }
    }

    // Показать диалог выбора длительности таймера
    private fun showDurationDialog(timerId: Int) {
        try {
            val options = arrayOf("5 минут", "10 минут", "15 минут", "30 минут", "45 минут", "1 час", "Другое...")

            AlertDialog.Builder(this)
                .setTitle("Выберите длительность")
                .setItems(options) { _, which ->
                    val durationInMinutes = when (which) {
                        0 -> 5
                        1 -> 10
                        2 -> 15
                        3 -> 30
                        4 -> 45
                        5 -> 60
                        6 -> {
                            showCustomDurationDialog(timerId)
                            return@setItems
                        }
                        else -> 5
                    }

                    val durationInMillis = TimeUnit.MINUTES.toMillis(durationInMinutes.toLong())
                    timerService?.startTimer(timerId, durationInMillis)
                    updateTimerViews()
                }
                .show()
        } catch (e: Exception) {
            Log.e("TimerActivity", "Ошибка в showDurationDialog: ${e.message}")
            Toast.makeText(
                this,
                "Ошибка при отображении диалога выбора длительности",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Диалог для ввода произвольной длительности
    private fun showCustomDurationDialog(timerId: Int) {
        try {
            val dialogView = layoutInflater.inflate(R.layout.dialog_custom_duration, null)
            val editTextMinutes = dialogView.findViewById<EditText>(R.id.editTextMinutes)

            AlertDialog.Builder(this)
                .setTitle("Введите длительность (минуты)")
                .setView(dialogView)
                .setPositiveButton("Начать") { _, _ ->
                    try {
                        val minutes = editTextMinutes.text.toString().toInt()
                        if (minutes > 0) {
                            val durationInMillis = TimeUnit.MINUTES.toMillis(minutes.toLong())
                            timerService?.startTimer(timerId, durationInMillis)
                            updateTimerViews()
                        } else {
                            Toast.makeText(this, "Пожалуйста, введите положительное число", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: NumberFormatException) {
                        Toast.makeText(this, "Пожалуйста, введите корректное число", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Отмена", null)
                .show()
        } catch (e: Exception) {
            Log.e("TimerActivity", "Ошибка в showCustomDurationDialog: ${e.message}")
            Toast.makeText(
                this,
                "Ошибка при отображении диалога ввода длительности",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Диалог настроек таймера
    private fun showTimerOptionsDialog(timerId: Int) {
        try {
            // Изменим список опций, чтобы временно исключить выбор звука
            val options = arrayOf("Изменить название", "Выбрать цвет")

            AlertDialog.Builder(this)
                .setTitle("Настройки таймера")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> showRenameTimerDialog(timerId)
                        1 -> showColorPickerDialog(timerId)
                        // Временно уберем выбор звука, так как он может вызывать проблемы
                        // 2 -> showSoundPickerDialog(timerId)
                    }
                }
                .show()
        } catch (e: Exception) {
            Log.e("TimerActivity", "Ошибка в showTimerOptionsDialog: ${e.message}")
            Toast.makeText(
                this,
                "Ошибка при отображении диалога настроек",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Диалог переименования таймера
    private fun showRenameTimerDialog(timerId: Int) {
        try {
            val dialogView = layoutInflater.inflate(R.layout.dialog_rename_timer, null)
            val editTextName = dialogView.findViewById<EditText>(R.id.editTextTimerName)
            editTextName.setText(timerService?.getTimerName(timerId) ?: "Таймер ${timerId + 1}")

            AlertDialog.Builder(this)
                .setTitle("Переименовать таймер")
                .setView(dialogView)
                .setPositiveButton("Сохранить") { _, _ ->
                    val newName = editTextName.text.toString().trim()
                    if (newName.isNotEmpty()) {
                        timerService?.setTimerName(timerId, newName)
                        updateTimerViews()
                    } else {
                        Toast.makeText(this, "Название не может быть пустым", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Отмена", null)
                .show()
        } catch (e: Exception) {
            Log.e("TimerActivity", "Ошибка в showRenameTimerDialog: ${e.message}")
            Toast.makeText(
                this,
                "Ошибка при отображении диалога переименования",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Диалог выбора цвета
    private fun showColorPickerDialog(timerId: Int) {
        try {
            val colors = arrayOf("Синий", "Зеленый", "Красный", "Оранжево-Желтый")
            val colorValues = arrayOf(
                Color.parseColor("#2196F3"), // Синий
                Color.parseColor("#4CAF50"), // Зеленый
                Color.parseColor("#F44336"), // Красный
                Color.parseColor("#FFC107")  // Оранжево-Желтый
            )

            AlertDialog.Builder(this)
                .setTitle("Выберите цвет")
                .setItems(colors) { _, which ->
                    timerService?.setTimerColor(timerId, colorValues[which])
                    updateTimerViews()
                }
                .show()
        } catch (e: Exception) {
            Log.e("TimerActivity", "Ошибка в showColorPickerDialog: ${e.message}")
            Toast.makeText(
                this,
                "Ошибка при отображении диалога выбора цвета",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun updateTimerViews() {
        try {
            if (!bound || timerService == null) return

            // Обновляем первый таймер
            updateTimerView(
                TimerService.TIMER_1,
                binding.cardTimer1,
                binding.textViewTimer1Name,
                binding.textViewTimer1,
                binding.buttonTimer1Start,
                binding.buttonTimer1Stop,
                binding.progressTimer1
            )

            // Обновляем второй таймер
            updateTimerView(
                TimerService.TIMER_2,
                binding.cardTimer2,
                binding.textViewTimer2Name,
                binding.textViewTimer2,
                binding.buttonTimer2Start,
                binding.buttonTimer2Stop,
                binding.progressTimer2
            )

            // Обновляем третий таймер
            updateTimerView(
                TimerService.TIMER_3,
                binding.cardTimer3,
                binding.textViewTimer3Name,
                binding.textViewTimer3,
                binding.buttonTimer3Start,
                binding.buttonTimer3Stop,
                binding.progressTimer3
            )

            // Обновляем четвертый таймер
            updateTimerView(
                TimerService.TIMER_4,
                binding.cardTimer4,
                binding.textViewTimer4Name,
                binding.textViewTimer4,
                binding.buttonTimer4Start,
                binding.buttonTimer4Stop,
                binding.progressTimer4
            )
        } catch (e: Exception) {
            Log.e("TimerActivity", "Ошибка в updateTimerViews: ${e.message}")
        }
    }

    private fun updateTimerView(
        timerId: Int,
        cardView: View,
        nameTextView: androidx.appcompat.widget.AppCompatTextView,
        timerTextView: androidx.appcompat.widget.AppCompatTextView,
        startButton: android.view.View,
        stopButton: android.view.View,
        progressBar: android.widget.ProgressBar
    ) {
        try {
            // Обновляем имя таймера
            nameTextView.text = timerService?.getTimerName(timerId) ?: "Таймер ${timerId + 1}"

            // Обновляем цвет таймера
            val timerColor = timerService?.getTimerColor(timerId) ?: Color.parseColor("#4CAF50")

            // Устанавливаем цвет элементов - безопасно проверяем тип фона
            try {
                val background = cardView.background
                if (background is android.graphics.drawable.GradientDrawable) {
                    background.setStroke(4, timerColor)
                }
            } catch (e: Exception) {
                Log.e("TimerActivity", "Ошибка установки цвета рамки: ${e.message}")
            }

            // Устанавливаем цвет прогресс-бара
            try {
                progressBar.progressTintList = android.content.res.ColorStateList.valueOf(timerColor)
            } catch (e: Exception) {
                Log.e("TimerActivity", "Ошибка установки цвета прогресс-бара: ${e.message}")
            }

            // Если таймер активен
            if (timerService?.isTimerRunning(timerId) == true) {
                // Отображаем время таймера
                timerTextView.text = timerService?.getFormattedTime(timerId) ?: "00:00"

                // Настраиваем видимость кнопок
                startButton.visibility = View.GONE
                stopButton.visibility = View.VISIBLE

                // Настраиваем прогресс
                val progress = timerService?.getTimerProgressPercent(timerId) ?: 0
                progressBar.progress = progress
                progressBar.visibility = View.VISIBLE

                // Обновляем основной текст цветом таймера
                timerTextView.setTextColor(timerColor)

                // Подписываемся на обновления таймера
                try {
                    timerService?.getTimerLiveData(timerId)?.observe(this, Observer { timeRemaining ->
                        if (timeRemaining <= 0) {
                            // Таймер завершен
                            timerTextView.text = "00:00"
                            startButton.visibility = View.VISIBLE
                            stopButton.visibility = View.GONE
                            progressBar.progress = 0
                        } else {
                            // Обновляем отображение
                            timerTextView.text = formatTime(timeRemaining)
                            progressBar.progress = timerService?.getTimerProgressPercent(timerId) ?: 0
                        }
                    })
                } catch (e: Exception) {
                    Log.e("TimerActivity", "Ошибка наблюдения за таймером: ${e.message}")
                }
            } else {
                // Таймер неактивен
                timerTextView.text = "00:00"
                startButton.visibility = View.VISIBLE
                stopButton.visibility = View.GONE
                progressBar.progress = 0
                timerTextView.setTextColor(Color.GRAY)
            }
        } catch (e: Exception) {
            Log.e("TimerActivity", "Ошибка в updateTimerView для таймера $timerId: ${e.message}")
        }
    }

    private fun formatTime(timeInMillis: Long): String {
        try {
            val hours = TimeUnit.MILLISECONDS.toHours(timeInMillis)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMillis) % 60
            val seconds = TimeUnit.MILLISECONDS.toSeconds(timeInMillis) % 60

            return if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        } catch (e: Exception) {
            Log.e("TimerActivity", "Ошибка в formatTime: ${e.message}")
            return "00:00"
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            if (bound) {
                updateTimerViews()
            }
        } catch (e: Exception) {
            Log.e("TimerActivity", "Ошибка в onResume: ${e.message}")
        }
    }

    override fun onDestroy() {
        try {
            super.onDestroy()
            if (bound) {
                unbindService(connection)
                bound = false
            }
        } catch (e: Exception) {
            Log.e("TimerActivity", "Ошибка в onDestroy: ${e.message}")
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return try {
            when (item.itemId) {
                android.R.id.home -> {
                    AnimationHelper.finishWithAnimation(this)
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
        } catch (e: Exception) {
            Log.e("TimerActivity", "Ошибка в onOptionsItemSelected: ${e.message}")
            finish()
            true
        }
    }
}