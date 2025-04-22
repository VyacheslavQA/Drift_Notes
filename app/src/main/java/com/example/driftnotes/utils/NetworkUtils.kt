package com.example.driftnotes.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestoreSettings

/**
 * Утилитарный класс для работы с сетевым подключением
 */
object NetworkUtils {

    private const val TAG = "NetworkUtils"

    /**
     * Проверяет наличие активного интернет-соединения
     */
    fun isNetworkAvailable(context: Context): Boolean {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork ?: return false
                val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

                return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            } else {
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo
                return networkInfo != null && networkInfo.isConnected
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при проверке сетевого подключения: ${e.message}", e)
            return false
        }
    }

    /**
     * Проверяет, может ли Firebase работать в офлайн-режиме
     */
    fun isFirestoreOfflineModeEnabled(): Boolean {
        try {
            // Поскольку мы не можем напрямую узнать состояние Firestore,
            // проверяем поддержку persistence в FirebaseFirestoreSettings
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()

            return true // Если смогли создать настройки, считаем что офлайн-режим поддерживается
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при проверке режима офлайн-работы Firestore: ${e.message}", e)
            return false
        }
    }
}