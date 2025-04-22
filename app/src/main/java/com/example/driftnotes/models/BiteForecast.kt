package com.example.driftnotes.models

import java.util.Date

/**
 * Модель данных для прогноза клёва
 */
data class BiteForecast(
    val id: String = "",
    val date: Date = Date(),         // Дата прогноза
    val rating: Int = 0,             // Оценка клёва (1-5)
    val weatherDescription: String = "", // Описание погоды
    val pressure: Double = 0.0,      // Давление
    val windSpeed: Double = 0.0,     // Скорость ветра
    val cloudCover: Int = 0,         // Облачность в процентах
    val moonPhase: String = "",      // Фаза луны
    val description: String = "",    // Текстовое описание прогноза
    val createdAt: Date = Date()     // Дата создания прогноза
)