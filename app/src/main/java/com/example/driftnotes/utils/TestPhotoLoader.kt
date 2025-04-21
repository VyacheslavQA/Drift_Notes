package com.example.driftnotes.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.driftnotes.models.FishingNote
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

/**
 * Тестовый класс для проверки доступности фотографий
 */
object TestPhotoLoader {
    private const val TAG = "TestPhotoLoader"

    /**
     * Проверяет доступность всех фотографий и выводит результат
     */
    fun testPhotos(context: Context, note: FishingNote) {
        if (note.photoUrls.isEmpty()) {
            Toast.makeText(context, "Нет фотографий для проверки", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(context, "Проверка доступности ${note.photoUrls.size} фото...", Toast.LENGTH_SHORT).show()

        thread {
            var successCount = 0
            var failCount = 0

            note.photoUrls.forEachIndexed { index, url ->
                try {
                    val urlConnection = URL(url).openConnection() as HttpURLConnection
                    urlConnection.connectTimeout = 5000
                    urlConnection.readTimeout = 5000
                    urlConnection.requestMethod = "HEAD"

                    val responseCode = urlConnection.responseCode
                    Log.d(TAG, "URL[$index]: $url, код ответа: $responseCode")

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        successCount++
                    } else {
                        failCount++
                        Log.e(TAG, "Фото недоступно: $url, код: $responseCode")
                    }

                    urlConnection.disconnect()
                } catch (e: IOException) {
                    failCount++
                    Log.e(TAG, "Ошибка при проверке фото: $url", e)
                }
            }

            // Выводим результат в основном потоке
            val message = "Результат: доступно $successCount, недоступно $failCount фото"
            Log.d(TAG, message)
        }
    }
}