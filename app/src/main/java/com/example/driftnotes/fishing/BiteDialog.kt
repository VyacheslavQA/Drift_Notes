package com.example.driftnotes.fishing

import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import com.example.driftnotes.databinding.DialogAddBiteBinding
import com.example.driftnotes.models.BiteRecord
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Диалог для добавления новой поклевки
 */
class BiteDialog(
    context: Context,
    private val date: Date, // Дата рыбалки
    private val onBiteAdded: (BiteRecord) -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogAddBiteBinding
    private val calendar = Calendar.getInstance().apply {
        time = Date() // Текущее время по умолчанию
    }
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Убираем заголовок диалога
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        // Инициализация привязки
        binding = DialogAddBiteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Устанавливаем ширину диалога
        window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        // Изначально устанавливаем текущее время
        updateTimeDisplay()

        // Обработчик клика на поле времени
        binding.editTextTime.setOnClickListener {
            showTimePicker()
        }

        // Обработчик кнопки сохранения
        binding.buttonSaveBite.setOnClickListener {
            saveBite()
        }

        // Обработчик кнопки отмены
        binding.buttonCancelBite.setOnClickListener {
            dismiss()
        }
    }

    private fun updateTimeDisplay() {
        binding.editTextTime.setText(timeFormat.format(calendar.time))
    }

    private fun showTimePicker() {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                updateTimeDisplay()
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true // 24-часовой формат
        ).show()
    }

    private fun saveBite() {
        // Получаем данные с формы
        val fishType = binding.editTextFishType.text.toString()
        val weightStr = binding.editTextWeight.text.toString()
        val notes = binding.editTextNotes.text.toString()

        // Преобразуем вес, если он введен
        val weight = if (weightStr.isNotEmpty()) {
            try {
                weightStr.toFloat()
            } catch (e: NumberFormatException) {
                0f
            }
        } else {
            0f
        }

        // Устанавливаем дату рыбалки, но сохраняем выбранное время
        val biteTime = calendar.time
        val fishingDate = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, calendar.get(Calendar.MINUTE))
            set(Calendar.SECOND, 0)
        }.time

        // Создаем объект поклевки
        val biteRecord = BiteRecord(
            id = UUID.randomUUID().toString(),
            time = fishingDate,
            fishType = fishType,
            weight = weight,
            notes = notes
        )

        // Вызываем callback
        onBiteAdded(biteRecord)

        // Закрываем диалог
        dismiss()
    }
}