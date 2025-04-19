package com.example.driftnotes.timer

import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.example.driftnotes.R
import com.example.driftnotes.databinding.ActivityTimerBinding
import java.util.concurrent.TimeUnit

class TimerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTimerBinding
    private var timerService: TimerService? = null
    private var bound = false

    // Добавляем Handler для выполнения задач в UI потоке
    private val handler = Handler(Looper.getMainLooper())

    // Для предотвращения множественных нажатий
    private var isBusy = false

    // Связь с сервисом
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as TimerService.LocalBinder
            timerService = binder.getService()
            bound = true
            updateAllTimerViews()
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
            binding.textViewTimer1Name,
            binding.buttonTimer1Start,
            binding.buttonTimer1Stop,
            binding.buttonTimer1Reset,
            binding.buttonTimer1Options
        )

        // Настройка второго таймера
        setupTimerControls(
            TimerService.TIMER_2,
            binding.textViewTimer2Name,
            binding.buttonTimer2Start,
            binding.buttonTimer2Stop,
            binding.buttonTimer2Reset,
            binding.buttonTimer2Options
        )

        // Настройка третьего таймера
        setupTimerControls(
            TimerService.TIMER_3,
            binding.textViewTimer3Name,
            binding.buttonTimer3Start,
            binding.buttonTimer3Stop,
            binding.buttonTimer3Reset,
            binding.buttonTimer3Options
        )

        // Настройка четвертого таймера
        setupTimerControls(
            TimerService.TIMER_4,
            binding.textViewTimer4Name,
            binding.buttonTimer4Start,
            binding.buttonTimer4Stop,
            binding.buttonTimer4Reset,
            binding.buttonTimer4Options
        )
    }

    private fun setupTimerControls(
        timerId: Int,
        nameTextView: androidx.appcompat.widget.AppCompatTextView,
        startButton: com.google.android.material.button.MaterialButton,
        stopButton: com.google.android.material.button.MaterialButton,
        resetButton: com.google.android.material.button.MaterialButton,
        optionsButton: com.google.android.material.button.MaterialButton
    ) {
        // Обработчик кнопки Старт
        startButton.setOnClickListener {
            if (!isBusy) {
                isBusy = true
                showDurationDialog(timerId)
            }
        }

        // Обработчик кнопки Стоп
        stopButton.setOnClickListener {
            if (!isBusy) {
                isBusy = true
                // Выполняем операцию в отдельном потоке, чтобы не блокировать UI
                Thread {
                    try {
                        timerService?.stopTimer(timerId)
                        // Обновляем UI в основном потоке
                        handler.post {
                            updateAllTimerViews()
                            isBusy = false
                        }
                    } catch (e: Exception) {
                        handler.post {
                            Toast.makeText(this@TimerActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                            isBusy = false
                        }
                    }
                }.start()
            }
        }

        // Обработчик кнопки Сброс
        resetButton.setOnClickListener {
            if (!isBusy) {
                isBusy = true
                // Выполняем операцию в отдельном потоке, чтобы не блокировать UI
                Thread {
                    try {
                        timerService?.resetTimer(timerId)
                        // Обновляем UI в основном потоке
                        handler.post {
                            updateAllTimerViews()
                            isBusy = false
                        }
                    } catch (e: Exception) {
                        handler.post {
                            Toast.makeText(this@TimerActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                            isBusy = false
                        }
                    }
                }.start()
            }
        }

        // Обработчик кнопки настроек
        optionsButton.setOnClickListener {
            if (!isBusy) {
                isBusy = true
                showTimerOptionsDialog(timerId)
            }
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
                        isBusy = false
                        return@setItems
                    }
                    else -> 5
                }

                // Запускаем таймер в отдельном потоке
                Thread {
                    try {
                        val durationInMillis = TimeUnit.MINUTES.toMillis(durationInMinutes.toLong())
                        timerService?.startTimer(timerId, durationInMillis)

                        // Обновляем UI в основном потоке
                        handler.post {
                            updateAllTimerViews()
                            isBusy = false
                        }
                    } catch (e: Exception) {
                        handler.post {
                            Toast.makeText(this, "Ошибка запуска таймера: ${e.message}", Toast.LENGTH_SHORT).show()
                            isBusy = false
                        }
                    }
                }.start()
            }
            .setOnCancelListener {
                isBusy = false
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
                        // Запускаем таймер в отдельном потоке
                        Thread {
                            try {
                                val durationInMillis = TimeUnit.MINUTES.toMillis(minutes.toLong())
                                timerService?.startTimer(timerId, durationInMillis)

                                // Обновляем UI в основном потоке
                                handler.post {
                                    updateAllTimerViews()
                                    isBusy = false
                                }
                            } catch (e: Exception) {
                                handler.post {
                                    Toast.makeText(this, "Ошибка запуска таймера: ${e.message}", Toast.LENGTH_SHORT).show()
                                    isBusy = false
                                }
                            }
                        }.start()
                    } else {
                        Toast.makeText(this, "Пожалуйста, введите положительное число", Toast.LENGTH_SHORT).show()
                        isBusy = false
                    }
                } catch (e: NumberFormatException) {
                    Toast.makeText(this, "Пожалуйста, введите корректное число", Toast.LENGTH_SHORT).show()
                    isBusy = false
                }
            }
            .setNegativeButton("Отмена") { _, _ ->
                isBusy = false
            }
            .setOnCancelListener {
                isBusy = false
            }
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
            .setOnCancelListener {
                isBusy = false
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
                    // Переименование в отдельном потоке
                    Thread {
                        try {
                            timerService?.setTimerName(timerId, newName)
                            handler.post {
                                updateAllTimerViews()
                                isBusy = false
                            }
                        } catch (e: Exception) {
                            handler.post {
                                Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                                isBusy = false
                            }
                        }
                    }.start()
                } else {
                    Toast.makeText(this, "Название не может быть пустым", Toast.LENGTH_SHORT).show()
                    isBusy = false
                }
            }
            .setNegativeButton("Отмена") { _, _ ->
                isBusy = false
            }
            .setOnCancelListener {
                isBusy = false
            }
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
                // Установка цвета в отдельном потоке
                Thread {
                    try {
                        timerService?.setTimerColor(timerId, colorValues[which])
                        handler.post {
                            updateAllTimerViews()
                            isBusy = false
                        }
                    } catch (e: Exception) {
                        handler.post {
                            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                            isBusy = false
                        }
                    }
                }.start()
            }
            .setOnCancelListener {
                isBusy = false
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
                // Установка звука в отдельном потоке
                Thread {
                    try {
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

                        handler.post {
                            isBusy = false
                        }
                    } catch (e: Exception) {
                        handler.post {
                            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                            isBusy = false
                        }
                    }
                }.start()
            }
            .setOnCancelListener {
                isBusy = false
            }
            .show()
    }

    // Обновляет все таймеры сразу
    private fun updateAllTimerViews() {
        if (!bound || timerService == null) return

        updateTimerView(TimerService.TIMER_1)
        updateTimerView(TimerService.TIMER_2)
        updateTimerView(TimerService.TIMER_3)
        updateTimerView(TimerService.TIMER_4)
    }

    // Обновляет один таймер
    private fun updateTimerView(timerId: Int) {
        if (!bound || timerService == null) return

        // Получаем ссылки на элементы UI для конкретного таймера
        val (cardView, nameTextView, timerTextView, startButton, stopButton, resetButton, progressBar) = when (timerId) {
            TimerService.TIMER_1 -> Tuple(
                binding.cardTimer1,
                binding.textViewTimer1Name,
                binding.textViewTimer1,
                binding.buttonTimer1Start,
                binding.buttonTimer1Stop,
                binding.buttonTimer1Reset,
                binding.progressTimer1
            )
            TimerService.TIMER_2 -> Tuple(
                binding.cardTimer2,
                binding.textViewTimer2Name,
                binding.textViewTimer2,
                binding.buttonTimer2Start,
                binding.buttonTimer2Stop,
                binding.buttonTimer2Reset,
                binding.progressTimer2
            )
            TimerService.TIMER_3 -> Tuple(
                binding.cardTimer3,
                binding.textViewTimer3Name,
                binding.textViewTimer3,
                binding.buttonTimer3Start,
                binding.buttonTimer3Stop,
                binding.buttonTimer3Reset,
                binding.progressTimer3
            )
            TimerService.TIMER_4 -> Tuple(
                binding.cardTimer4,
                binding.textViewTimer4Name,
                binding.textViewTimer4,
                binding.buttonTimer4Start,
                binding.buttonTimer4Stop,
                binding.buttonTimer4Reset,
                binding.progressTimer4
            )
            else -> return
        }

        // Обновляем имя таймера
        nameTextView.text = timerService?.getTimerName(timerId) ?: "Таймер ${timerId + 1}"

        // Обновляем цвет таймера
        val timerColor = timerService?.getTimerColor(timerId) ?: Color.parseColor("#4CAF50")

        // Устанавливаем цвет элементов
        val background = cardView.background
        if (background is android.graphics.drawable.GradientDrawable) {
            background.setStroke(4, timerColor)
        }

        progressBar.progressTintList = android.content.res.ColorStateList.valueOf(timerColor)

        // Определяем состояние таймера
        val isRunning = timerService?.isTimerRunning(timerId) == true
        val remainingTime = timerService?.getTimerLiveData(timerId)?.value ?: 0L

        // Обновляем видимость кнопок в зависимости от состояния
        if (isRunning) {
            // Таймер запущен
            startButton.visibility = View.GONE
            stopButton.visibility = View.VISIBLE
            resetButton.visibility = View.VISIBLE
            timerTextView.text = timerService?.getFormattedTime(timerId) ?: "00:00"
            timerTextView.setTextColor(timerColor)
            progressBar.progress = timerService?.getTimerProgressPercent(timerId) ?: 0
        } else if (remainingTime > 0) {
            // Таймер на паузе
            startButton.visibility = View.VISIBLE
            stopButton.visibility = View.GONE
            resetButton.visibility = View.VISIBLE
            timerTextView.text = formatTime(remainingTime)
            timerTextView.setTextColor(timerColor)
            progressBar.progress = 0
        } else {
            // Таймер неактивен или сброшен
            startButton.visibility = View.VISIBLE
            stopButton.visibility = View.GONE
            resetButton.visibility = View.GONE
            timerTextView.text = "00:00"
            timerTextView.setTextColor(Color.GRAY)
            progressBar.progress = 0
        }

        // Наблюдаем за изменениями таймера (только если он запущен)
        if (isRunning) {
            // Удаляем старых наблюдателей, чтобы избежать утечек памяти
            timerService?.getTimerLiveData(timerId)?.removeObservers(this)

            // Добавляем нового наблюдателя
            timerService?.getTimerLiveData(timerId)?.observe(this, Observer { timeRemaining ->
                try {
                    if (timeRemaining <= 0) {
                        // Таймер завершился - обновляем все элементы
                        updateAllTimerViews()
                    } else {
                        // Обновляем только отображение времени и прогресс
                        timerTextView.text = formatTime(timeRemaining)
                        progressBar.progress = timerService?.getTimerProgressPercent(timerId) ?: 0
                    }
                } catch (e: Exception) {
                    // Игнорируем ошибки при обновлении UI
                }
            })
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

    // Вспомогательный класс для удобной работы с несколькими возвращаемыми значениями
    private data class Tuple(
        val cardView: View,
        val nameTextView: androidx.appcompat.widget.AppCompatTextView,
        val timerTextView: androidx.appcompat.widget.AppCompatTextView,
        val startButton: View,
        val stopButton: View,
        val resetButton: View,
        val progressBar: android.widget.ProgressBar
    )

    override fun onResume() {
        super.onResume()
        if (bound) {
            try {
                updateAllTimerViews()
            } catch (e: Exception) {
                // Игнорируем ошибки
            }
        }
    }

    override fun onPause() {
        super.onPause()

        try {
            // Убираем всех наблюдателей, чтобы избежать утечек памяти
            if (bound && timerService != null) {
                for (i in 0 until 4) {
                    timerService?.getTimerLiveData(i)?.removeObservers(this)
                }
            }
        } catch (e: Exception) {
            // Игнорируем ошибки
        }
    }

    override fun onStop() {
        super.onStop()

        try {
            // Убираем всех наблюдателей при остановке активности
            if (bound && timerService != null) {
                for (i in 0 until 4) {
                    timerService?.getTimerLiveData(i)?.removeObservers(this)
                }
            }

            // Отменяем все задачи handler
            handler.removeCallbacksAndMessages(null)
        } catch (e: Exception) {
            // Игнорируем ошибки
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            // Корректно отсоединяем сервис
            if (bound) {
                try {
                    unbindService(connection)
                } catch (e: Exception) {
                    // Игнорируем ошибки при отсоединении
                }
                bound = false
            }

            // Отменяем все задачи handler
            handler.removeCallbacksAndMessages(null)
        } catch (e: Exception) {
            // Игнорируем ошибки
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish() // Просто завершаем активность без анимации
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        finish() // Просто завершаем активность без анимации
    }
}