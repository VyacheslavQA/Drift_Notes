package com.example.driftnotes.models

import java.util.Date

data class FishingNote(
    val id: String = "",
    val userId: String = "",
    val location: String = "",
    val latitude: Double = 0.0,  // Добавляем широту
    val longitude: Double = 0.0,  // Добавляем долготу
    val date: Date = Date(),
    val tackle: String = "",
    val notes: String = "",
    val photoUrls: List<String> = listOf()
)