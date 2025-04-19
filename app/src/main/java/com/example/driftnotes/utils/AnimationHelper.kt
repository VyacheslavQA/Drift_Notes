package com.example.driftnotes.utils

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityOptionsCompat
import com.example.driftnotes.R

/**
 * Вспомогательный класс для анимаций переходов между активностями
 */
object AnimationHelper {

    /**
     * Запуск активности с анимацией слайда справа
     */
    fun startActivityWithAnimation(activity: Activity, intent: Intent) {
        activity.startActivity(intent)
        // Хотя метод устарел, мы продолжаем его использовать для поддержки старых версий Android
        @Suppress("DEPRECATION")
        activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    /**
     * Запуск активности с анимацией слайда снизу
     */
    fun startActivityWithUpAnimation(activity: Activity, intent: Intent) {
        activity.startActivity(intent)
        // Используем новые анимации для перехода снизу вверх
        @Suppress("DEPRECATION")
        activity.overridePendingTransition(R.anim.slide_in_bottom, R.anim.slide_out_top)
    }

    /**
     * Завершение активности с анимацией слайда влево (как назад)
     */
    fun finishWithAnimation(activity: Activity) {
        activity.finish()
        // Хотя метод устарел, мы продолжаем его использовать для поддержки старых версий Android
        @Suppress("DEPRECATION")
        activity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    /**
     * Завершение активности с анимацией слайда вниз
     */
    fun finishWithDownAnimation(activity: Activity) {
        activity.finish()
        @Suppress("DEPRECATION")
        activity.overridePendingTransition(R.anim.slide_in_top, R.anim.slide_out_bottom)
    }
    /**
     * Безопасное завершение активности с анимацией
     */
    fun safeFinishWithAnimation(activity: Activity) {
        try {
            activity.finish()
            // Используем проверку isFinishing, чтобы избежать ошибок при анимации
            if (!activity.isFinishing) {
                @Suppress("DEPRECATION")
                activity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            }
        } catch (e: Exception) {
            // Если возникла ошибка с анимацией, просто завершаем активность
            try {
                activity.finish()
            } catch (ignored: Exception) {
                // Игнорируем любые ошибки - активность уже может быть уничтожена
            }
        }
    }
}