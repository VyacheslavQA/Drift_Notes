package com.example.driftnotes.calendar

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ArrayAdapter
import com.example.driftnotes.R
import com.example.driftnotes.databinding.DialogPlannedTripBinding
import com.example.driftnotes.models.PlannedTrip
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Диалог для добавления/редактирования запланированной рыбалки
 */
class PlannedTripDialog(
    context: Context,
    private val initialTrip: PlannedTrip? = null,
    private val initialDate: Date? = null,
    private val onSaved: (PlannedTrip) -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogPlannedTripBinding

    // Календари для выбора дат
    private val startCalendar = Calendar.getInstance()
    private val endCalendar = Calendar.getInstance()

    // Форматтер для отображения дат
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    // Флаг многодневной рыбалки
    private var isMultiDay = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Инициализируем binding
        binding = DialogPlannedTripBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Настраиваем размер диалога
        window?.apply {
            requestFeature(Window.FEATURE_NO_TITLE)
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }

        // Настраиваем заголовок
        binding.dialogTitle.text = if (initialTrip != null) {
            "Редактирование плана"
        } else {
            "Новая запланированная рыбалка"
        }

        // Инициализируем даты
        when {
            initialTrip != null -> {
                // Если редактируем существующий план
                startCalendar.time = initialTrip.date
                if (initialTrip.isMultiDay && initialTrip.endDate != null) {
                    endCalendar.time = initialTrip.endDate
                    isMultiDay = true
                } else {
                    // Для однодневной рыбалки устанавливаем конечную дату на день позже
                    endCalendar.time = initialTrip.date
                    endCalendar.add(Calendar.DAY_OF_MONTH, 1)
                    isMultiDay = false
                }

                // Заполняем поля диалога
                binding.editTextLocation.setText(initialTrip.location)
                binding.editTextNote.setText(initialTrip.note)
            }
            initialDate != null -> {
                // Если есть начальная дата
                startCalendar.time = initialDate
                endCalendar.time = initialDate
                endCalendar.add(Calendar.DAY_OF_MONTH, 1)
                isMultiDay = false
            }
            else -> {
                // По умолчанию - сегодня
                endCalendar.add(Calendar.DAY_OF_MONTH, 1)
                isMultiDay = false
            }
        }

        // Обновляем отображение дат
        updateDateDisplay()

        // Настраиваем чекбокс многодневной рыбалки
        binding.checkBoxMultiDay.isChecked = isMultiDay
        binding.layoutEndDate.visibility = if (isMultiDay) View.VISIBLE else View.GONE

        // Настраиваем выпадающий список типов рыбалки
        setupFishingTypeDropdown()

        // Если редактируем существующий план, выбираем тип рыбалки
        if (initialTrip != null && initialTrip.fishingType.isNotEmpty()) {
            val position = getFishingTypePosition(initialTrip.fishingType)
            binding.dropdownFishingType.setText(initialTrip.fishingType)
            binding.dropdownFishingType.listSelection = position
        }

        // Настраиваем обработчики событий
        setupEventListeners()
    }

    /**
     * Настраивает выпадающий список типов рыбалки
     */
    private fun setupFishingTypeDropdown() {
        // Создаем список типов рыбалки из строковых ресурсов
        val fishingTypes = listOf(
            context.getString(R.string.fishing_type_carp),
            context.getString(R.string.fishing_type_spinning),
            context.getString(R.string.fishing_type_feeder),
            context.getString(R.string.fishing_type_float),
            context.getString(R.string.fishing_type_winter),
            context.getString(R.string.fishing_type_flyfishing),
            context.getString(R.string.fishing_type_trolling),
            context.getString(R.string.fishing_type_other)
        )

        // Создаем адаптер для выпадающего списка
        val adapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, fishingTypes)
        binding.dropdownFishingType.setAdapter(adapter)
    }

    /**
     * Получает позицию типа рыбалки в списке
     */
    private fun getFishingTypePosition(fishingType: String): Int {
        val fishingTypes = listOf(
            context.getString(R.string.fishing_type_carp),
            context.getString(R.string.fishing_type_spinning),
            context.getString(R.string.fishing_type_feeder),
            context.getString(R.string.fishing_type_float),
            context.getString(R.string.fishing_type_winter),
            context.getString(R.string.fishing_type_flyfishing),
            context.getString(R.string.fishing_type_trolling),
            context.getString(R.string.fishing_type_other)
        )

        return fishingTypes.indexOf(fishingType).takeIf { it >= 0 } ?: 0
    }

    /**
     * Настраивает обработчики событий
     */
    private fun setupEventListeners() {
        // Обработчик нажатия на поле начальной даты
        binding.editTextStartDate.setOnClickListener {
            showStartDatePicker()
        }

        // Обработчик нажатия на поле конечной даты
        binding.editTextEndDate.setOnClickListener {
            showEndDatePicker()
        }

        // Обработчик изменения чекбокса многодневной рыбалки
        binding.checkBoxMultiDay.setOnCheckedChangeListener { _, isChecked ->
            isMultiDay = isChecked
            binding.layoutEndDate.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Обработчик нажатия на кнопку сохранения
        binding.buttonSave.setOnClickListener {
            saveTrip()
        }

        // Обработчик нажатия на кнопку отмены
        binding.buttonCancel.setOnClickListener {
            dismiss()
        }
    }

    /**
     * Обновляет отображение дат
     */
    private fun updateDateDisplay() {
        binding.editTextStartDate.setText(dateFormat.format(startCalendar.time))
        binding.editTextEndDate.setText(dateFormat.format(endCalendar.time))
    }

    /**
     * Показывает диалог выбора начальной даты
     */
    private fun showStartDatePicker() {
        DatePickerDialog(
            context,
            { _, year, month, day ->
                startCalendar.set(Calendar.YEAR, year)
                startCalendar.set(Calendar.MONTH, month)
                startCalendar.set(Calendar.DAY_OF_MONTH, day)

                // Если конечная дата раньше начальной, обновляем и ее
                if (isMultiDay && endCalendar.before(startCalendar)) {
                    endCalendar.time = startCalendar.time
                    endCalendar.add(Calendar.DAY_OF_MONTH, 1)
                }

                updateDateDisplay()
            },
            startCalendar.get(Calendar.YEAR),
            startCalendar.get(Calendar.MONTH),
            startCalendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    /**
     * Показывает диалог выбора конечной даты
     */
    private fun showEndDatePicker() {
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val tempCalendar = Calendar.getInstance()
                tempCalendar.set(Calendar.YEAR, year)
                tempCalendar.set(Calendar.MONTH, month)
                tempCalendar.set(Calendar.DAY_OF_MONTH, day)

                // Проверяем, что конечная дата не раньше начальной
                if (tempCalendar.before(startCalendar)) {
                    // Если раньше, устанавливаем на день позже начальной
                    endCalendar.time = startCalendar.time
                    endCalendar.add(Calendar.DAY_OF_MONTH, 1)
                } else {
                    endCalendar.set(Calendar.YEAR, year)
                    endCalendar.set(Calendar.MONTH, month)
                    endCalendar.set(Calendar.DAY_OF_MONTH, day)
                }

                updateDateDisplay()
            },
            endCalendar.get(Calendar.YEAR),
            endCalendar.get(Calendar.MONTH),
            endCalendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    /**
     * Сохраняет запланированную рыбалку
     */
    private fun saveTrip() {
        // Получаем данные из полей
        val location = binding.editTextLocation.text.toString().trim()
        val fishingType = binding.dropdownFishingType.text.toString().trim()
        val note = binding.editTextNote.text.toString().trim()

        // Проверяем обязательные поля
        if (location.isEmpty()) {
            binding.editTextLocation.error = "Укажите место"
            return
        }

        // Создаем объект запланированной рыбалки
        val trip = PlannedTrip(
            id = initialTrip?.id ?: "",
            userId = initialTrip?.userId ?: "",
            date = startCalendar.time,
            endDate = if (isMultiDay) endCalendar.time else null,
            isMultiDay = isMultiDay,
            location = location,
            fishingType = fishingType,
            note = note,
            createdAt = initialTrip?.createdAt ?: Date()
        )

        // Вызываем callback
        onSaved(trip)

        // Закрываем диалог
        dismiss()
    }
}