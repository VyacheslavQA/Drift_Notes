package com.example.driftnotes.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log

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
            val firestoreSettings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                .isPersistenceEnabled()
                .build()

            return firestoreSettings.isPersistenceEnabled
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при проверке режима офлайн-работы Firestore: ${e.message}", e)
            return false
        }
    }
}