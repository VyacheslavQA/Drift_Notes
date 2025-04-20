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
    private val timers = Array(4) { TimerInfo() }
    private var mediaPlayer: MediaPlayer? = null

    // Класс для хранения информации о таймере
    inner class TimerInfo {
        var name: String = "Таймер"
        var color: Int = Color.parseColor("#4CAF50") // Зеленый по умолчанию
        var soundResId: Int = R.raw.timer_bell // Звук по умолчанию

        var duration: Long = 0
        var timeRemaining: Long = 0
        var timer: CountDownTimer? = null

        val timeRemainingLiveData = MutableLiveData<Long>()

        fun isRunning(): Boolean = timer != null
    }

    inner class LocalBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
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

        val contentTitle = if (timerId != -1) {
            "Таймер: ${timers[timerId].name}"
        } else {
            "Таймеры"
        }

        val contentText = if (timerId != -1) {
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
        if (timerId != -1) {
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
            // Обработка возможных ошибок при обновлении уведомления
            e.printStackTrace()
        }
    }

    // Запуск таймера
    @Synchronized
    fun startTimer(timerId: Int, duration: Long) {
        Log.d("TimerService", "startTimer: timerId=$timerId, duration=$duration")
        try {
            // Останавливаем существующий таймер, если он запущен
            stopTimer(timerId)

            // Настраиваем новый таймер
            timers[timerId].duration = duration
            timers[timerId].timeRemaining = duration

            // Создаем и запускаем CountDownTimer
            timers[timerId].timer = object : CountDownTimer(duration, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    try {
                        timers[timerId].timeRemaining = millisUntilFinished
                        timers[timerId].timeRemainingLiveData.postValue(millisUntilFinished)

                        // Обновляем уведомление каждые 5 секунд
                        if (millisUntilFinished % 5000 <= 1000) {
                            updateNotification(timerId)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun onFinish() {
                    try {
                        timers[timerId].timeRemaining = 0
                        timers[timerId].timeRemainingLiveData.postValue(0L)
                        timers[timerId].timer = null

                        // Воспроизводим звук завершения
                        playSound(timers[timerId].soundResId)

                        // Обновляем уведомление
                        updateNotification(-1)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }.start()

            // Обновляем уведомление
            updateNotification(timerId)
        } catch (e: Exception) {
            Log.e("TimerService", "Ошибка при запуске таймера: ${e.message}", e)
            // Если таймер не запустился, сбрасываем его состояние
            timers[timerId].timer = null
            timers[timerId].timeRemaining = 0
            timers[timerId].timeRemainingLiveData.postValue(0L)
        }
    }

    // Остановка таймера
    @Synchronized
    fun stopTimer(timerId: Int) {
        Log.d("TimerService", "stopTimer: timerId=$timerId")
        try {
            timers[timerId].timer?.cancel()
            timers[timerId].timer = null
            timers[timerId].timeRemainingLiveData.postValue(timers[timerId].timeRemaining)

            // Обновляем уведомление
            updateNotification(-1)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Сброс таймера
    @Synchronized
    fun resetTimer(timerId: Int) {
        Log.d("TimerService", "resetTimer: timerId=$timerId")
        try {
            timers[timerId].timer?.cancel()
            timers[timerId].timer = null
            timers[timerId].timeRemaining = 0
            timers[timerId].timeRemainingLiveData.postValue(0L)

            // Обновляем уведомление
            updateNotification(-1)
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
                    e.printStackTrace()
                }
            }
            mediaPlayer?.start()

            // Если устройство поддерживает вибрацию, вибрируем
            try {
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(android.os.VibrationEffect.createOneShot(500, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(500)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
            e.printStackTrace()
            return "00:00"
        }
    }

    // Получение форматированного времени таймера
    fun getFormattedTime(timerId: Int): String {
        return formatTime(timers[timerId].timeRemaining)
    }

    // Проверка, запущен ли таймер
    fun isTimerRunning(timerId: Int): Boolean {
        return timers[timerId].isRunning()
    }

    // Получение процента прогресса таймера
    fun getTimerProgressPercent(timerId: Int): Int {
        if (timers[timerId].duration <= 0) return 0
        val progress = ((timers[timerId].timeRemaining * 100) / timers[timerId].duration).toInt()
        return progress.coerceIn(0, 100) // Гарантируем, что прогресс в диапазоне от 0 до 100
    }

    // Установка имени таймера
    fun setTimerName(timerId: Int, name: String) {
        timers[timerId].name = name
        // Обновляем уведомление, если таймер запущен
        if (timers[timerId].isRunning()) {
            updateNotification(timerId)
        }
    }

    // Получение имени таймера
    fun getTimerName(timerId: Int): String {
        return timers[timerId].name
    }

    // Установка цвета таймера
    fun setTimerColor(timerId: Int, color: Int) {
        timers[timerId].color = color
    }

    // Получение цвета таймера
    fun getTimerColor(timerId: Int): Int {
        return timers[timerId].color
    }

    // Установка звука таймера
    fun setTimerSound(timerId: Int, soundResId: Int) {
        timers[timerId].soundResId = soundResId
    }

    // Получение звука таймера
    fun getTimerSound(timerId: Int): Int {
        return timers[timerId].soundResId
    }

    // Получение LiveData для таймера
    fun getTimerLiveData(timerId: Int): LiveData<Long> {
        return timers[timerId].timeRemainingLiveData
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
            e.printStackTrace()
        }

        super.onDestroy()
    }
}