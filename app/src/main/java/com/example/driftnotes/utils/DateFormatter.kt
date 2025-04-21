// Полный код для файла app/src/main/java/com/example/driftnotes/utils/DateFormatter.kt
package com.example.driftnotes.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Утилитарный класс для форматирования дат
 */
object DateFormatter {
    private val fullDateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("ru"))
    private val dayFormat = SimpleDateFormat("dd", Locale("ru"))
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale("ru"))

    /**
     * Форматирует одну дату
     */
    fun formatDate(date: Date): String {
        return fullDateFormat.format(date)
    }

    /**
     * Форматирует диапазон дат
     * Примеры:
     * - Если месяц и год одинаковые: "12–14 апреля 2025"
     * - Если год одинаковый, но месяцы разные: "30 апреля – 2 мая 2025"
     * - Если годы разные: "30 декабря 2024 – 2 января 2025"
     */
    fun formatDateRange(startDate: Date, endDate: Date): String {
        val startCalendar = Calendar.getInstance()
        startCalendar.time = startDate

        val endCalendar = Calendar.getInstance()
        endCalendar.time = endDate

        // Проверяем, одинаковый ли год
        val sameYear = startCalendar.get(Calendar.YEAR) == endCalendar.get(Calendar.YEAR)

        // Проверяем, одинаковый ли месяц (только если год одинаковый)
        val sameMonth = sameYear &&
                startCalendar.get(Calendar.MONTH) == endCalendar.get(Calendar.MONTH)

        return when {
            // Если месяц и год одинаковые
            sameMonth -> {
                val startDay = dayFormat.format(startDate)
                val endDay = dayFormat.format(endDate)
                val monthYear = monthYearFormat.format(endDate)
                "$startDay–$endDay $monthYear"
            }
            // Если год одинаковый, но месяцы разные
            sameYear -> {
                val startDateShort = SimpleDateFormat("dd MMMM", Locale("ru")).format(startDate)
                val endDateShort = SimpleDateFormat("dd MMMM", Locale("ru")).format(endDate)
                val year = SimpleDateFormat("yyyy", Locale("ru")).format(endDate)
                "$startDateShort – $endDateShort $year"
            }
            // Если годы разные
            else -> {
                "${fullDateFormat.format(startDate)} – ${fullDateFormat.format(endDate)}"
            }
        }
    }
}