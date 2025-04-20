package com.example.driftnotes.timer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.driftnotes.R
import java.util.concurrent.TimeUnit

class TimerService : Service() {

    companion object {
        const val TIMER_1 = 0
        const val TIMER_2 = 1
        const val TIMER_3 = 2
        const val TIMER_4 = 3

        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "TimerServiceChannel"
    }

    private val binder = LocalBinder()
    private lateinit var timers: Array<TimerInfo>
    private var mediaPlayer: MediaPlayer? = null

    // Класс для хранения информации о таймере
    inner class TimerInfo(val timerId: Int) {
        var name: String = "Таймер ${timerId + 1}"
        var color: Int = Color.parseColor("#4CAF50") // Зеленый по умолчанию
        var soundResId: Int = R.raw.timer_bell // Звук по умолчанию

        var duration: Long = 0
        var timeRemaining: Long = 0
        var running: Boolean = false
        var countDownTimer: CountDownTimer? = null

        val timeRemainingLiveData = MutableLiveData<Long>()

        // Функция для проверки состояния таймера
        fun isRunning(): Boolean = running
    }

    inner class LocalBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    override fun onCreate() {
        super.onCreate()
        // Инициализируем массив таймеров
        timers = Array(4) { TimerInfo(it) }
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        Log.d("TimerService", "Service created")
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("TimerService", "onStartCommand: Запуск сервиса с flags=$flags, startId=$startId")

        // Проверка, не является ли это намерением остановки или сброса таймера
        intent?.let {
            when(intent.action) {
                "STOP_TIMER" -> {
                    val timerId = intent.getIntExtra("TIMER_ID", -1)
                    if (timerId != -1) {
                        stopTimer(timerId)
                    }
                }
                "RESET_TIMER" -> {
                    val timerId = intent.getIntExtra("TIMER_ID", -1)
                    if (timerId != -1) {
                        resetTimer(timerId)
                    }
                }
            }
        }

        return START_STICKY
    }

    // Создание канала уведомлений (для Android 8.0+)
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Таймеры",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "Канал для отображения работающих таймеров"

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // Создание уведомления
    private fun createNotification(timerId: Int = -1): Notification {
        val notificationIntent = Intent(this, TimerActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        val contentTitle = if (timerId != -1 && timerId < timers.size) {
            "Таймер: ${timers[timerId].name}"
        } else {
            "Таймеры"
        }

        val contentText = if (timerId != -1 && timerId < timers.size) {
            "Осталось: ${formatTime(timers[timerId].timeRemaining)}"
        } else {
            "Следите за своими таймерами"
        }

        // Создание действия для остановки таймера
        val stopIntent = Intent(this, TimerService::class.java).apply {
            action = "STOP_TIMER"
            putExtra("TIMER_ID", timerId)
        }

        val stopPendingIntent = PendingIntent.getService(
            this,
            timerId, // Разные requestCode для разных таймеров
            stopIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        // Создание действия для сброса таймера
        val resetIntent = Intent(this, TimerService::class.java).apply {
            action = "RESET_TIMER"
            putExtra("TIMER_ID", timerId)
        }

        val resetPendingIntent = PendingIntent.getService(
            this,
            100 + timerId, // Другой requestCode для сброса
            resetIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_bottom_timer)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(true)

        // Добавляем кнопки только если это уведомление для конкретного таймера
        if (timerId != -1 && timerId < timers.size) {
            builder.addAction(
                android.R.drawable.ic_media_pause,
                "Стоп",
                stopPendingIntent
            )
            builder.addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Сброс",
                resetPendingIntent
            )
        }

        return builder.build()
    }

    // Обновление уведомления
    private fun updateNotification(timerId: Int) {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, createNotification(timerId))
        } catch (e: Exception) {
            Log.e("TimerService", "Ошибка при обновлении уведомления", e)
        }
    }

    // Запуск таймера
    fun startTimer(timerId: Int, duration: Long) {
        Log.d("TimerService", "startTimer: timerId=$timerId, duration=$duration")

        if (timerId < 0 || timerId >= timers.size) {
            Log.e("TimerService", "Неверный timerId: $timerId")
            return
        }

        // Останавливаем существующий таймер, если он запущен
        stopTimer(timerId)

        // Настраиваем новый таймер
        timers[timerId].duration = duration
        timers[timerId].timeRemaining = duration
        timers[timerId].running = true

        // Создаем и запускаем CountDownTimer для этого таймера
        timers[timerId].countDownTimer = object : CountDownTimer(duration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // Обновляем оставшееся время
                timers[timerId].timeRemaining = millisUntilFinished
                timers[timerId].timeRemainingLiveData.postValue(millisUntilFinished)

                // Обновляем уведомление каждые 5 секунд
                if (millisUntilFinished % 5000 <= 1000) {
                    updateNotification(timerId)
                }

                Log.d("TimerService", "Timer $timerId: $millisUntilFinished ms left")
            }

            override fun onFinish() {
                // Таймер завершился
                timers[timerId].timeRemaining = 0
                timers[timerId].running = false
                timers[timerId].timeRemainingLiveData.postValue(0L)

                // Воспроизводим звук завершения
                playSound(timers[timerId].soundResId)

                // Обновляем уведомление
                updateNotification(-1)

                Log.d("TimerService", "Timer $timerId finished")
            }
        }.start()

        // Обновляем уведомление
        updateNotification(timerId)
    }

    // Остановка таймера
    fun stopTimer(timerId: Int) {
        Log.d("TimerService", "stopTimer: timerId=$timerId")

        if (timerId < 0 || timerId >= timers.size) {
            Log.e("TimerService", "Неверный timerId: $timerId")
            return
        }

        timers[timerId].running = false
        timers[timerId].countDownTimer?.cancel()
        timers[timerId].timeRemainingLiveData.postValue(timers[timerId].timeRemaining)

        // Обновляем уведомление
        updateNotification(-1)
    }

    // Сброс таймера
    fun resetTimer(timerId: Int) {
        Log.d("TimerService", "resetTimer: timerId=$timerId")

        if (timerId < 0 || timerId >= timers.size) {
            Log.e("TimerService", "Неверный timerId: $timerId")
            return
        }

        timers[timerId].running = false
        timers[timerId].countDownTimer?.cancel()
        timers[timerId].timeRemaining = 0
        timers[timerId].timeRemainingLiveData.postValue(0L)

        // Обновляем уведомление
        updateNotification(-1)
    }

    // Воспроизведение звука
    fun playSound(soundResId: Int) {
        try {
            // Очищаем предыдущий MediaPlayer, если он существует
            mediaPlayer?.release()
            mediaPlayer = null

            // Создаем новый MediaPlayer и воспроизводим звук
            mediaPlayer = MediaPlayer.create(this, soundResId)
            mediaPlayer?.setOnCompletionListener { mp ->
                try {
                    mp.release()
                } catch (e: Exception) {
                    Log.e("TimerService", "Ошибка при освобождении MediaPlayer", e)
                }
            }
            mediaPlayer?.start()
        } catch (e: Exception) {
            Log.e("TimerService", "Ошибка при воспроизведении звука", e)
        }
    }

    // Форматирование времени
    fun formatTime(timeInMillis: Long): String {
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
            Log.e("TimerService", "Ошибка при форматировании времени", e)
            return "00:00"
        }
    }

    // Получение форматированного времени таймера
    fun getFormattedTime(timerId: Int): String {
        return if (timerId >= 0 && timerId < timers.size) {
            formatTime(timers[timerId].timeRemaining)
        } else {
            "00:00"
        }
    }

    // Проверка, запущен ли таймер
    fun isTimerRunning(timerId: Int): Boolean {
        return if (timerId >= 0 && timerId < timers.size) {
            timers[timerId].running
        } else {
            false
        }
    }

    // Получение процента прогресса таймера
    fun getTimerProgressPercent(timerId: Int): Int {
        if (timerId < 0 || timerId >= timers.size || timers[timerId].duration <= 0) return 0
        val progress = ((timers[timerId].timeRemaining * 100) / timers[timerId].duration).toInt()
        return progress.coerceIn(0, 100) // Гарантируем, что прогресс в диапазоне от 0 до 100
    }

    // Установка имени таймера
    fun setTimerName(timerId: Int, name: String) {
        if (timerId >= 0 && timerId < timers.size) {
            timers[timerId].name = name
            // Обновляем уведомление, если таймер запущен
            if (timers[timerId].running) {
                updateNotification(timerId)
            }
        }
    }

    // Получение имени таймера
    fun getTimerName(timerId: Int): String {
        return if (timerId >= 0 && timerId < timers.size) {
            timers[timerId].name
        } else {
            "Таймер"
        }
    }

    // Установка цвета таймера
    fun setTimerColor(timerId: Int, color: Int) {
        if (timerId >= 0 && timerId < timers.size) {
            timers[timerId].color = color
        }
    }

    // Получение цвета таймера
    fun getTimerColor(timerId: Int): Int {
        return if (timerId >= 0 && timerId < timers.size) {
            timers[timerId].color
        } else {
            Color.GREEN
        }
    }

    // Установка звука таймера
    fun setTimerSound(timerId: Int, soundResId: Int) {
        if (timerId >= 0 && timerId < timers.size) {
            timers[timerId].soundResId = soundResId
        }
    }

    // Получение звука таймера
    fun getTimerSound(timerId: Int): Int {
        return if (timerId >= 0 && timerId < timers.size) {
            timers[timerId].soundResId
        } else {
            R.raw.timer_bell
        }
    }

    // Получение LiveData для таймера
    fun getTimerLiveData(timerId: Int): LiveData<Long> {
        return if (timerId >= 0 && timerId < timers.size) {
            timers[timerId].timeRemainingLiveData
        } else {
            MutableLiveData<Long>().apply { value = 0L }
        }
    }

    override fun onDestroy() {
        // Останавливаем все таймеры при уничтожении сервиса
        for (i in timers.indices) {
            stopTimer(i)
        }

        // Освобождаем MediaPlayer
        try {
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e("TimerService", "Ошибка при освобождении MediaPlayer", e)
        }

        super.onDestroy()
        Log.d("TimerService", "Service destroyed")
    }
}