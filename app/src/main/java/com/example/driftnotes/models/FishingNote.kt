// Путь: app/src/main/java/com/example/driftnotes/models/FishingNote.kt
package com.example.driftnotes.models

import java.util.Date

data class FishingNote(
    val id: String = "",
    val userId: String = "",
    val location: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val date: Date = Date(),         // Начальная дата рыбалки
    val endDate: Date? = null,       // Конечная дата рыбалки (null для однодневной)
    val isMultiDay: Boolean = false, // Флаг многодневной рыбалки
    val tackle: String = "",         // Теперь необязательное поле
    val notes: String = "",          // Теперь необязательное поле
    val photoUrls: List<String> = listOf(),
    val fishingType: String = "",    // Тип рыбалки
    val weather: FishingWeather? = null, // Информация о погоде
    val markerMapId: String = "",    // ID маркерной карты
    val biteRecords: List<BiteRecord> = listOf(), // Список поклевок для всех дней
    val dayBiteMaps: Map<String, List<String>> = mapOf() // Карта связи дат и ID поклевок для разных дней
)