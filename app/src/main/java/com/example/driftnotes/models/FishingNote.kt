package com.example.driftnotes.models

import java.util.Date

data class FishingNote(
    val id: String = "",
    val userId: String = "",
    val location: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val date: Date = Date(),
    val tackle: String = "",
    val notes: String = "",
    val photoUrls: List<String> = listOf(),
    val fishingType: String = "",  // Тип рыбалки
    val weather: FishingWeather? = null, // Информация о погоде
    val markerMapId: String = "", // ID маркерной карты
    val biteRecords: List<BiteRecord> = listOf() // Список поклевок
)