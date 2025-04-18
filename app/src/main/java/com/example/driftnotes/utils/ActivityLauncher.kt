package com.example.driftnotes.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.driftnotes.R
import com.example.driftnotes.fishing.AddFishingNoteActivity

/**
 * Вспомогательный класс для запуска активностей с гарантированной обработкой ошибок
 */
object ActivityLauncher {

    /**
     * Запускает активность добавления заметки с правильной анимацией
     * и обработкой ошибок
     */
    fun launchAddFishingNote(context: Context) {
        try {
            // Прямое создание Intent для активности
            val intent = Intent(context, AddFishingNoteActivity::class.java)

            // Добавляем флаг, который поможет решить проблемы с запуском активности
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            if (context is Activity) {
                // Запуск с анимацией, если контекст - это активность
                context.startActivity(intent)
                context.overridePendingTransition(R.anim.slide_in_bottom, R.anim.slide_out_top)
            } else {
                // Обычный запуск, если контекст - не активность
                context.startActivity(intent)
            }

            // Для отладки
            Toast.makeText(context, "Запуск активности добавления заметки", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            // Показываем подробную информацию об ошибке
            Toast.makeText(
                context,
                "Ошибка запуска активности: ${e.message}",
                Toast.LENGTH_LONG
            ).show()

            // Логируем ошибку
            android.util.Log.e("ActivityLauncher", "Ошибка запуска AddFishingNoteActivity", e)
        }
    }
}