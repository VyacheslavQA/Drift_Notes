package com.example.driftnotes.repository

import android.util.Log
import com.example.driftnotes.models.BiteForecast
import com.example.driftnotes.models.CalendarDay
import com.example.driftnotes.models.FishingNote
import com.example.driftnotes.models.PlannedTrip
import com.example.driftnotes.utils.FirebaseManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date

/**
 * Репозиторий для работы с календарем и событиями
 */
class CalendarRepository {
    private val firestore: FirebaseFirestore = FirebaseManager.firestore
    private val TAG = "CalendarRepository"

    /**
     * Получает данные календаря на указанный месяц
     */
    suspend fun getCalendarData(year: Int, month: Int): Result<Map<Int, CalendarDay>> {
        return try {
            val userId = FirebaseManager.getCurrentUserId() ?: throw Exception("Пользователь не авторизован")

            // Получаем первый и последний день месяца
            val calendar = Calendar.getInstance()
            calendar.set(year, month, 1, 0, 0, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startDate = calendar.time

            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val endDate = calendar.time

            Log.d(TAG, "Запрашиваем данные для периода: ${startDate} - ${endDate}")

            // Календарные дни, ключ - день месяца (1-31)
            val calendarDays = mutableMapOf<Int, CalendarDay>()

            // Получаем заметки о рыбалке
            val fishingNotes = getFishingNotes(userId, startDate, endDate)
            Log.d(TAG, "Найдено заметок о рыбалке: ${fishingNotes.size}")

            // Получаем запланированные рыбалки
            val plannedTrips = getPlannedTrips(userId, startDate, endDate)
            Log.d(TAG, "Найдено запланированных рыбалок: ${plannedTrips.size}")

            // Получаем прогнозы клёва
            val biteForecasts = getBiteForecasts(startDate, endDate)
            Log.d(TAG, "Найдено прогнозов клёва: ${biteForecasts.size}")

            // Создаем объекты CalendarDay для каждого дня месяца
            val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

            for (day in 1..daysInMonth) {
                // Задаем дату для текущего дня
                calendar.set(year, month, day, 12, 0, 0)
                val currentDate = calendar.time

                // Проверяем, есть ли события на этот день
                val hasFishingNote = fishingNotes.any { noteCoversDate(it, currentDate) }
                val hasPlannedTrip = plannedTrips.any { tripCoversDate(it, currentDate) }
                val biteForecast = biteForecasts.firstOrNull {
                    sameDay(it.date, currentDate)
                }

                // Найденный ID для заметки о рыбалке
                val fishingNoteId = fishingNotes.firstOrNull {
                    noteCoversDate(it, currentDate)
                }?.id ?: ""

                // Найденный ID для запланированной рыбалки
                val plannedTripId = plannedTrips.firstOrNull {
                    tripCoversDate(it, currentDate)
                }?.id ?: ""

                // Создаем объект дня календаря
                val calendarDay = CalendarDay(
                    date = currentDate,
                    hasFishingNote = hasFishingNote,
                    hasPlannedTrip = hasPlannedTrip,
                    hasBiteForecast = biteForecast != null,
                    biteRating = biteForecast?.rating ?: 0,
                    fishingNoteId = fishingNoteId,
                    plannedTripId = plannedTripId,
                    biteForecastId = biteForecast?.id ?: ""
                )

                // Добавляем день в карту по номеру дня (1-31)
                calendarDays[day] = calendarDay
            }

            Result.success(calendarDays)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении данных календаря: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Получает заметки о рыбалке для указанного периода
     */
    private suspend fun getFishingNotes(userId: String, startDate: Date, endDate: Date): List<FishingNote> {
        return try {
            // Создаем список для хранения заметок
            val fishingNotes = mutableListOf<FishingNote>()

            // Запрос на заметки, которые начинаются в указанном периоде
            val snapshot = firestore.collection("fishing_notes")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .orderBy("date", Query.Direction.ASCENDING)
                .get()
                .await()

            // Добавляем найденные заметки в список
            for (doc in snapshot.documents) {
                try {
                    val note = doc.toObject(FishingNote::class.java)?.copy(id = doc.id)
                    if (note != null) {
                        Log.d(TAG, "Найдена заметка: $note")
                        fishingNotes.add(note)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка при преобразовании заметки: ${e.message}", e)
                }
            }

            // Для многодневных рыбалок, которые начинаются до startDate, но заканчиваются после
            val secondQuery = firestore.collection("fishing_notes")
                .whereEqualTo("userId", userId)
                .whereLessThan("date", startDate)
                .get()
                .await()

            for (doc in secondQuery.documents) {
                try {
                    val note = doc.toObject(FishingNote::class.java)?.copy(id = doc.id)
                    if (note != null && note.endDate != null && note.endDate.after(startDate)) {
                        Log.d(TAG, "Найдена многодневная заметка, пересекающаяся с периодом: $note")
                        fishingNotes.add(note)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка при преобразовании заметки (второй запрос): ${e.message}", e)
                }
            }

            Log.d(TAG, "Всего найдено заметок: ${fishingNotes.size}")
            fishingNotes
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении заметок о рыбалке: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Получает запланированные рыбалки для указанного периода
     */
    private suspend fun getPlannedTrips(userId: String, startDate: Date, endDate: Date): List<PlannedTrip> {
        return try {
            // Запрос на планы, которые попадают в указанный период
            val snapshot = firestore.collection("planned_trips")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .orderBy("date", Query.Direction.ASCENDING)
                .get()
                .await()

            val plannedTrips = mutableListOf<PlannedTrip>()
            for (doc in snapshot.documents) {
                val trip = doc.toObject(PlannedTrip::class.java)?.copy(id = doc.id)
                if (trip != null) {
                    plannedTrips.add(trip)
                }
            }

            // Для многодневных планов, которые начинаются до startDate, но заканчиваются после
            val secondQuery = firestore.collection("planned_trips")
                .whereEqualTo("userId", userId)
                .whereLessThan("date", startDate)
                .whereEqualTo("isMultiDay", true)
                .get()
                .await()

            for (doc in secondQuery.documents) {
                val trip = doc.toObject(PlannedTrip::class.java)?.copy(id = doc.id)
                if (trip != null && trip.endDate != null && trip.endDate.after(startDate)) {
                    plannedTrips.add(trip)
                }
            }

            plannedTrips
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении запланированных рыбалок: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Получает прогнозы клёва для указанного периода
     */
    private suspend fun getBiteForecasts(startDate: Date, endDate: Date): List<BiteForecast> {
        return try {
            val snapshot = firestore.collection("bite_forecasts")
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .orderBy("date", Query.Direction.ASCENDING)
                .get()
                .await()

            val forecasts = mutableListOf<BiteForecast>()
            for (doc in snapshot.documents) {
                val forecast = doc.toObject(BiteForecast::class.java)?.copy(id = doc.id)
                if (forecast != null) {
                    forecasts.add(forecast)
                }
            }

            forecasts
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении прогнозов клёва: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Проверяет, относится ли заданная дата к периоду рыбалки
     */
    private fun noteCoversDate(note: FishingNote, date: Date): Boolean {
        // Для однодневной рыбалки проверяем только одну дату
        if (!note.isMultiDay || note.endDate == null) {
            return sameDay(note.date, date)
        }

        // Для многодневной рыбалки проверяем, входит ли дата в диапазон
        val calendar = Calendar.getInstance()

        // Устанавливаем время для начала дня заметки
        calendar.time = note.date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfNoteDay = calendar.time

        // Устанавливаем время для конца дня окончания рыбалки
        calendar.time = note.endDate
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfNoteDay = calendar.time

        // Дата входит в диапазон, если она не раньше начала и не позже конца
        return !date.before(startOfNoteDay) && !date.after(endOfNoteDay)
    }

    /**
     * Проверяет, относится ли заданная дата к периоду запланированной рыбалки
     */
    private fun tripCoversDate(trip: PlannedTrip, date: Date): Boolean {
        // Для однодневной рыбалки проверяем только одну дату
        if (!trip.isMultiDay || trip.endDate == null) {
            return sameDay(trip.date, date)
        }

        // Для многодневной рыбалки проверяем, входит ли дата в диапазон
        val calendar = Calendar.getInstance()

        // Устанавливаем время для начала дня плана
        calendar.time = trip.date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfTripDay = calendar.time

        // Устанавливаем время для конца дня окончания плана
        calendar.time = trip.endDate
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfTripDay = calendar.time

        // Дата входит в диапазон, если она не раньше начала и не позже конца
        return !date.before(startOfTripDay) && !date.after(endOfTripDay)
    }

    /**
     * Проверяет, относятся ли две даты к одному и тому же дню
     */
    private fun sameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance()
        val cal2 = Calendar.getInstance()
        cal1.time = date1
        cal2.time = date2
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
    }

    /**
     * Добавляет новую запланированную рыбалку
     */
    suspend fun addPlannedTrip(trip: PlannedTrip): Result<String> {
        return try {
            // Проверяем авторизацию
            val userId = FirebaseManager.getCurrentUserId() ?: throw Exception("Пользователь не авторизован")

            // Создаем план с ID пользователя
            val tripWithUserId = trip.copy(userId = userId)

            // Сохраняем в Firestore
            val documentRef = if (trip.id.isNotEmpty()) {
                // Если ID уже есть, обновляем существующий документ
                firestore.collection("planned_trips").document(trip.id).set(tripWithUserId).await()
                trip.id
            } else {
                // Иначе создаем новый документ
                val doc = firestore.collection("planned_trips").add(tripWithUserId).await()
                doc.id
            }

            Result.success(documentRef.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при добавлении запланированной рыбалки: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Обновляет запланированную рыбалку
     */
    suspend fun updatePlannedTrip(trip: PlannedTrip): Result<Unit> {
        return try {
            // Проверяем авторизацию
            val userId = FirebaseManager.getCurrentUserId() ?: throw Exception("Пользователь не авторизован")

            // Проверяем, что план существует и принадлежит текущему пользователю
            val existingTrip = firestore.collection("planned_trips").document(trip.id).get().await()

            if (!existingTrip.exists()) {
                throw Exception("Запланированная рыбалка не найдена")
            }

            if (existingTrip.getString("userId") != userId) {
                throw Exception("У вас нет прав на редактирование этой записи")
            }

            // Обновляем план
            firestore.collection("planned_trips").document(trip.id).set(trip).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при обновлении запланированной рыбалки: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Удаляет запланированную рыбалку
     */
    suspend fun deletePlannedTrip(tripId: String): Result<Unit> {
        return try {
            // Проверяем авторизацию
            val userId = FirebaseManager.getCurrentUserId() ?: throw Exception("Пользователь не авторизован")

            // Проверяем, что план существует и принадлежит текущему пользователю
            val existingTrip = firestore.collection("planned_trips").document(tripId).get().await()

            if (!existingTrip.exists()) {
                throw Exception("Запланированная рыбалка не найдена")
            }

            if (existingTrip.getString("userId") != userId) {
                throw Exception("У вас нет прав на удаление этой записи")
            }

            // Удаляем план
            firestore.collection("planned_trips").document(tripId).delete().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при удалении запланированной рыбалки: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Получает детали запланированной рыбалки
     */
    suspend fun getPlannedTripById(tripId: String): Result<PlannedTrip> {
        return try {
            // Проверяем авторизацию
            val userId = FirebaseManager.getCurrentUserId() ?: throw Exception("Пользователь не авторизован")

            // Получаем план
            val tripDoc = firestore.collection("planned_trips").document(tripId).get().await()

            if (!tripDoc.exists()) {
                throw Exception("Запланированная рыбалка не найдена")
            }

            val trip = tripDoc.toObject(PlannedTrip::class.java)?.copy(id = tripDoc.id)
                ?: throw Exception("Ошибка при чтении данных")

            // Проверяем права доступа
            if (trip.userId != userId) {
                throw Exception("У вас нет прав на просмотр этой записи")
            }

            Result.success(trip)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении запланированной рыбалки: ${e.message}", e)
            Result.failure(e)
        }
    }
}