package com.example.driftnotes.timer

import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.os.Bundle
import android.os.IBinder
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
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
            val binder = service as TimerService.LocalBinder
            timerService = binder.getService()
            bound = true
            updateTimerViews()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            timerService = null
            bound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    }

    private fun setupTimerButtons() {
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
    }

    // Показать диалог выбора длительности таймера
    private fun showDurationDialog(timerId: Int) {
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
    }

    // Диалог для ввода произвольной длительности
    private fun showCustomDurationDialog(timerId: Int) {
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
    }

    // Диалог настроек таймера
    private fun showTimerOptionsDialog(timerId: Int) {
        val options = arrayOf("Изменить название", "Выбрать цвет", "Выбрать звук")

        AlertDialog.Builder(this)
            .setTitle("Настройки таймера")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showRenameTimerDialog(timerId)
                    1 -> showColorPickerDialog(timerId)
                    2 -> showSoundPickerDialog(timerId)
                }
            }
            .show()
    }

    // Диалог переименования таймера
    private fun showRenameTimerDialog(timerId: Int) {
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
    }

    // Диалог выбора цвета
    private fun showColorPickerDialog(timerId: Int) {
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
    }

    // Диалог выбора звука
    private fun showSoundPickerDialog(timerId: Int) {
        val sounds = arrayOf("Звонок", "Пинг", "Мелодия 1", "Мелодия 2", "Вибрация (без звука)")
        val soundResources = arrayOf(
            R.raw.timer_bell,     // Замените на реальные
            R.raw.timer_ping,     // ресурсы звуков
            R.raw.timer_melody1,  // в вашем проекте
            R.raw.timer_melody2,
            0 // Только вибрация
        )

        AlertDialog.Builder(this)
            .setTitle("Выберите звук")
            .setItems(sounds) { _, which ->
                timerService?.setTimerSound(timerId, soundResources[which])

                // Воспроизводим предпросмотр звука, кроме режима вибрации
                if (soundResources[which] != 0) {
                    timerService?.playSound(soundResources[which])
                } else {
                    // Вибрация
                    val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        vibrator.vibrate(android.os.VibrationEffect.createOneShot(500, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(500)
                    }
                }
            }
            .show()
    }

    private fun updateTimerViews() {
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
        // Обновляем имя таймера
        nameTextView.text = timerService?.getTimerName(timerId) ?: "Таймер ${timerId + 1}"

        // Обновляем цвет таймера
        val timerColor = timerService?.getTimerColor(timerId) ?: Color.parseColor("#4CAF50")

        // Устанавливаем цвет элементов
        when (val background = cardView.background) {
            is android.graphics.drawable.GradientDrawable -> {
                background.setStroke(4, timerColor)
            }
        }

        progressBar.progressTintList = android.content.res.ColorStateList.valueOf(timerColor)

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
        } else {
            // Таймер неактивен
            timerTextView.text = "00:00"
            startButton.visibility = View.VISIBLE
            stopButton.visibility = View.GONE
            progressBar.progress = 0
            timerTextView.setTextColor(Color.GRAY)
        }
    }

    private fun formatTime(timeInMillis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(timeInMillis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMillis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeInMillis) % 60

        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    override fun onResume() {
        super.onResume()
        if (bound) {
            updateTimerViews()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (bound) {
            unbindService(connection)
            bound = false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                AnimationHelper.finishWithAnimation(this)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        AnimationHelper.finishWithAnimation(this)
    }
}