package com.example.driftnotes.utils

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.storage.FirebaseStorage

/**
 * Singleton объект для управления Firebase сервисами в приложении.
 * Обеспечивает централизованный доступ к Firebase функциональности.
 */
object FirebaseManager {
    // Тег для логирования
    private const val TAG = "FirebaseManager"

    // Firebase Analytics
    lateinit var analytics: FirebaseAnalytics
        private set

    // Firebase Authentication
    lateinit var auth: FirebaseAuth
        private set

    // Firebase Firestore (база данных)
    lateinit var firestore: FirebaseFirestore
        private set

    // Firebase Storage (для хранения фотографий)
    lateinit var storage: FirebaseStorage
        private set

    // Флаг, указывающий, включен ли режим офлайн-работы
    private var offlineModeEnabled = false

    /**
     * Инициализирует все необходимые Firebase сервисы.
     * Должен вызываться один раз при запуске приложения.
     *
     * @param context Контекст приложения
     */
    fun initialize(context: Context) {
        try {
            // Firebase уже инициализируется автоматически через google-services.json,
            // но можно убедиться, что приложение инициализировано
            if (!::analytics.isInitialized) {
                // Убедимся, что Firebase инициализирован
                FirebaseApp.initializeApp(context)

                // Инициализация Analytics
                analytics = FirebaseAnalytics.getInstance(context)

                // Инициализация Auth
                auth = FirebaseAuth.getInstance()

                // Инициализация Firestore с настройками офлайн-режима
                firestore = FirebaseFirestore.getInstance()

                // Настраиваем Firestore для работы в офлайн-режиме
                setupFirestoreOfflineMode()

                // Инициализация Storage
                storage = FirebaseStorage.getInstance()

                Log.d(TAG, "Firebase сервисы успешно инициализированы")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при инициализации Firebase: ${e.message}", e)
        }
    }

    /**
     * Настраивает Firestore для работы в офлайн-режиме
     */
    private fun setupFirestoreOfflineMode() {
        try {
            // Создаем настройки с включенным кэшированием данных
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build()

            // Применяем настройки
            firestore.firestoreSettings = settings

            // Включаем автоматическую синхронизацию в офлайн-режиме
            firestore.enableNetwork().addOnSuccessListener {
                Log.d(TAG, "Firestore сетевой режим включен")
            }.addOnFailureListener { e ->
                Log.e(TAG, "Ошибка при включении сетевого режима Firestore: ${e.message}", e)
                // В случае ошибки принудительно включаем офлайн-режим
                disableNetworkAndEnableOfflineMode()
            }

            offlineModeEnabled = true
            Log.d(TAG, "Firestore настроен для работы в офлайн-режиме")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при настройке офлайн-режима Firestore: ${e.message}", e)
        }
    }

    /**
     * Принудительно отключает сетевую работу Firestore и переводит в офлайн-режим
     */
    private fun disableNetworkAndEnableOfflineMode() {
        try {
            firestore.disableNetwork().addOnSuccessListener {
                Log.d(TAG, "Firestore сетевой режим отключен, работа в офлайн-режиме")
                offlineModeEnabled = true
            }.addOnFailureListener { e ->
                Log.e(TAG, "Ошибка при отключении сетевого режима Firestore: ${e.message}", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при переходе в офлайн-режим: ${e.message}", e)
        }
    }

    /**
     * Проверяет, активен ли офлайн-режим для Firestore
     */
    fun isOfflineModeEnabled(): Boolean {
        return offlineModeEnabled
    }

    /**
     * Логирует событие в Firebase Analytics.
     *
     * @param eventName Название события
     * @param params Параметры события (опционально)
     */
    fun logEvent(eventName: String, params: Map<String, String>? = null) {
        if (::analytics.isInitialized) {
            val bundle = android.os.Bundle()
            params?.forEach { (key, value) ->
                bundle.putString(key, value)
            }
            analytics.logEvent(eventName, bundle)
        }
    }

    /**
     * Проверяет, авторизован ли текущий пользователь.
     *
     * @return true если пользователь авторизован, иначе false
     */
    fun isUserLoggedIn(): Boolean {
        return ::auth.isInitialized && auth.currentUser != null
    }

    /**
     * Получает ID текущего пользователя.
     *
     * @return ID пользователя или null, если пользователь не авторизован
     */
    fun getCurrentUserId(): String? {
        return if (isUserLoggedIn()) {
            auth.currentUser?.uid
        } else {
            null
        }
    }

    /**
     * Проверяет наличие интернет-соединения и если его нет, переключает Firestore в офлайн-режим
     *
     * @param context Контекст приложения
     * @return true если есть интернет-соединение, иначе false
     */
    fun checkNetworkAndSwitchToOfflineModeIfNeeded(context: Context): Boolean {
        val isNetworkAvailable = NetworkUtils.isNetworkAvailable(context)

        if (!isNetworkAvailable && ::firestore.isInitialized) {
            disableNetworkAndEnableOfflineMode()
        } else if (isNetworkAvailable && ::firestore.isInitialized && offlineModeEnabled) {
            // Если соединение восстановлено и ранее был офлайн-режим, включаем сеть
            firestore.enableNetwork().addOnSuccessListener {
                Log.d(TAG, "Firestore сетевой режим восстановлен")
            }
        }

        return isNetworkAvailable
    }
}