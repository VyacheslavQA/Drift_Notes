package com.example.driftnotes.repository

import android.util.Log
import com.example.driftnotes.BuildConfig
import com.example.driftnotes.api.WeatherApiService
import com.example.driftnotes.models.FishingWeather
import com.example.driftnotes.models.YandexWeatherResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.util.Date

/**
 * Репозиторий для работы с данными о погоде
 */
class WeatherRepository {
    private val weatherApi = WeatherApiService.weatherApi

    /**
     * Получает текущую погоду для указанной локации
     * @param latitude широта
     * @param longitude долгота
     * @return объект с данными о погоде или null в случае ошибки
     */
    suspend fun getWeatherForLocation(latitude: Double, longitude: Double): FishingWeather? {
        return withContext(Dispatchers.IO) {
            try {
                // Сначала пробуем с заголовком X-Yandex-API-Key
                var response = weatherApi.getWeather(
                    apiKey = BuildConfig.YANDEX_WEATHER_API_KEY,
                    latitude = latitude,
                    longitude = longitude
                )

                // Если первый вариант не удался, пробуем с заголовком X-Yandex-Weather-Key
                if (!response.isSuccessful) {
                    Log.d(TAG, "Пробуем альтернативный вариант заголовка")
                    response = weatherApi.getWeatherAlternative(
                        apiKey = BuildConfig.YANDEX_WEATHER_API_KEY,
                        latitude = latitude,
                        longitude = longitude
                    )
                }

                if (response.isSuccessful) {
                    val weatherResponse = response.body()
                    return@withContext convertToFishingWeather(weatherResponse)
                } else {
                    Log.e(TAG, "Error fetching weather: ${response.code()} - ${response.message()}")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception fetching weather", e)
                null
            }
        }
    }

    /**
     * Преобразует ответ API в модель данных приложения
     */
    private fun convertToFishingWeather(weatherResponse: YandexWeatherResponse?): FishingWeather? {
        if (weatherResponse == null) return null

        val fact = weatherResponse.fact

        return FishingWeather(
            temperature = fact.temperature,
            condition = fact.condition,
            windSpeed = fact.windSpeed,
            windDirection = fact.windDirection,
            humidity = fact.humidity,
            pressure = fact.pressureMm,
            observationTime = Date(fact.observationTime * 1000),
            weatherDescription = fact.getWeatherSummary()
        )
    }

    companion object {
        private const val TAG = "WeatherRepository"
    }
}