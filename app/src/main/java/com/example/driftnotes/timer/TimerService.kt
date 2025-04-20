// Файл app/src/main/java/com/example/driftnotes/timer/TimerService.kt
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
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Process
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

        // Коды сообщений для обработчика
        private const val MSG_START_TIMER = 1
        private const val MSG_STOP_TIMER = 2
        private const val MSG_RESET_TIMER = 3
        private const val MSG_UPDATE_TIMER = 4
        private const val MSG_PLAY_SOUND = 5

        // Интервал обновления в миллисекундах
        private const val UPDATE_INTERVAL = 1000L
    }

    private val binder = LocalBinder()
    private lateinit var timers: Array<TimerInfo>
    private var mediaPlayer: MediaPlayer? = null

    // Создаем отдельный поток для работы таймеров
    private lateinit var serviceHandler: Handler
    private lateinit var handlerThread: HandlerThread

    // Обработчик для UI потока
    private val mainHandler = Handler(Looper.getMainLooper())

    // Класс для хранения информации о таймере
    inner class TimerInfo(val timerId: Int) {
        var name: String = "Таймер ${timerId + 1}"
        var color: Int = Color.parseColor("#4CAF50") // Зеленый по умолчанию
        var soundResId: Int = R.raw.timer_bell // Звук по умолчанию

        var duration: Long = 0
        var timeRemaining: Long = 0
        var startTime: Long = 0
        var running: Boolean = false
        var nextUpdateTime: Long = 0

        val timeRemainingLiveData = MutableLiveData<Long>()

        // Функция для проверки состояния таймера
        fun isRunning(): Boolean = running
    }

    inner class LocalBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    override fun onCreate() {
        super.onCreate()
        try {
            // Инициализируем массив таймеров
            timers = Array(4) { TimerInfo(it) }

            // Создаем отдельный поток с повышенным приоритетом
            handlerThread = HandlerThread("TimerServiceThread", Process.THREAD_PRIORITY_BACKGROUND)
            handlerThread.start()

            // Создаем обработчик для этого потока
            serviceHandler = object : Handler(handlerThread.looper) {
                override fun handleMessage(msg: Message) {
                    try {
                        when (msg.what) {
                            MSG_START_TIMER -> {
                                val timerId = msg.arg1
                                val duration = msg.obj as Long
                                handleStartTimer(timerId, duration)
                            }
                            MSG_STOP_TIMER -> {
                                handleStopTimer(msg.arg1)
                            }
                            MSG_RESET_TIMER -> {
                                handleResetTimer(msg.arg1)
                            }
                            MSG_UPDATE_TIMER -> {
                                handleUpdateTimer(msg.arg1)
                            }
                            MSG_PLAY_SOUND -> {
                                handlePlaySound(msg.arg1)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("TimerService", "Ошибка в обработчике сообщений: ${e.message}", e)
                    }
                }
            }

            createNotificationChannel()
            startForeground(NOTIFICATION_ID, createNotification())
            Log.d("TimerService", "Service created")
        } catch (e: Exception) {
            Log.e("TimerService", "Ошибка при создании сервиса: ${e.message}", e)
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("TimerService", "onStartCommand: Запуск сервиса с flags=$flags, startId=$startId")

        try {
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
        } catch (e: Exception) {
            Log.e("TimerService", "Ошибка в onStartCommand: ${e.message}", e)
        }

        return START_STICKY
    }

    // Создание канала уведомлений (для Android 8.0+)
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Таймеры",
                    NotificationManager.IMPORTANCE_LOW
                )
                channel.description = "Канал для отображения работающих таймеров"

                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            } catch (e: Exception) {
                Log.e("TimerService", "Ошибка при создании канала уведомлений: ${e.message}", e)
            }
        }
    }

    // Создание уведомления
    private fun createNotification(timerId: Int = -1): Notification {
        try {
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
        } catch (e: Exception) {
            Log.e("TimerService", "Ошибка при создании уведомления: ${e.message}", e)
            // Возвращаем базовое уведомление если произошла ошибка
            return NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Таймеры")
                .setContentText("Сервис таймеров работает")
                .setSmallIcon(R.drawable.ic_bottom_timer)
                .build()
        }
    }

    // Обновление уведомления (выполняется в UI потоке)
    private fun updateNotification(timerId: Int) {
        mainHandler.post {
            try {
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, createNotification(timerId))
            } catch (e: Exception) {
                Log.e("TimerService", "Ошибка при обновлении уведомления: ${e.message}", e)
            }
        }
    }

    // Запуск таймера
    fun startTimer(timerId: Int, duration: Long) {
        if (timerId < 0 || timerId >= timers.size) return

        try {
            // Отправляем сообщение обработчику в фоновом потоке
            val msg = serviceHandler.obtainMessage(MSG_START_TIMER, timerId, 0, duration)
            serviceHandler.sendMessage(msg)
        } catch (e: Exception) {
            Log.e("TimerService", "Ошибка при запуске таймера: ${e.message}", e)
        }
    }

    // Внутренняя обработка запуска таймера (выполняется в фоновом потоке)
    private fun handleStartTimer(timerId: Int, duration: Long) {
        Log.d("TimerService", "startTimer: timerId=$timerId, duration=$duration")
        try {
            // Останавливаем существующий таймер, если он запущен
            handleStopTimer(timerId)

            // Настраиваем новый таймер
            timers[timerId].duration = duration
            timers[timerId].timeRemaining = duration
            timers[timerId].startTime = System.currentTimeMillis()
            timers[timerId].running = true

            // Устанавливаем время следующего обновления
            timers[timerId].nextUpdateTime = System.currentTimeMillis() + UPDATE_INTERVAL

            // Отправляем сообщение для обновления таймера через 1 секунду
            val updateMsg = serviceHandler.obtainMessage(MSG_UPDATE_TIMER, timerId, 0)
            serviceHandler.sendMessageDelayed(updateMsg, UPDATE_INTERVAL)

            // Отправляем начальное значение в LiveData
            mainHandler.post {
                try {
                    timers[timerId].timeRemainingLiveData.value = duration
                } catch (e: Exception) {
                    Log.e("TimerService", "Ошибка при обновлении LiveData: ${e.message}", e)
                }
            }

            // Обновляем уведомление
            updateNotification(timerId)
        } catch (e: Exception) {
            Log.e("TimerService", "Ошибка при обработке запуска таймера: ${e.message}", e)
        }
    }

    // Остановка таймера
    fun stopTimer(timerId: Int) {
        if (timerId < 0 || timerId >= timers.size) return

        try {
            // Отправляем сообщение обработчику в фоновом потоке
            val msg = serviceHandler.obtainMessage(MSG_STOP_TIMER, timerId, 0)
            serviceHandler.sendMessage(msg)
        } catch (e: Exception) {
            Log.e("TimerService", "Ошибка при остановке таймера: ${e.message}", e)
        }
    }

    // Внутренняя обработка остановки таймера (выполняется в фоновом потоке)
    private fun handleStopTimer(timerId: Int) {
        Log.d("TimerService", "stopTimer: timerId=$timerId")
        try {
            // Отменяем ожидающие сообщения для этого таймера
            serviceHandler.removeMessages(MSG_UPDATE_TIMER, timerId)

            // Останавливаем таймер
            timers[timerId].running = false

            // Отправляем текущее значение в LiveData
            mainHandler.post {
                try {
                    timers[timerId].timeRemainingLiveData.value = timers[timerId].timeRemaining
                } catch (e: Exception) {
                    Log.e("TimerService", "Ошибка при обновлении LiveData: ${e.message}", e)
                }
            }

            // Обновляем уведомление
            updateNotification(-1)
        } catch (e: Exception) {
            Log.e("TimerService", "Ошибка при обработке остановки таймера: ${e.message}", e)
        }
    }

    // Сброс таймера
    fun resetTimer(timerId: Int) {
        if (timerId < 0 || timerId >= timers.size) return

        try {
            // Отправляем сообщение обработчику в фоновом потоке
            val msg = serviceHandler.obtainMessage(MSG_RESET_TIMER, timerId, 0)
            serviceHandler.sendMessage(msg)
        } catch (e: Exception) {
            Log.e("TimerService", "Ошибка при сбросе таймера: ${e.message}", e)

            // Если произошла ошибка при отправке сообщения, попробуем выполнить сброс прямо здесь
            mainHandler.post {
                try {
                    // Сбрасываем таймер в основном потоке
                    timers[timerId].running = false
                    timers[timerId].timeRemaining = 0
                    timers[timerId].timeRemainingLiveData.value = 0L
                } catch (innerE: Exception) {
                    Log.e("TimerService", "Ошибка при аварийном сбросе таймера: ${innerE.message}", innerE)
                }
            }
        }
    }

    // Внутренняя обработка сброса таймера (выполняется в фоновом потоке)
    private fun handleResetTimer(timerId: Int) {
        Log.d("TimerService", "resetTimer: timerId=$timerId")
        try {
            // Отменяем ожидающие сообщения для этого таймера
            serviceHandler.removeMessages(MSG_UPDATE_TIMER, timerId)

            // Сбрасываем таймер
            timers[timerId].running = false
            timers[timerId].timeRemaining = 0

            // Отправляем нулевое значение в LiveData
            mainHandler.post {
                try {
                    timers[timerId].timeRemainingLiveData.value = 0L
                } catch (e: Exception) {
                    Log.e("TimerService", "Ошибка при обновлении LiveData: ${e.message}", e)
                }
            }

            // Обновляем уведомление
            updateNotification(-1)
        } catch (e: Exception) {
            Log.e("TimerService", "Ошибка при обработке сброса таймера: ${e.message}", e)
        }
    }

    // Обработка обновления таймера (выполняется в фоновом потоке)
    private fun handleUpdateTimer(timerId: Int) {
        try {
            if (timerId < 0 || timerId >= timers.size || !timers[timerId].running) return

            // Вычисляем прошедшее время
            val currentTime = System.currentTimeMillis()
            val elapsedTime = currentTime - timers[timerId].startTime
            val remainingTime = timers[timerId].duration - elapsedTime

            if (remainingTime <= 0) {
                // Таймер завершился
                timers[timerId].timeRemaining = 0
                timers[timerId].running = false

                // Отправляем нулевое значение в LiveData
                mainHandler.post {
                    try {
                        timers[timerId].timeRemainingLiveData.value = 0L
                    } catch (e: Exception) {
                        Log.e("TimerService", "Ошибка при обновлении LiveData: ${e.message}", e)
                    }
                }

                // Воспроизводим звук завершения
                val soundMsg = serviceHandler.obtainMessage(MSG_PLAY_SOUND, timers[timerId].soundResId, 0)
                serviceHandler.sendMessage(soundMsg)

                // Обновляем уведомление
                updateNotification(-1)

                Log.d("TimerService", "Timer $timerId finished")
            } else {
                // Таймер еще работает
                timers[timerId].timeRemaining = remainingTime

                // Отправляем текущее значение в LiveData
                mainHandler.post {
                    try {
                        timers[timerId].timeRemainingLiveData.value = remainingTime
                    } catch (e: Exception) {
                        Log.e("TimerService", "Ошибка при обновлении LiveData: ${e.message}", e)
                    }
                }

                // Обновляем уведомление каждые 5 секунд
                if (currentTime >= timers[timerId].nextUpdateTime) {
                    updateNotification(timerId)
                    timers[timerId].nextUpdateTime = currentTime + 5000 // Следующее обновление через 5 секунд
                }

                // Планируем следующее обновление через 1 секунду, только если таймер запущен
                if (timers[timerId].running) {
                    val updateMsg = serviceHandler.obtainMessage(MSG_UPDATE_TIMER, timerId, 0)
                    serviceHandler.sendMessageDelayed(updateMsg, UPDATE_INTERVAL)

                    Log.d("TimerService", "Timer $timerId: $remainingTime ms left")
                }
            }
        } catch (e: Exception) {
            Log.e("TimerService", "Ошибка при обновлении таймера: ${e.message}", e)
        }
    }

    // Воспроизведение звука (выполняется в фоновом потоке)
    private fun handlePlaySound(soundResId: Int) {
        try {
            // Используем UI поток для работы с MediaPlayer
            mainHandler.post {
                try {
                    // Очищаем предыдущий MediaPlayer, если он существует
                    mediaPlayer?.release()
                    mediaPlayer = null

                    // Создаем новый MediaPlayer и воспроизводим звук
                    mediaPlayer = MediaPlayer.create(this@TimerService, soundResId)
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
        } catch (e: Exception) {
            Log.e("TimerService", "Ошибка при обработке звука", e)
        }
    }

    // Воспроизведение звука (публичный метод)
    fun playSound(soundResId: Int) {
        try {
            val msg = serviceHandler.obtainMessage(MSG_PLAY_SOUND, soundResId, 0)
            serviceHandler.sendMessage(msg)
        } catch (e: Exception) {
            Log.e("TimerService", "Ошибка при отправке запроса на воспроизведение звука: ${e.message}", e)
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
        return try {
            if (timerId >= 0 && timerId < timers.size) {
                formatTime(timers[timerId].timeRemaining)
            } else {
                "00:00"
            }
        } catch (e: Exception) {
            Log.e("TimerService", "Ошибка при получении форматированного времени", e)
            "00:00"
        }
    }

    // Проверка, запущен ли таймер
    fun isTimerRunning(timerId: Int): Boolean {
        return try {
            if (timerId >= 0 && timerId < timers.size) {
                timers[timerId].running
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("TimerService", "Ошибка при проверке состояния таймера", e)
            false
        }
    }

    // Получение процента прогресса таймера
    fun getTimerProgressPercent(timerId: Int): Int {
        try {
            if (timerId < 0 || timerId >= timers.size || timers[timerId].duration <= 0) return 0
            val progress = ((timers[timerId].timeRemaining * 100) / timers[timerId].duration).toInt()
            return progress.coerceIn(0, 100) // Гарантируем, что прогресс в диапазоне от 0 до 100
        } catch (e: Exception) {
            Log.e("TimerService", "Ошибка при получении процента прогресса", e)
            return 0
        }
    }

    // Установка имени таймера
    fun setTimerName(timerId: Int, name: String) {
        try {
            if (timerId >= 0 && timerId < timers.size) {
                timers[timerId].name = name
                // Обновляем уведомление, если таймер запущен
                if (timers[timerId].running) {
                    updateNotification(timerId)
                }
            }
        } catch (e: Exception) {
            Log.e("TimerService", "Ошибка при установке имени таймера", e)
        }
    }

    // Получение имени таймера
    fun getTimerName(timerId: Int): String {
        return try {
            if (timerId >= 0 && timerId < timers.size) {
                timers[timerId].name
            } else {
                "Таймер"
            }
        } catch (e: Exception) {
            Log.e("TimerService", "Ошибка при получении имени таймера", e)
            "Таймер"
        }
    }

    // Установка цвета таймера
    fun setTimerColor(timerId: Int, color: Int) {
        try {
            if (timerId >= 0 && timerId < timers.size) {
                timers[timerId].color = color
            }
        } catch (e: Exception) {
            Log.e("TimerService", "Ошибка при установке цвета таймера", e)
        }
    }

    // Получение цвета таймера
    fun getTimerColor(timerId: Int): Int {
        return try {
            if (timerId >= 0 && timerId < timers.size) {
                timers[timerId].color
            } else {
                Color.GREEN
            }
        } catch (e: Exception) {
            Log.e("TimerService", "Ошибка при получении цвета таймера", e)
            Color.GREEN
        }
    }

    // Установка звука таймера
    fun setTimerSound(timerId: Int, soundResId: Int) {
        try {
            if (timerId >= 0 && timerId < timers.size) {
                timers[timerId].soundResId = soundResId
            }
        } catch (e: Exception) {
            Log.e("TimerService", "Ошибка при установке звука таймера", e)
        }
    }

    // Получение звука таймера
    fun getTimerSound(timerId: Int): Int {
        return try {
            if (timerId >= 0 && timerId < timers.size) {
                timers[timerId].soundResId
            } else {
                R.raw.timer_bell
            }
        } catch (e: Exception) {
            Log.e("TimerService", "Ошибка при получении звука таймера", e)
            R.raw.timer_bell
        }
    }

    // Получение LiveData для таймера
    fun getTimerLiveData(timerId: Int): LiveData<Long> {
        return try {
            if (timerId >= 0 && timerId < timers.size) {
                timers[timerId].timeRemainingLiveData
            } else {
                MutableLiveData<Long>().apply { value = 0L }
            }
        } catch (e: Exception) {
            Log.e("TimerService", "Ошибка при получении LiveData таймера", e)
            MutableLiveData<Long>().apply { value = 0L }
        }
    }

    override fun onDestroy() {
        // Останавливаем все таймеры при уничтожении сервиса
        try {
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

            // Останавливаем фоновый поток
            try {
                handlerThread.quitSafely()
            } catch (e: Exception) {
                Log.e("TimerService", "Ошибка при остановке потока", e)
            }
        } catch (e: Exception) {
            Log.e("TimerService", "Ошибка при уничтожении сервиса", e)
        }

        super.onDestroy()
        Log.d("TimerService", "Service destroyed")
    }
}