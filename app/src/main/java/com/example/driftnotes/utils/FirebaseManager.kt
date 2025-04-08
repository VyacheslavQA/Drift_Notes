package com.example.driftnotes.utils

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

/**
 * Singleton объект для управления Firebase сервисами в приложении.
 * Обеспечивает централизованный доступ к Firebase функциональности.
 */
object FirebaseManager {
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
    
    /**
     * Инициализирует все необходимые Firebase сервисы.
     * Должен вызываться один раз при запуске приложения.
     *
     * @param context Контекст приложения
     */
    fun initialize(context: Context) {
        // Firebase уже инициализируется автоматически через google-services.json,
        // но можно убедиться, что приложение инициализировано
        if (!::analytics.isInitialized) {
            // Убедимся, что Firebase инициализирован
            FirebaseApp.initializeApp(context)
            
            // Инициализация Analytics
            analytics = FirebaseAnalytics.getInstance(context)
            
            // Инициализация Auth
            auth = FirebaseAuth.getInstance()
            
            // Инициализация Firestore
            firestore = FirebaseFirestore.getInstance()
            
            // Инициализация Storage
            storage = FirebaseStorage.getInstance()
        }
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
}