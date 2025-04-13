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
     * Завершение активности с анимацией слайда влево (как назад)
     */
    fun finishWithAnimation(activity: Activity) {
        activity.finish()
        // Хотя метод устарел, мы продолжаем его использовать для поддержки старых версий Android
        @Suppress("DEPRECATION")
        activity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}