package com.example.driftnotes.models

import com.google.gson.annotations.SerializedName
import java.util.Date

/**
 * Модель ответа Яндекс.Погоды
 */
data class YandexWeatherResponse(
    @SerializedName("now") val timestamp: Long,
    @SerializedName("fact") val fact: WeatherFact,
    @SerializedName("info") val info: WeatherInfo
)

/**
 * Модель фактической погоды
 */
data class WeatherFact(
    @SerializedName("temp") val temperature: Int, // Температура в градусах Цельсия
    @SerializedName("feels_like") val feelsLike: Int, // Ощущаемая температура
    @SerializedName("icon") val icon: String, // Код иконки погоды
    @SerializedName("condition") val condition: String, // Код погодного явления
    @SerializedName("wind_speed") val windSpeed: Double, // Скорость ветра (м/с)
    @SerializedName("wind_dir") val windDirection: String, // Направление ветра
    @SerializedName("pressure_mm") val pressureMm: Int, // Давление (в мм рт. ст.)
    @SerializedName("humidity") val humidity: Int, // Влажность воздуха (%)
    @SerializedName("daytime") val daytime: String, // Светлое или темное время суток
    @SerializedName("season") val season: String, // Время года в данном населенном пункте
    @SerializedName("obs_time") val observationTime: Long // Время замера погодных данных
) {
    /**
     * Получить удобочитаемое название погодного условия
     */
    fun getReadableCondition(): String {
        return when (condition) {
            "clear" -> "Ясно"
            "partly-cloudy" -> "Малооблачно"
            "cloudy" -> "Облачно с прояснениями"
            "overcast" -> "Пасмурно"
            "drizzle" -> "Морось"
            "rain" -> "Дождь"
            "heavy-rain" -> "Сильный дождь"
            "showers" -> "Ливень"
            "wet-snow" -> "Дождь со снегом"
            "light-snow" -> "Небольшой снег"
            "snow" -> "Снег"
            "snow-showers" -> "Снегопад"
            "hail" -> "Град"
            "thunderstorm" -> "Гроза"
            "thunderstorm-with-rain" -> "Дождь с грозой"
            "thunderstorm-with-hail" -> "Гроза с градом"
            else -> condition
        }
    }

    /**
     * Получить читаемое направление ветра
     */
    fun getReadableWindDirection(): String {
        return when (windDirection) {
            "nw" -> "СЗ"
            "n" -> "С"
            "ne" -> "СВ"
            "e" -> "В"
            "se" -> "ЮВ"
            "s" -> "Ю"
            "sw" -> "ЮЗ"
            "w" -> "З"
            "c" -> "Штиль"
            else -> windDirection
        }
    }

    /**
     * Полный текст о погоде
     */
    fun getWeatherSummary(): String {
        return "${getReadableCondition()}, ${temperature}°C, " +
                "ощущается как ${feelsLike}°C\n" +
                "Ветер: ${getReadableWindDirection()}, ${windSpeed} м/с\n" +
                "Влажность: ${humidity}%, Давление: ${pressureMm} мм рт.ст."
    }
}

/**
 * Информация о геолокации
 */
data class WeatherInfo(
    @SerializedName("lat") val latitude: Double,
    @SerializedName("lon") val longitude: Double,
    @SerializedName("url") val url: String
)

/**
 * Модель для сохранения погодных данных в заметке о рыбалке
 */
data class FishingWeather(
    val temperature: Int = 0,
    val condition: String = "",
    val windSpeed: Double = 0.0,
    val windDirection: String = "",
    val humidity: Int = 0,
    val pressure: Int = 0,
    val observationTime: Date = Date(),
    val weatherDescription: String = ""
)