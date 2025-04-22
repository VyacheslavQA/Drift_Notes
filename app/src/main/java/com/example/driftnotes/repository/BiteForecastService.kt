package com.example.driftnotes.repository

import android.util.Log
import com.example.driftnotes.models.BiteForecast
import com.example.driftnotes.utils.FirebaseManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Сервис для работы с прогнозами клёва
 */
class BiteForecastService {
    private val firestore: FirebaseFirestore = FirebaseManager.firestore
    private val TAG = "BiteForecastService"

    /**
     * Получает прогноз клёва на указанную дату
     */
    suspend fun getBiteForecast(date: Date): Result<BiteForecast?> {
        return try {
            // Определяем начало дня
            val calendar = Calendar.getInstance()
            calendar.time = date
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfDay = calendar.time

            // Определяем конец дня
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val endOfDay = calendar.time

            // Ищем прогноз для указанной даты
            val snapshot = firestore.collection("bite_forecasts")
                .whereGreaterThanOrEqualTo("date", startOfDay)
                .whereLessThanOrEqualTo("date", endOfDay)
                .get()
                .await()

            if (snapshot.documents.isNotEmpty()) {
                // Берем первый найденный прогноз
                val doc = snapshot.documents.first()
                val forecast = doc.toObject(BiteForecast::class.java)?.copy(id = doc.id)
                Result.success(forecast)
            } else {
                // Создаем автоматический прогноз
                val newForecast = generateBiteForecast(date)
                Result.success(newForecast)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении прогноза клёва: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Генерирует прогноз клёва на основе даты
     */
    private suspend fun generateBiteForecast(date: Date): BiteForecast? {
        try {
            // Получаем номер дня и месяца
            val calendar = Calendar.getInstance()
            calendar.time = date
            val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
            val month = calendar.get(Calendar.MONTH)

            // Генерируем расчетный рейтинг на основе даты (просто для примера)
            // В реальном сервисе здесь должны быть учтены различные факторы: давление, ветер, фаза луны и т.д.

            // Особо хороший клёв в начале и середине месяца
            val baseRating = when {
                dayOfMonth in 1..5 -> 4
                dayOfMonth in 13..17 -> 5
                dayOfMonth in 27..31 -> 3
                else -> 2
            }

            // Корректировка в зависимости от сезона
            val seasonMultiplier = when (month) {
                // Весна
                Calendar.MARCH, Calendar.APRIL, Calendar.MAY -> 1.2
                // Лето
                Calendar.JUNE, Calendar.JULY, Calendar.AUGUST -> 1.0
                // Осень
                Calendar.SEPTEMBER, Calendar.OCTOBER, Calendar.NOVEMBER -> 1.3
                // Зима
                else -> 0.8
            }

            // Рассчитываем итоговый рейтинг (от 1 до 5)
            var finalRating = (baseRating * seasonMultiplier).toInt()
            if (finalRating < 1) finalRating = 1
            if (finalRating > 5) finalRating = 5

            // Выбираем описание в зависимости от рейтинга
            val description = when (finalRating) {
                5 -> "Исключительные условия для клёва. Рыба очень активна."
                4 -> "Хорошие условия для клёва. Рыба активна."
                3 -> "Средние условия для клёва. Умеренная активность."
                2 -> "Ниже среднего. Клёв слабый."
                1 -> "Плохие условия для клёва. Рыба малоактивна."
                else -> "Неопределённые условия для клёва."
            }

            // Создаем прогноз
            val forecast = BiteForecast(
                date = calendar.time,
                rating = finalRating,
                weatherDescription = "Прогноз погоды не загружен",
                pressure = 760.0,    // Значения по умолчанию
                windSpeed = 3.0,     // Значения по умолчанию
                cloudCover = 50,     // Значения по умолчанию
                moonPhase = getMoonPhaseForDate(date),
                description = description,
                createdAt = Date()
            )

            // Сохраняем прогноз в базу данных
            val docRef = firestore.collection("bite_forecasts").add(forecast).await()
            return forecast.copy(id = docRef.id)

        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при генерации прогноза клёва: ${e.message}", e)
            return null
        }
    }

    /**
     * Определяет фазу луны для заданной даты
     */
    private fun getMoonPhaseForDate(date: Date): String {
        val calendar = Calendar.getInstance()
        calendar.time = date

        // Получаем день месяца
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

        // Упрощенная реализация (в реальности требуется сложный алгоритм)
        // Здесь мы просто делим месяц на 4 части для определения фазы
        return when {
            dayOfMonth in 1..7 -> "Новолуние"
            dayOfMonth in 8..14 -> "Первая четверть"
            dayOfMonth in 15..21 -> "Полнолуние"
            else -> "Последняя четверть"
        }
    }

    /**
     * Обновляет прогнозы клёва на указанный месяц
     */
    suspend fun updateBiteForecastsForMonth(year: Int, month: Int): Result<Int> {
        return try {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, 1)

            val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            var updatedCount = 0

            for (day in 1..daysInMonth) {
                calendar.set(Calendar.DAY_OF_MONTH, day)
                val forecast = getBiteForecast(calendar.time).getOrNull()
                if (forecast != null) {
                    updatedCount++
                }
            }

            Log.d(TAG, "Обновлено $updatedCount прогнозов для $year-$month")
            Result.success(updatedCount)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при обновлении прогнозов: ${e.message}", e)
            Result.failure(e)
        }
    }
}