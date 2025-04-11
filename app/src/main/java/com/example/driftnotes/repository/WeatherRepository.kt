package com.example.driftnotes.repository

import android.util.Log
import com.example.driftnotes.api.OpenMeteoApiService
import com.example.driftnotes.models.FishingWeather
import com.example.driftnotes.models.OpenMeteoResponse
import com.example.driftnotes.models.WeatherCodeHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Репозиторий для работы с данными о погоде
 */
class WeatherRepository {
    private val weatherApi = OpenMeteoApiService.weatherApi
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())

    /**
     * Получает текущую погоду для указанной локации
     * @param latitude широта
     * @param longitude долгота
     * @return объект с данными о погоде или null в случае ошибки
     */
    suspend fun getWeatherForLocation(latitude: Double, longitude: Double): FishingWeather? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Начало запроса погоды для координат: lat=$latitude, lon=$longitude")

                val response = weatherApi.getWeather(
                    latitude = latitude,
                    longitude = longitude
                )

                Log.d(TAG, "Получен ответ от API: isSuccessful=${response.isSuccessful}, код=${response.code()}")

                if (response.isSuccessful) {
                    val weatherResponse = response.body()
                    if (weatherResponse != null) {
                        Log.d(TAG, "Тело ответа получено успешно")
                        return@withContext convertToFishingWeather(weatherResponse)
                    } else {
                        Log.e(TAG, "Тело ответа пустое (null)")
                        return@withContext null
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Тело ошибки недоступно"
                    Log.e(TAG, "Ошибка запроса: ${response.code()} - ${response.message()}")
                    Log.e(TAG, "Подробности ошибки: $errorBody")
                    return@withContext null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Исключение при получении погоды: ${e.javaClass.simpleName} - ${e.message}")
                e.printStackTrace()
                return@withContext null
            }
        }
    }

    /**
     * Преобразует ответ API в модель данных приложения
     */
    private fun convertToFishingWeather(weatherResponse: OpenMeteoResponse?): FishingWeather? {
        try {
            if (weatherResponse == null) {
                Log.e(TAG, "Невозможно преобразовать null-ответ")
                return null
            }

            Log.d(TAG, "Начало преобразования данных погоды")

            val current = weatherResponse.current
            val daily = weatherResponse.daily

            // Логирование полученных данных
            Log.d(TAG, "Текущая температура: ${current.temperature}")
            Log.d(TAG, "Погодный код: ${current.weatherCode}")

            // Время восхода и заката берем из первого дня массива
            val sunrise = if (daily.sunrise.isNotEmpty()) daily.sunrise[0] else ""
            val sunset = if (daily.sunset.isNotEmpty()) daily.sunset[0] else ""

            Log.d(TAG, "Восход: $sunrise, Закат: $sunset")

            val result = FishingWeather(
                temperature = current.temperature,
                feelsLike = current.feelsLike,
                humidity = current.humidity,
                pressure = current.pressure,
                windSpeed = current.windSpeed,
                windDirection = WeatherCodeHelper.getWindDirection(current.windDirection),
                weatherDescription = generateWeatherDescription(current),
                cloudCover = current.cloudCover,
                moonPhase = "Данные недоступны",
                observationTime = try {
                    dateFormat.parse(current.time) ?: Date()
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка при преобразовании даты: ${e.message}")
                    Date()
                },
                sunrise = formatTimeFromIso(sunrise),
                sunset = formatTimeFromIso(sunset),
                isDay = current.isDay == 1
            )

            Log.d(TAG, "Преобразование данных завершено успешно")
            Log.d(TAG, "Сформировано описание погоды: ${result.weatherDescription}")

            return result
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при преобразовании данных погоды: ${e.javaClass.simpleName} - ${e.message}")
            e.printStackTrace()
            return null
        }
    }

    /**
     * Генерирует полное описание погоды
     */
    private fun generateWeatherDescription(current: com.example.driftnotes.models.CurrentWeather): String {
        try {
            val weatherDesc = WeatherCodeHelper.getWeatherDescription(current.weatherCode)
            val windDirection = WeatherCodeHelper.getWindDirection(current.windDirection)

            return "${weatherDesc}, ${current.temperature.toInt()}°C, " +
                    "ощущается как ${current.feelsLike.toInt()}°C\n" +
                    "Ветер: ${windDirection}, ${current.windSpeed} м/с\n" +
                    "Влажность: ${current.humidity}%, Давление: ${(current.pressure / 1.333).toInt()} мм рт.ст.\n" +
                    "Облачность: ${current.cloudCover}%"
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при генерации описания погоды: ${e.message}")
            return "Ошибка при формировании описания погоды"
        }
    }

    /**
     * Форматирует время из формата ISO в читаемый вид
     */
    private fun formatTimeFromIso(isoTime: String): String {
        return try {
            val time = dateFormat.parse(isoTime)
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(time)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка форматирования времени: ${e.message}")
            ""
        }
    }

    companion object {
        private const val TAG = "WeatherRepository"
    }
}