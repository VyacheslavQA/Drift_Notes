package com.example.driftnotes.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.animation.AnimationUtils
import android.widget.PopupMenu
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
        activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    /**
     * Запуск активности для получения результата с анимацией слайда справа
     */
    fun startActivityForResultWithAnimation(activity: Activity, intent: Intent, requestCode: Int) {
        activity.startActivityForResult(intent, requestCode)
        activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    /**
     * Завершение активности с анимацией слайда влево (как назад)
     */
    fun finishWithAnimation(activity: Activity) {
        activity.finish()
        activity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    /**
     * Применяет анимацию к PopupMenu
     */
    fun applyAnimationToPopupMenu(context: Context, popupMenu: PopupMenu) {
        try {
            val menuPopupHelper = PopupMenu::class.java.getDeclaredField("mPopup")
            menuPopupHelper.isAccessible = true
            val popupHelper = menuPopupHelper.get(popupMenu)
            val method = popupHelper.javaClass.getMethod("setForceShowIcon", Boolean::class.java)
            method.invoke(popupHelper, true)

            // Применяем анимацию к всплывающему меню
            val setAnimationStyle = popupHelper.javaClass.getMethod(
                "setAnimationStyle",
                Int::class.java
            )
            setAnimationStyle.invoke(popupHelper, R.style.Theme_DriftNotes_FadeTransition)
        } catch (e: Exception) {
            // Если что-то пошло не так с рефлексией, просто используем меню без анимации
            e.printStackTrace()
        }
    }
}