package com.example.driftnotes

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.driftnotes.fishing.AddFishingNoteActivity

/**
 * Активность-посредник для запуска других активностей
 * Решает проблему с запуском активностей из MainActivity
 */
class ProxyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Получаем тип запускаемой активности
        val targetType = intent.getStringExtra("TARGET_TYPE") ?: "NONE"

        try {
            when (targetType) {
                "ADD_NOTE" -> {
                    // Запускаем активность добавления заметки
                    val addNoteIntent = Intent(this, AddFishingNoteActivity::class.java)
                    startActivity(addNoteIntent)

                    // Специальная анимация для этого перехода
                    overridePendingTransition(R.anim.slide_in_bottom, R.anim.slide_out_top)
                }
                else -> {
                    Toast.makeText(this, "Неизвестный тип активности", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка запуска: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            // Завершаем эту активность, чтобы она не оставалась в стеке
            finish()
            // Без анимации
            overridePendingTransition(0, 0)
        }
    }
}