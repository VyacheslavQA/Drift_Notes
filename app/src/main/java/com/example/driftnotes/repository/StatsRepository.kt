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
import java.util.Calendar
import java.util.Date

/**
 * Репозиторий для получения статистики рыбалок
 */
class StatsRepository {
    private val TAG = "StatsRepository"

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

            // Получаем все заметки пользователя в указанном диапазоне дат
            val notesSnapshot = FirebaseManager.firestore.collection("fishing_notes")
                .whereEqualTo("userId", userId)
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .await()

            // Преобразуем документы в объекты FishingNote
            val allNotes = notesSnapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(FishingNote::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка при преобразовании заметки: ${e.message}")
                    null
                }
            }

            // Фильтруем заметки по диапазону дат
            val notes = allNotes.filter { note ->
                // Для однодневных заметок проверяем, что дата в диапазоне
                if (!note.isMultiDay || note.endDate == null) {
                    isDateInRange(note.date, startDate, endDate)
                } else {
                    // Для многодневных заметок проверяем пересечение диапазонов
                    isDateRangeOverlapping(note.date, note.endDate, startDate, endDate)
                }
            }

            if (notes.isEmpty()) {
                // Если у пользователя нет заметок, возвращаем пустую статистику
                return Result.success(FishingStats())
            }

            // Общее количество рыбалок
            val totalTrips = notes.size

            // Фильтруем поклевки по диапазону дат
            val totalFish = notes.sumOf { note ->
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
            val biggestFish = findBiggestFish(notes, startDate, endDate)

            // Самая долгая рыбалка
            val longestTrip = findLongestTrip(notes, startDate, endDate)

            // Лучший месяц
            val bestMonth = findBestMonth(notes, startDate, endDate)

            // Последняя рыбалка (первая запись, так как сортировка по убыванию)
            val lastTrip = if (notes.isNotEmpty()) {
                LastTripInfo(
                    date = notes.first().date,
                    location = notes.first().location
                )
            } else {
                null
            }

            // Последние трофеи (фото)
            val lastTrophies = findLastTrophies(notes, startDate, endDate)

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
            Log.e(TAG, "Ошибка при получении статистики: ${e.message}")
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
     * Находит информацию о самой долгой рыбалке в указанном диапазоне дат
     */
    private fun findLongestTrip(notes: List<FishingNote>, startDate: Date, endDate: Date): LongestTripInfo? {
        // Находим заметку с максимальной длительностью
        val longestTripNote = notes
            .filter { it.isMultiDay && it.endDate != null && isDateRangeOverlapping(it.date, it.endDate, startDate, endDate) }
            .maxByOrNull { note ->
                val startCalendar = Calendar.getInstance().apply { time = note.date }
                val endCalendar = Calendar.getInstance().apply { time = note.endDate!! }

                // Вычисляем разницу в днях
                val diffMillis = endCalendar.timeInMillis - startCalendar.timeInMillis
                (diffMillis / (1000 * 60 * 60 * 24)).toInt() + 1 // +1 чтобы учесть первый день
            }

        return longestTripNote?.let { note ->
            val startCalendar = Calendar.getInstance().apply { time = note.date }
            val endCalendar = Calendar.getInstance().apply { time = note.endDate!! }

            // Вычисляем разницу в днях
            val diffMillis = endCalendar.timeInMillis - startCalendar.timeInMillis
            val durationDays = (diffMillis / (1000 * 60 * 60 * 24)).toInt() + 1 // +1 чтобы учесть первый день

            LongestTripInfo(
                durationDays = durationDays,
                startDate = note.date,
                endDate = note.endDate!!,
                location = note.location
            )
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