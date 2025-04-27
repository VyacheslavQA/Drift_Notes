// Полный код для app/src/main/java/com/example/driftnotes/repository/StatsRepository.kt
package com.example.driftnotes.repository

import android.util.Log
import com.example.driftnotes.models.BestMonthInfo
import com.example.driftnotes.models.BiggestFishInfo
import com.example.driftnotes.models.FishingNote
import com.example.driftnotes.models.FishingStats
import com.example.driftnotes.models.LastTripInfo
import com.example.driftnotes.models.LongestTripInfo
import com.example.driftnotes.models.TrophyInfo
import com.example.driftnotes.utils.FirebaseManager
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Репозиторий для получения статистики рыбалок
 */
class StatsRepository {
    private val TAG = "StatsRepository"
    private val dateFormatter = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())

    /**
     * Получает статистику рыбалок для текущего пользователя
     * с возможностью фильтрации по датам
     *
     * @param startDate начальная дата периода (включительно)
     * @param endDate конечная дата периода (включительно)
     */
    suspend fun getFishingStats(startDate: Date = Date(0), endDate: Date = Date()): Result<FishingStats> {
        return try {
            val userId = FirebaseManager.getCurrentUserId() ?: throw Exception("Пользователь не авторизован")

            // Получаем ВСЕ заметки пользователя без ограничений даты
            val allNotesSnapshot = FirebaseManager.firestore.collection("fishing_notes")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val allNotes = allNotesSnapshot.documents.mapNotNull { doc ->
                try {
                    val note = doc.toObject(FishingNote::class.java)?.copy(id = doc.id)
                    note?.let {
                        // Выводим подробную информацию о каждой заметке
                        Log.d(TAG, "Заметка ID: ${it.id}")
                        Log.d(TAG, "- Локация: ${it.location}")
                        Log.d(TAG, "- Дата: ${dateFormatter.format(it.date)}")
                        Log.d(TAG, "- Конечная дата: ${it.endDate?.let { date -> dateFormatter.format(date) } ?: "NULL"}")
                        Log.d(TAG, "- Многодневная: ${it.isMultiDay}")
                        Log.d(TAG, "- JSON: ${doc.data}")
                    }
                    note
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка при преобразовании заметки: ${e.message}", e)
                    null
                }
            }

            Log.d(TAG, "Всего заметок пользователя: ${allNotes.size}")

            // Для поиска самой долгой рыбалки используем ВСЕ заметки
            // Исправленная версия с принудительным расчетом продолжительности
            val longestTrip = findLongestTripForced(allNotes)

            // Логируем информацию о самой долгой рыбалке
            longestTrip?.let {
                Log.d(TAG, "Найдена самая долгая рыбалка: ${it.durationDays} дней, ${it.location}, " +
                        "даты: ${dateFormatter.format(it.startDate)} - ${it.endDate?.let { date -> dateFormatter.format(date) } ?: "NULL"}")
            } ?: Log.d(TAG, "Не найдена информация о самой долгой рыбалке")

            // Фильтруем заметки по указанному диапазону дат для остальной статистики
            val filteredNotes = allNotes.filter { note ->
                // Для однодневных заметок проверяем, что дата в диапазоне
                if (!note.isMultiDay || note.endDate == null) {
                    isDateInRange(note.date, startDate, endDate)
                } else {
                    // Для многодневных заметок проверяем пересечение диапазонов
                    isDateRangeOverlapping(note.date, note.endDate, startDate, endDate)
                }
            }

            Log.d(TAG, "Заметок после фильтрации по датам: ${filteredNotes.size}")

            if (filteredNotes.isEmpty() && allNotes.isEmpty()) {
                // Если у пользователя нет заметок, возвращаем пустую статистику
                return Result.success(FishingStats())
            }

            // Общее количество рыбалок
            val totalTrips = filteredNotes.size

            // Фильтруем поклевки по диапазону дат
            val totalFish = filteredNotes.sumOf { note ->
                note.biteRecords.count { bite ->
                    isDateInRange(bite.time, startDate, endDate)
                }
            }

            // Среднее количество рыбы за одну рыбалку
            val averageFish = if (totalTrips > 0) {
                totalFish.toFloat() / totalTrips.toFloat()
            } else {
                0f
            }

            // Самая большая рыба (находим запись с максимальным весом)
            val biggestFish = findBiggestFish(filteredNotes, startDate, endDate)

            // Лучший месяц
            val bestMonth = findBestMonth(filteredNotes, startDate, endDate)

            // Последняя рыбалка (заметка с самой поздней датой)
            val lastTrip = findLastTrip(filteredNotes)

            // Последние трофеи (фото)
            val lastTrophies = findLastTrophies(filteredNotes, startDate, endDate)

            // Создаем объект статистики
            val stats = FishingStats(
                totalFishingTrips = totalTrips,
                totalFishCaught = totalFish,
                averageFishPerTrip = averageFish,
                biggestFish = biggestFish,
                longestTrip = longestTrip,
                bestMonth = bestMonth,
                lastTrip = lastTrip,
                lastTrophies = lastTrophies
            )

            Result.success(stats)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении статистики: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Проверяет, находится ли дата в указанном диапазоне (включительно)
     */
    private fun isDateInRange(date: Date, startDate: Date, endDate: Date): Boolean {
        return !date.before(startDate) && !date.after(endDate)
    }

    /**
     * Проверяет, пересекаются ли два диапазона дат
     */
    private fun isDateRangeOverlapping(start1: Date, end1: Date, start2: Date, end2: Date): Boolean {
        return !start1.after(end2) && !start2.after(end1)
    }

    /**
     * Находит последнюю рыбалку (по дате)
     */
    private fun findLastTrip(notes: List<FishingNote>): LastTripInfo? {
        if (notes.isEmpty()) return null

        // Находим заметку с самой поздней датой
        val latestNote = notes.maxByOrNull { note ->
            if (note.isMultiDay && note.endDate != null) note.endDate.time else note.date.time
        }

        return latestNote?.let {
            LastTripInfo(
                date = if (it.isMultiDay && it.endDate != null) it.endDate else it.date,
                location = it.location
            )
        }
    }

    /**
     * Находит информацию о самой большой рыбе в указанном диапазоне дат
     */
    private fun findBiggestFish(notes: List<FishingNote>, startDate: Date, endDate: Date): BiggestFishInfo? {
        // Собираем все записи о поклевках из всех заметок, фильтруя по дате
        val allBites = notes.flatMap { note ->
            note.biteRecords
                .filter { bite -> isDateInRange(bite.time, startDate, endDate) } // Фильтруем по дате
                .map { bite ->
                    // Для каждой поклевки создаем пару "поклевка + заметка", чтобы сохранить контекст
                    Pair(bite, note)
                }
        }

        // Находим поклевку с максимальным весом
        val biggestBitePair = allBites.maxByOrNull { (bite, _) -> bite.weight }

        return biggestBitePair?.let { (bite, note) ->
            // Если нашли, создаем объект с информацией о самой большой рыбе
            BiggestFishInfo(
                weight = bite.weight,
                fishType = bite.fishType,
                date = bite.time,
                location = note.location,
                photoUrl = note.photoUrls.firstOrNull() ?: ""
            )
        }
    }

    /**
     * Новая версия метода, которая принудительно вычисляет продолжительность по датам
     * независимо от флага isMultiDay
     */
    private fun findLongestTripForced(notes: List<FishingNote>): LongestTripInfo? {
        if (notes.isEmpty()) {
            Log.d(TAG, "Нет заметок для анализа самой долгой рыбалки")
            return null
        }

        // Вычисляем продолжительность для каждой заметки и создаем пары (заметка, продолжительность)
        val notesWithDuration = notes.map { note ->
            // Принудительно проверяем, есть ли конечная дата, независимо от флага isMultiDay
            val duration = calculateTripDurationForced(note)

            Log.d(TAG, "Заметка: ${note.location}, даты: " +
                    "${dateFormatter.format(note.date)} - " +
                    "${note.endDate?.let { dateFormatter.format(it) } ?: "NULL"}, " +
                    "isMultiDay: ${note.isMultiDay}, продолжительность: $duration дней")

            Pair(note, duration)
        }

        // Находим максимальную продолжительность
        val maxDuration = notesWithDuration.maxOfOrNull { (_, duration) -> duration } ?: 0
        Log.d(TAG, "Максимальная продолжительность: $maxDuration дней")

        // Фильтруем заметки с максимальной продолжительностью
        val longestTrips = notesWithDuration
            .filter { (_, duration) -> duration == maxDuration }
            .map { (note, _) -> note }

        Log.d(TAG, "Найдено рыбалок с максимальной продолжительностью: ${longestTrips.size}")

        // Если несколько рыбалок с одинаковой продолжительностью, берем самую последнюю по дате
        val latestLongestTrip = longestTrips.maxByOrNull { note ->
            if (note.endDate != null) note.endDate.time else note.date.time
        }

        return latestLongestTrip?.let { note ->
            val duration = calculateTripDurationForced(note)
            Log.d(TAG, "Выбрана самая долгая рыбалка: ${note.location}, " +
                    "даты: ${dateFormatter.format(note.date)} - " +
                    "${note.endDate?.let { dateFormatter.format(it) } ?: "NULL"}, " +
                    "продолжительность: $duration дней")

            LongestTripInfo(
                durationDays = duration,
                startDate = note.date,
                endDate = note.endDate ?: note.date,
                location = note.location
            )
        }
    }

    /**
     * Принудительно вычисляет продолжительность рыбалки в днях,
     * используя разницу между начальной и конечной датами, независимо от флага isMultiDay
     */
    private fun calculateTripDurationForced(note: FishingNote): Int {
        // Если нет конечной даты, это однодневная рыбалка
        if (note.endDate == null) {
            Log.d(TAG, "Отсутствует конечная дата для заметки: ${note.location}, считаем как 1 день")
            return 1
        }

        try {
            // Разница между начальной и конечной датой
            val diffMillis = note.endDate.time - note.date.time

            // Если разница отрицательная или равна нулю (конечная дата раньше или равна начальной),
            // считаем как однодневную рыбалку
            if (diffMillis <= 0) {
                Log.d(TAG, "Конечная дата не позже начальной для заметки: ${note.location}, считаем как 1 день")
                return 1
            }

            // Конвертируем миллисекунды в дни и добавляем 1 (чтобы включить первый день)
            val days = (diffMillis / (1000 * 60 * 60 * 24)) + 1

            Log.d(TAG, "Рассчитанная продолжительность для заметки ${note.location}: $days дней " +
                    "(разница в мс: $diffMillis)")

            return days.toInt()
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при расчете продолжительности: ${e.message}", e)
            return 1
        }
    }

    /**
     * Находит лучший месяц по количеству пойманной рыбы в указанном диапазоне дат
     */
    private fun findBestMonth(notes: List<FishingNote>, startDate: Date, endDate: Date): BestMonthInfo? {
        // Создаем карту для подсчета рыбы по месяцам
        val fishCountByMonth = mutableMapOf<Pair<Int, Int>, Int>() // Пара (год, месяц) -> количество рыбы

        // Перебираем все заметки и их поклевки
        notes.forEach { note ->
            note.biteRecords
                .filter { bite -> isDateInRange(bite.time, startDate, endDate) } // Фильтруем по дате
                .forEach { bite ->
                    val calendar = Calendar.getInstance().apply { time = bite.time }
                    val year = calendar.get(Calendar.YEAR)
                    val month = calendar.get(Calendar.MONTH) + 1 // +1 т.к. месяц в Calendar с 0

                    val key = Pair(year, month)
                    fishCountByMonth[key] = (fishCountByMonth[key] ?: 0) + 1
                }
        }

        // Находим месяц с максимальным количеством рыбы
        val bestMonthEntry = fishCountByMonth.maxByOrNull { it.value }

        return bestMonthEntry?.let { (yearMonth, count) ->
            BestMonthInfo(
                year = yearMonth.first,
                month = yearMonth.second,
                fishCount = count
            )
        }
    }

    /**
     * Находит последние трофеи (фото пойманных рыб) в указанном диапазоне дат
     */
    private fun findLastTrophies(notes: List<FishingNote>, startDate: Date, endDate: Date): List<TrophyInfo> {
        // Создаем список всех поклевок с фотографиями
        val photoBites = mutableListOf<TrophyInfo>()

        notes.forEach { note ->
            if (isDateInRange(note.date, startDate, endDate) && note.photoUrls.isNotEmpty()) {
                // Если у заметки есть фотографии, считаем их трофеями
                note.photoUrls.forEachIndexed { index, photoUrl ->
                    // Ищем соответствующую запись о поклевке, если есть
                    val matchingBite = if (index < note.biteRecords.size) {
                        val bite = note.biteRecords[index]
                        if (isDateInRange(bite.time, startDate, endDate)) bite else null
                    } else {
                        null
                    }

                    photoBites.add(
                        TrophyInfo(
                            date = matchingBite?.time ?: note.date,
                            photoUrl = photoUrl,
                            fishType = matchingBite?.fishType ?: "",
                            weight = matchingBite?.weight ?: 0f
                        )
                    )
                }
            }
        }

        // Сортируем по дате (новые в начале) и берем последние 3
        return photoBites.sortedByDescending { it.date }.take(3)
    }
}