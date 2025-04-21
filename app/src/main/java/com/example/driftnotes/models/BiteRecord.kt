package com.example.driftnotes.models

import java.util.Date

/**
 * Модель данных для записи о поклевке рыбы
 */
data class BiteRecord(
    val id: String = "",
    val time: Date = Date(),     // Время поклевки
    val fishType: String = "",   // Тип рыбы (необязательно)
    val weight: Float = 0f,      // Вес рыбы (необязательно)
    val notes: String = "",      // Примечания
    val dayIndex: Int = 0,       // Индекс дня для многодневной рыбалки (0 - первый день)
    val spotIndex: Int = 0       // Индекс точки ловли
)