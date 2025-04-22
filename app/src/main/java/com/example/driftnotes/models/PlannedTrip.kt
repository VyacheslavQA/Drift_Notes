package com.example.driftnotes.models

import java.util.Date

/**
 * Модель данных для запланированной рыбалки
 */
data class PlannedTrip(
    val id: String = "",
    val userId: String = "",
    val date: Date = Date(),         // Дата запланированной рыбалки
    val endDate: Date? = null,       // Конечная дата (для многодневной рыбалки)
    val isMultiDay: Boolean = false, // Флаг многодневной рыбалки
    val location: String = "",       // Место рыбалки
    val note: String = "",           // Заметка/комментарий
    val fishingType: String = "",    // Тип рыбалки (если известен)
    val createdAt: Date = Date()     // Дата создания плана
)