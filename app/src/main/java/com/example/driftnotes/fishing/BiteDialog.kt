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
 * Диалог для добавления/редактирования поклевки
 */
class BiteDialog(
    context: Context,
    private val date: Date, // Дата рыбалки (конкретный день)
    private val existingBite: BiteRecord? = null, // Существующая поклевка для редактирования
    private val spotIndex: Int = 0, // Индекс точки ловли
    private val onBiteAdded: (BiteRecord) -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogAddBiteBinding
    private val calendar = Calendar.getInstance()
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

        // Меняем заголовок в зависимости от режима (добавление/редактирование)
        binding.textViewDialogTitle.text = if (existingBite != null)
            "Редактировать поклёвку" else "Добавить поклёвку"

        // Если редактируем существующую поклевку, заполняем поля
        if (existingBite != null) {
            // Устанавливаем время из существующей поклевки
            calendar.time = existingBite.time

            // Заполняем все поля
            binding.editTextFishType.setText(existingBite.fishType)
            if (existingBite.weight > 0) {
                binding.editTextWeight.setText(existingBite.weight.toString())
            }
            binding.editTextNotes.setText(existingBite.notes)

            // Меняем текст кнопки
            binding.buttonSaveBite.text = "Сохранить изменения"
        } else {
            // В режиме добавления устанавливаем дату из переданного параметра
            // но время берем текущее
            calendar.time = Date()

            // Получаем день, месяц, год из переданной даты
            val dateCalendar = Calendar.getInstance()
            dateCalendar.time = date

            // Устанавливаем день, месяц, год из даты выбранного дня рыбалки
            calendar.set(Calendar.YEAR, dateCalendar.get(Calendar.YEAR))
            calendar.set(Calendar.MONTH, dateCalendar.get(Calendar.MONTH))
            calendar.set(Calendar.DAY_OF_MONTH, dateCalendar.get(Calendar.DAY_OF_MONTH))
        }

        // Обновляем отображение времени
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

        // Сохраняем индекс дня и точки из существующей поклевки или используем текущие
        val dayIndex = existingBite?.dayIndex ?: 0
        val spotIndex = existingBite?.spotIndex ?: this.spotIndex

        // Создаем или обновляем объект поклевки
        val biteRecord = if (existingBite != null) {
            // Обновляем существующую, сохраняя ID, dayIndex и spotIndex
            existingBite.copy(
                time = calendar.time,
                fishType = fishType,
                weight = weight,
                notes = notes,
                dayIndex = dayIndex,
                spotIndex = spotIndex
            )
        } else {
            // Создаем новую
            BiteRecord(
                id = UUID.randomUUID().toString(),
                time = calendar.time,
                fishType = fishType,
                weight = weight,
                notes = notes,
                dayIndex = dayIndex,
                spotIndex = spotIndex
            )
        }

        // Вызываем callback
        onBiteAdded(biteRecord)

        // Закрываем диалог
        dismiss()
    }
}