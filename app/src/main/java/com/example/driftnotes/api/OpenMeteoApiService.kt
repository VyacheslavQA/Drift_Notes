package com.example.driftnotes.api

import com.example.driftnotes.models.OpenMeteoResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * Интерфейс для API Open Meteo
 */
interface OpenMeteoApi {
    @GET("v1/forecast")
    suspend fun getWeather(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,rain,weather_code,cloud_cover,pressure_msl,wind_speed_10m,wind_direction_10m",
        @Query("daily") daily: String = "temperature_2m_max,temperature_2m_min,sunrise,sunset,uv_index_max",
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 1
    ): Response<OpenMeteoResponse>
}

/**
 * Синглтон для доступа к API Open Meteo
 */
object OpenMeteoApiService {
    private const val BASE_URL = "https://api.open-meteo.com/"

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

    val weatherApi: OpenMeteoApi = retrofit.create(OpenMeteoApi::class.java)
}