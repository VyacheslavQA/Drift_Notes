package com.example.driftnotes.api

import com.example.driftnotes.BuildConfig
import com.example.driftnotes.models.YandexWeatherResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * Интерфейс для API Яндекс.Погоды
 */
interface YandexWeatherApi {
    @GET("v2/forecast")
    suspend fun getWeather(
        @Header("X-Yandex-API-Key") apiKey: String = BuildConfig.YANDEX_WEATHER_API_KEY,
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("lang") language: String = "ru_RU"
    ): Response<YandexWeatherResponse>

    // Альтернативный вариант с использованием заголовка X-Yandex-Weather-Key
    @GET("v2/forecast")
    suspend fun getWeatherAlternative(
        @Header("X-Yandex-Weather-Key") apiKey: String = BuildConfig.YANDEX_WEATHER_API_KEY,
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("lang") language: String = "ru_RU"
    ): Response<YandexWeatherResponse>
}

/**
 * Синглтон для доступа к API Яндекс.Погоды
 */
object WeatherApiService {
    private const val BASE_URL = "https://api.weather.yandex.ru/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val weatherApi: YandexWeatherApi = retrofit.create(YandexWeatherApi::class.java)
}