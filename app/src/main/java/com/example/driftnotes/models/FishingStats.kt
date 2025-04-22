package com.example.driftnotes.models

import java.util.Date

/**
 * Модель данных для статистики рыбалки
 */
data class FishingStats(
    val totalFishingTrips: Int = 0,                   // Общее количество рыбалок
    val totalFishCaught: Int = 0,                     // Общее количество пойманной рыбы
    val averageFishPerTrip: Float = 0f,               // Среднее количество рыбы за одну рыбалку
    val biggestFish: BiggestFishInfo? = null,         // Информация о самой большой рыбе
    val longestTrip: LongestTripInfo? = null,         // Информация о самой долгой рыбалке
    val bestMonth: BestMonthInfo? = null,             // Информация о лучшем месяце
    val lastTrip: LastTripInfo? = null,               // Информация о последней рыбалке
    val lastTrophies: List<TrophyInfo> = emptyList()  // Список последних трофеев
)

/**
 * Информация о самой большой рыбе
 */
data class BiggestFishInfo(
    val weight: Float = 0f,       // Вес рыбы в килограммах
    val fishType: String = "",    // Вид рыбы
    val date: Date = Date(),      // Дата поимки
    val location: String = "",    // Место поимки
    val photoUrl: String = ""     // URL фотографии (опционально)
)

/**
 * Информация о самой долгой рыбалке
 */
data class LongestTripInfo(
    val durationDays: Int = 0,    // Длительность в днях
    val startDate: Date = Date(), // Дата начала
    val endDate: Date = Date(),   // Дата окончания
    val location: String = ""     // Место рыбалки
)

/**
 * Информация о лучшем месяце
 */
data class BestMonthInfo(
    val month: Int = 0,           // Месяц (1-12)
    val year: Int = 0,            // Год
    val fishCount: Int = 0        // Количество пойманной рыбы
)

/**
 * Информация о последней рыбалке
 */
data class LastTripInfo(
    val date: Date = Date(),      // Дата рыбалки
    val location: String = ""     // Место рыбалки
)

/**
 * Информация о трофее (пойманной рыбе)
 */
data class TrophyInfo(
    val date: Date = Date(),      // Дата поимки
    val photoUrl: String = "",    // URL фотографии
    val fishType: String = "",    // Вид рыбы (опционально)
    val weight: Float = 0f        // Вес рыбы (опционально)
)