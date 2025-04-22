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
     */
    suspend fun getFishingStats(): Result<FishingStats> {
        return try {
            val userId = FirebaseManager.getCurrentUserId() ?: throw Exception("Пользователь не авторизован")

            // Получаем все заметки пользователя, отсортированные по дате (новые в начале)
            val notesSnapshot = FirebaseManager.firestore.collection("fishing_notes")
                .whereEqualTo("userId", userId)
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .await()

            val notes = notesSnapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(FishingNote::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка при преобразовании заметки: ${e.message}")
                    null
                }
            }

            if (notes.isEmpty()) {
                // Если у пользователя нет заметок, возвращаем пустую статистику
                return Result.success(FishingStats())
            }

            // Общее количество рыбалок
            val totalTrips = notes.size

            // Общее количество пойманной рыбы (суммируем из всех записей поклевок)
            val totalFish = notes.sumOf { note -> note.biteRecords.size }

            // Среднее количество рыбы за одну рыбалку
            val averageFish = if (totalTrips > 0) {
                totalFish.toFloat() / totalTrips.toFloat()
            } else {
                0f
            }

            // Самая большая рыба (находим запись с максимальным весом)
            val biggestFish = findBiggestFish(notes)

            // Самая долгая рыбалка
            val longestTrip = findLongestTrip(notes)

            // Лучший месяц
            val bestMonth = findBestMonth(notes)

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
            val lastTrophies = findLastTrophies(notes)

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
     * Находит информацию о самой большой рыбе
     */
    private fun findBiggestFish(notes: List<FishingNote>): BiggestFishInfo? {
        // Собираем все записи о поклевках из всех заметок
        val allBites = notes.flatMap { note ->
            note.biteRecords.map { bite ->
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
     * Находит информацию о самой долгой рыбалке
     */
    private fun findLongestTrip(notes: List<FishingNote>): LongestTripInfo? {
        // Находим заметку с максимальной длительностью
        val longestTripNote = notes.filter { it.isMultiDay && it.endDate != null }
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
     * Находит лучший месяц по количеству пойманной рыбы
     */
    private fun findBestMonth(notes: List<FishingNote>): BestMonthInfo? {
        // Создаем карту для подсчета рыбы по месяцам
        val fishCountByMonth = mutableMapOf<Pair<Int, Int>, Int>() // Пара (год, месяц) -> количество рыбы

        // Перебираем все заметки и их поклевки
        notes.forEach { note ->
            note.biteRecords.forEach { bite ->
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
     * Находит последние трофеи (фото пойманных рыб)
     */
    private fun findLastTrophies(notes: List<FishingNote>): List<TrophyInfo> {
        // Создаем список всех поклевок с фотографиями
        val photoBites = mutableListOf<TrophyInfo>()

        notes.forEach { note ->
            // Если у заметки есть фотографии, считаем их трофеями
            if (note.photoUrls.isNotEmpty()) {
                note.photoUrls.forEachIndexed { index, photoUrl ->
                    // Ищем соответствующую запись о поклевке, если есть
                    val matchingBite = if (index < note.biteRecords.size) {
                        note.biteRecords[index]
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