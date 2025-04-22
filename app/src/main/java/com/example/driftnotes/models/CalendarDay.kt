package com.example.driftnotes.models

import java.util.Date

/**
 * Модель данных для дня в календаре с информацией о событиях
 */
data class CalendarDay(
    val date: Date,                 // Дата
    val hasFishingNote: Boolean = false,     // Есть ли записи о рыбалке
    val hasPlannedTrip: Boolean = false,     // Есть ли запланированная рыбалка
    val hasBiteForecast: Boolean = false,    // Есть ли прогноз клёва
    val biteRating: Int = 0,                 // Оценка клёва (1-5), если есть
    val fishingNoteId: String = "",          // ID записи о рыбалке (если есть)
    val plannedTripId: String = "",          // ID запланированной рыбалки (если есть)
    val biteForecastId: String = ""          // ID прогноза клёва (если есть)
)