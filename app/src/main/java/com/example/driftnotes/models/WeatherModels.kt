package com.example.driftnotes.models

import com.google.gson.annotations.SerializedName
import java.util.Date

/**
 * Модель ответа Open Meteo API
 */
data class OpenMeteoResponse(
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    @SerializedName("timezone_abbreviation") val timezoneAbbreviation: String,
    val current: CurrentWeather,
    val daily: DailyWeather
)

/**
 * Модель текущих погодных условий
 */
data class CurrentWeather(
    @SerializedName("time") val time: String,
    @SerializedName("temperature_2m") val temperature: Double,
    @SerializedName("relative_humidity_2m") val humidity: Int,
    @SerializedName("apparent_temperature") val feelsLike: Double,
    @SerializedName("is_day") val isDay: Int,
    @SerializedName("precipitation") val precipitation: Double,
    @SerializedName("rain") val rain: Double,
    @SerializedName("weather_code") val weatherCode: Int,
    @SerializedName("cloud_cover") val cloudCover: Int,
    @SerializedName("pressure_msl") val pressure: Double,
    @SerializedName("wind_speed_10m") val windSpeed: Double,
    @SerializedName("wind_direction_10m") val windDirection: Int
)

/**
 * Модель ежедневных погодных условий
 */
data class DailyWeather(
    @SerializedName("time") val time: List<String>,
    @SerializedName("temperature_2m_max") val temperatureMax: List<Double>,
    @SerializedName("temperature_2m_min") val temperatureMin: List<Double>,
    @SerializedName("sunrise") val sunrise: List<String>,
    @SerializedName("sunset") val sunset: List<String>,
    @SerializedName("uv_index_max") val uvIndexMax: List<Double>
)

/**
 * Вспомогательный класс для работы с кодами погоды
 */
object WeatherCodeHelper {
    /**
     * Получить описание погоды по коду
     */
    fun getWeatherDescription(code: Int): String {
        return when (code) {
            0 -> "Ясно"
            1, 2, 3 -> "Переменная облачность"
            45, 48 -> "Туман"
            51, 53, 55 -> "Морось"
            56, 57 -> "Морось со снегом"
            61, 63, 65 -> "Дождь"
            66, 67 -> "Ледяной дождь"
            71, 73, 75 -> "Снег"
            77 -> "Снежные зерна"
            80, 81, 82 -> "Ливень"
            85, 86 -> "Снежный шквал"
            95 -> "Гроза"
            96, 99 -> "Гроза с градом"
            else -> "Неизвестно"
        }
    }

    /**
     * Получить направление ветра
     */
    fun getWindDirection(degrees: Int): String {
        return when {
            (degrees >= 337.5 || degrees < 22.5) -> "С"
            (degrees >= 22.5 && degrees < 67.5) -> "СВ"
            (degrees >= 67.5 && degrees < 112.5) -> "В"
            (degrees >= 112.5 && degrees < 157.5) -> "ЮВ"
            (degrees >= 157.5 && degrees < 202.5) -> "Ю"
            (degrees >= 202.5 && degrees < 247.5) -> "ЮЗ"
            (degrees >= 247.5 && degrees < 292.5) -> "З"
            (degrees >= 292.5 && degrees < 337.5) -> "СЗ"
            else -> "Неизвестно"
        }
    }
}

/**
 * Модель для сохранения погодных данных в заметке о рыбалке
 */
data class FishingWeather(
    val temperature: Double = 0.0,
    val feelsLike: Double = 0.0,
    val humidity: Int = 0,
    val pressure: Double = 0.0,
    val windSpeed: Double = 0.0,
    val windDirection: String = "",
    val weatherDescription: String = "",
    val cloudCover: Int = 0,
    val moonPhase: String = "",
    val observationTime: Date = Date(),
    val sunrise: String = "",
    val sunset: String = "",
    val isDay: Boolean = true
)