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
            Log.d("TimerActivity", "onServiceConnected: Сервис подключен")
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

        try {
            // Настройка ActionBar
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = "Таймеры"

            // Запускаем сервис и привязываемся к нему
            val intent = Intent(this, TimerService::class.java)
            startService(intent)
            bindService(intent, connection, Context.BIND_AUTO_CREATE)

            // Настраиваем обработчики кнопок для каждого таймера
            setupTimerButtons()
        } catch (e: Exception) {
            Log.e("TimerActivity", "Ошибка в onCreate: ${e.message}", e)
            Toast.makeText(this, "Ошибка инициализации: ${e.message}", Toast.LENGTH_SHORT).show()
        }

        Log.d("TimerActivity", "onCreate: Инициализация TimerActivity завершена")
    }

    private fun setupTimerButtons() {
        try {
            // Настройка первого таймера
            setupTimerControls(
                TimerService.TIMER_1,
                binding.textViewTimer1Name,
                binding.textViewTimer1,
                binding.progressTimer1,
                binding.buttonTimer1Start,
                binding.buttonTimer1Stop,
                binding.buttonTimer1Reset,
                binding.buttonTimer1Options,
                binding.cardTimer1
            )

            // Настройка второго таймера
            setupTimerControls(
                TimerService.TIMER_2,
                binding.textViewTimer2Name,
                binding.textViewTimer2,
                binding.progressTimer2,
                binding.buttonTimer2Start,
                binding.buttonTimer2Stop,
                binding.buttonTimer2Reset,
                binding.buttonTimer2Options,
                binding.cardTimer2
            )

            // Настройка третьего таймера
            setupTimerControls(
                TimerService.TIMER_3,
                binding.textViewTimer3Name,
                binding.textViewTimer3,
                binding.progressTimer3,
                binding.buttonTimer3Start,
                binding.buttonTimer3Stop,
                binding.buttonTimer3Reset,
                binding.buttonTimer3Options,
                binding.cardTimer3
            )

            // Настройка четвертого таймера
            setupTimerControls(
                TimerService.TIMER_4,
                binding.textViewTimer4Name,
                binding.textViewTimer4,
                binding.progressTimer4,
                binding.buttonTimer4Start,
                binding.buttonTimer4Stop,
                binding.buttonTimer4Reset,
                binding.buttonTimer4Options,
                binding.cardTimer4
            )
        } catch (e: Exception) {
            Log.e("TimerActivity", "Ошибка при настройке кнопок таймеров: ${e.message}", e)
        }
    }

    private fun setupTimerControls(
        timerId: Int,
        nameTextView: androidx.appcompat.widget.AppCompatTextView,
        timerTextView: androidx.appcompat.widget.AppCompatTextView,
        progressBar: android.widget.ProgressBar,
        startButton: com.google.android.material.button.MaterialButton,
        stopButton: com.google.android.material.button.MaterialButton,
        resetButton: com.google.android.material.button.MaterialButton,
        optionsButton: com.google.android.material.button.MaterialButton,
        cardView: androidx.cardview.widget.CardView
    ) {
        try {
            // Обработчик кнопки Старт
            startButton.setOnClickListener {
                try {
                    showDurationDialog(timerId)
                } catch (e: Exception) {
                    Log.e("TimerActivity", "Ошибка в обработчике Start кнопки: ${e.message}", e)
                    Toast.makeText(this, "Не удалось запустить таймер", Toast.LENGTH_SHORT).show()
                }
            }

            // Обработчик кнопки Стоп
            stopButton.setOnClickListener {
                try {
                    timerService?.stopTimer(timerId)
                    updateTimerView(timerId)
                } catch (e: Exception) {
                    Log.e("TimerActivity", "Ошибка в обработчике Stop кнопки: ${e.message}", e)
                    Toast.makeText(this, "Не удалось остановить таймер", Toast.LENGTH_SHORT).show()

                    // Экстренное обновление UI
                    stopButton.visibility = View.GONE
                    startButton.visibility = View.VISIBLE
                }
            }

            // Обработчик кнопки Сброс
            resetButton.setOnClickListener {
                try {
                    timerService?.resetTimer(timerId)
                    updateTimerView(timerId)
                } catch (e: Exception) {
                    Log.e("TimerActivity", "Ошибка в обработчике Reset кнопки: ${e.message}", e)
                    Toast.makeText(this, "Не удалось сбросить таймер", Toast.LENGTH_SHORT).show()

                    // Экстренное обновление UI
                    timerTextView.text = "00:00"
                    progressBar.progress = 0
                }
            }

            // Обработчик кнопки настроек
            optionsButton.setOnClickListener {
                try {
                    showTimerOptionsDialog(timerId)
                } catch (e: Exception) {
                    Log.e("TimerActivity", "Ошибка в обработчике Options кнопки: ${e.message}", e)
                    Toast.makeText(this, "Не удалось открыть настройки", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e("TimerActivity", "Ошибка при настройке обработчиков таймера $timerId: ${e.message}", e)
        }
    }

    // Показать диалог выбора длительности таймера
    private fun showDurationDialog(timerId: Int) {
        val options = arrayOf("5 минут", "10 минут", "15 минут", "30 минут", "45 минут", "1 час", "Другое...")

        try {
            AlertDialog.Builder(this)
                .setTitle("Выберите длительность")
                .setItems(options) { dialog, which ->
                    try {
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
                        updateTimerView(timerId)
                    } catch (e: Exception) {
                        Log.e("TimerActivity", "Ошибка при выборе длительности: ${e.message}", e)
                        Toast.makeText(this, "Не удалось установить длительность", Toast.LENGTH_SHORT).show()
                    }
                }
                .show()
        } catch (e: Exception) {
            Log.e("TimerActivity", "Ошибка при отображении диалога: ${e.message}", e)
            Toast.makeText(this, "Не удалось отобразить диалог: ${e.message}", Toast.LENGTH_SHORT).show()
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
                        val minutes = editTextMinutes.text.toString().toIntOrNull() ?: 0
                        if (minutes > 0) {
                            val durationInMillis = TimeUnit.MINUTES.toMillis(minutes.toLong())
                            timerService?.startTimer(timerId, durationInMillis)
                            updateTimerView(timerId)
                        } else {
                            Toast.makeText(this, "Пожалуйста, введите положительное число", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Log.e("TimerActivity", "Ошибка при обработке ввода длительности: ${e.message}", e)
                        Toast.makeText(this, "Ошибка при установке длительности", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Отмена", null)
                .show()
        } catch (e: Exception) {
            Log.e("TimerActivity", "Ошибка при отображении диалога произвольной длительности: ${e.message}", e)
            Toast.makeText(this, "Не удалось отобразить диалог: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Диалог настроек таймера
    private fun showTimerOptionsDialog(timerId: Int) {
        try {
            val options = arrayOf("Изменить название", "Выбрать цвет", "Выбрать звук")

            AlertDialog.Builder(this)
                .setTitle("Настройки таймера")
                .setItems(options) { _, which ->
                    try {
                        when (which) {
                            0 -> showRenameTimerDialog(timerId)
                            1 -> showColorPickerDialog(timerId)
                            2 -> showSoundPickerDialog(timerId)
                        }
                    } catch (e: Exception) {
                        Log.e("TimerActivity", "Ошибка при обработке выбора опции: ${e.message}", e)
                        Toast.makeText(this, "Не удалось выполнить действие", Toast.LENGTH_SHORT).show()
                    }
                }
                .show()
        } catch (e: Exception) {
            Log.e("TimerActivity", "Ошибка при отображении диалога настроек: ${e.message}", e)
            Toast.makeText(this, "Не удалось отобразить настройки", Toast.LENGTH_SHORT).show()
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
                    try {
                        val newName = editTextName.text.toString().trim()
                        if (newName.isNotEmpty()) {
                            timerService?.setTimerName(timerId, newName)
                            updateTimerView(timerId)
                        } else {
                            Toast.makeText(this, "Название не может быть пустым", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Log.e("TimerActivity", "Ошибка при переименовании таймера: ${e.message}", e)
                        Toast.makeText(this, "Не удалось переименовать таймер", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Отмена", null)
                .show()
        } catch (e: Exception) {
            Log.e("TimerActivity", "Ошибка при отображении диалога переименования: ${e.message}", e)
            Toast.makeText(this, "Не удалось отобразить диалог переименования", Toast.LENGTH_SHORT).show()
        }
    }

    // Диалог выбора цвета
    private fun showColorPickerDialog(timerId: Int) {
        try {
            val colors = arrayOf("Синий", "Зеленый", "Красный", "Оранжевый")
            val colorValues = arrayOf(
                Color.parseColor("#2196F3"), // Синий
                Color.parseColor("#4CAF50"), // Зеленый
                Color.parseColor("#F44336"), // Красный
                Color.parseColor("#FF9800")  // Оранжевый
            )

            AlertDialog.Builder(this)
                .setTitle("Выберите цвет")
                .setItems(colors) { _, which ->
                    try {
                        timerService?.setTimerColor(timerId, colorValues[which])
                        updateTimerView(timerId)
                    } catch (e: Exception) {
                        Log.e("TimerActivity", "Ошибка при установке цвета таймера: ${e.message}", e)
                        Toast.makeText(this, "Не удалось изменить цвет", Toast.LENGTH_SHORT).show()
                    }
                }
                .show()
        } catch (e: Exception) {
            Log.e("TimerActivity", "Ошибка при отображении диалога выбора цвета: ${e.message}", e)
            Toast.makeText(this, "Не удалось отобразить выбор цвета", Toast.LENGTH_SHORT).show()
        }
    }

    // Диалог выбора звука
    private fun showSoundPickerDialog(timerId: Int) {
        try {
            val sounds = arrayOf("Звонок", "Пинг", "Мелодия 1", "Мелодия 2")
            val soundResources = arrayOf(
                R.raw.timer_bell,
                R.raw.timer_bell,  // Повторно используем один и тот же файл
                R.raw.timer_bell,  // если другие файлы недоступны
                R.raw.timer_bell
            )

            AlertDialog.Builder(this)
                .setTitle("Выберите звук")
                .setItems(sounds) { _, which ->
                    try {
                        timerService?.setTimerSound(timerId, soundResources[which])

                        // Воспроизводим предпросмотр звука
                        timerService?.playSound(soundResources[which])
                    } catch (e: Exception) {
                        Log.e("TimerActivity", "Ошибка при установке звука таймера: ${e.message}", e)
                        Toast.makeText(this, "Не удалось изменить звук", Toast.LENGTH_SHORT).show()
                    }
                }
                .show()
        } catch (e: Exception) {
            Log.e("TimerActivity", "Ошибка при отображении диалога выбора звука: ${e.message}", e)
            Toast.makeText(this, "Не удалось отобразить выбор звука", Toast.LENGTH_SHORT).show()
        }
    }

    // Обновляет все таймеры сразу
    private fun updateAllTimerViews() {
        if (!bound || timerService == null) return

        try {
            updateTimerView(TimerService.TIMER_1)
            updateTimerView(TimerService.TIMER_2)
            updateTimerView(TimerService.TIMER_3)
            updateTimerView(TimerService.TIMER_4)
        } catch (e: Exception) {
            Log.e("TimerActivity", "Ошибка при обновлении всех таймеров: ${e.message}", e)
            Toast.makeText(this, "Ошибка обновления таймеров", Toast.LENGTH_SHORT).show()
        }
    }

    // Обновляет один таймер
    private fun updateTimerView(timerId: Int) {
        if (!bound || timerService == null) return

        try {
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
            timerTextView.setTextColor(timerColor)
            progressBar.progressTintList = android.content.res.ColorStateList.valueOf(timerColor)

            // Определяем состояние таймера
            val isRunning = timerService?.isTimerRunning(timerId) == true
            val timeRemaining = timerService?.getTimerLiveData(timerId)?.value ?: 0L

            // Обновляем видимость кнопок в зависимости от состояния
            if (isRunning) {
                // Таймер запущен
                startButton.visibility = View.GONE
                stopButton.visibility = View.VISIBLE
                timerTextView.text = timerService?.getFormattedTime(timerId) ?: "00:00"
                progressBar.progress = timerService?.getTimerProgressPercent(timerId) ?: 0
            } else if (timeRemaining > 0) {
                // Таймер на паузе
                startButton.visibility = View.VISIBLE
                stopButton.visibility = View.GONE
                timerTextView.text = formatTime(timeRemaining)
                progressBar.progress = timerService?.getTimerProgressPercent(timerId) ?: 0
            } else {
                // Таймер неактивен или сброшен
                startButton.visibility = View.VISIBLE
                stopButton.visibility = View.GONE
                timerTextView.text = "00:00"
                progressBar.progress = 0
            }

            // Удаляем предыдущие наблюдатели, чтобы избежать дублирования
            try {
                timerService?.getTimerLiveData(timerId)?.removeObservers(this)
            } catch (e: Exception) {
                Log.e("TimerActivity", "Ошибка при удалении наблюдателей: ${e.message}", e)
            }

            // Наблюдаем за изменениями таймера
            try {
                timerService?.getTimerLiveData(timerId)?.observe(this, Observer { time ->
                    try {
                        if (time <= 0) {
                            // Таймер завершился - обновляем все элементы
                            startButton.visibility = View.VISIBLE
                            stopButton.visibility = View.GONE
                            timerTextView.text = "00:00"
                            progressBar.progress = 0
                        } else {
                            // Обновляем только отображение времени и прогресс
                            timerTextView.text = formatTime(time)
                            progressBar.progress = timerService?.getTimerProgressPercent(timerId) ?: 0
                        }
                    } catch (e: Exception) {
                        Log.e("TimerActivity", "Ошибка при обновлении UI: ${e.message}", e)
                    }
                })
            } catch (e: Exception) {
                Log.e("TimerActivity", "Ошибка при установке наблюдателя: ${e.message}", e)
            }

        } catch (e: Exception) {
            Log.e("TimerActivity", "Ошибка при обновлении таймера $timerId: ${e.message}", e)
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
            Log.e("TimerActivity", "Ошибка при форматировании времени", e)
            return "00:00"
        }
    }

    // Вспомогательный класс для удобной работы с несколькими возвращаемыми значениями
    private data class Tuple(
        val cardView: androidx.cardview.widget.CardView,
        val nameTextView: androidx.appcompat.widget.AppCompatTextView,
        val timerTextView: androidx.appcompat.widget.AppCompatTextView,
        val startButton: com.google.android.material.button.MaterialButton,
        val stopButton: com.google.android.material.button.MaterialButton,
        val resetButton: com.google.android.material.button.MaterialButton,
        val progressBar: android.widget.ProgressBar
    )

    override fun onResume() {
        super.onResume()
        try {
            if (bound) {
                updateAllTimerViews()
            } else {
                // Если не привязаны к сервису, повторяем привязку
                val intent = Intent(this, TimerService::class.java)
                bindService(intent, connection, Context.BIND_AUTO_CREATE)
            }
        } catch (e: Exception) {
            Log.e("TimerActivity", "Ошибка в onResume: ${e.message}", e)
        }
    }

    override fun onStop() {
        super.onStop()
        // Важно сохранить сервис работающим, даже когда активность остановлена
    }

    override fun onDestroy() {
        super.onDestroy()

        // Корректно отсоединяем сервис
        try {
            if (bound) {
                try {
                    unbindService(connection)
                    bound = false
                } catch (e: Exception) {
                    Log.e("TimerActivity", "Ошибка при отключении от сервиса", e)
                }
            }
        } catch (e: Exception) {
            Log.e("TimerActivity", "Ошибка в onDestroy: ${e.message}", e)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                try {
                    AnimationHelper.finishWithAnimation(this)
                } catch (e: Exception) {
                    finish()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Для Android 13+ (API 33+)
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        try {
            AnimationHelper.finishWithAnimation(this)
        } catch (e: Exception) {
            finish()
        }
    }
}