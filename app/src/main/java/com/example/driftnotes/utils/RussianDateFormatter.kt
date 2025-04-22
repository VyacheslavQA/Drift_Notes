package com.example.driftnotes.utils

import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Утилитный класс для форматирования дат с учетом русского языка
 */
object RussianDateFormatter {

    // Месяцы в именительном падеже
    private val monthsInNominative = mapOf(
        0 to "Январь",
        1 to "Февраль",
        2 to "Март",
        3 to "Апрель",
        4 to "Май",
        5 to "Июнь",
        6 to "Июль",
        7 to "Август",
        8 to "Сентябрь",
        9 to "Октябрь",
        10 to "Ноябрь",
        11 to "Декабрь"
    )

    // Месяцы в родительном падеже
    private val monthsInGenitive = mapOf(
        0 to "января",
        1 to "февраля",
        2 to "марта",
        3 to "апреля",
        4 to "мая",
        5 to "июня",
        6 to "июля",
        7 to "августа",
        8 to "сентября",
        9 to "октября",
        10 to "ноября",
        11 to "декабря"
    )

    /**
     * Получает название месяца в именительном падеже
     */
    fun getMonthInNominative(monthIndex: Int): String {
        return monthsInNominative[monthIndex] ?: "Неизвестный месяц"
    }

    /**
     * Получает название месяца в родительном падеже
     */
    fun getMonthInGenitive(monthIndex: Int): String {
        return monthsInGenitive[monthIndex] ?: "неизвестного месяца"
    }

    /**
     * Форматирует дату в формате "31 декабря"
     */
    fun formatDateWithGenitiveMonth(date: Date): String {
        val calendar = Calendar.getInstance()
        calendar.time = date

        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = getMonthInGenitive(calendar.get(Calendar.MONTH))

        return "$day $month"
    }

    /**
     * Возвращает правильную форму слова "день" в зависимости от количества
     */
    fun getDaysText(days: Int): String {
        return when {
            days % 10 == 1 && days % 100 != 11 -> "день"
            days % 10 in 2..4 && (days % 100 < 10 || days % 100 > 20) -> "дня"
            else -> "дней"
        }
    }

    /**
     * Возвращает правильную форму слова "рыба" в зависимости от количества
     */
    fun getFishText(count: Int): String {
        return when {
            count % 10 == 1 && count % 100 != 11 -> "рыба"
            count % 10 in 2..4 && (count % 100 < 10 || count % 100 > 20) -> "рыбы"
            else -> "рыб"
        }
    }

    /**
     * Возвращает правильную форму слова "рыбалка" в зависимости от количества
     */
    fun getFishingTripsText(count: Int): String {
        return when {
            count % 10 == 1 && count % 100 != 11 -> "рыбалка"
            count % 10 in 2..4 && (count % 100 < 10 || count % 100 > 20) -> "рыбалки"
            else -> "рыбалок"
        }
    }

    /**
     * Форматирует диапазон дат в формате "12-15 августа" или "30 апреля - 2 мая"
     */
    fun formatDateRange(startDate: Date, endDate: Date): String {
        val startCalendar = Calendar.getInstance()
        startCalendar.time = startDate

        val endCalendar = Calendar.getInstance()
        endCalendar.time = endDate

        val startDay = startCalendar.get(Calendar.DAY_OF_MONTH)
        val startMonth = startCalendar.get(Calendar.MONTH)
        val startYear = startCalendar.get(Calendar.YEAR)

        val endDay = endCalendar.get(Calendar.DAY_OF_MONTH)
        val endMonth = endCalendar.get(Calendar.MONTH)
        val endYear = endCalendar.get(Calendar.YEAR)

        return when {
            // Если месяц и год одинаковые: "12-15 апреля 2023"
            startMonth == endMonth && startYear == endYear -> {
                "$startDay-$endDay ${getMonthInGenitive(endMonth)} $endYear"
            }
            // Если год одинаковый, но месяцы разные: "30 апреля - 2 мая 2023"
            startYear == endYear -> {
                "$startDay ${getMonthInGenitive(startMonth)} - $endDay ${getMonthInGenitive(endMonth)} $endYear"
            }
            // Если годы разные: "30 декабря 2022 - 2 января 2023"
            else -> {
                "$startDay ${getMonthInGenitive(startMonth)} $startYear - $endDay ${getMonthInGenitive(endMonth)} $endYear"
            }
        }
    }
}