package com.example.driftnotes.models

import java.util.Date

/**
 * Модель данных для записи о поклевке рыбы
 */
data class BiteRecord(
    val id: String = "",
    val time: Date = Date(),  // Время поклевки
    val fishType: String = "", // Тип рыбы (необязательно)
    val weight: Float = 0f,    // Вес рыбы (необязательно)
    val notes: String = ""     // Примечания
)