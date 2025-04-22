package com.example.driftnotes.stats

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import com.example.driftnotes.R
import com.example.driftnotes.databinding.DialogDateFilterBinding
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Диалог для выбора периода для фильтрации статистики
 */
class DateFilterDialog(
    context: Context,
    private val initialStartDate: Date,
    private val initialEndDate: Date,
    private val onFilterApplied: (startDate: Date, endDate: Date, filterType: String) -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogDateFilterBinding

    // Форматтер для отображения дат
    private val simpleDateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("ru"))

    // Календари для хранения выбранных дат
    private val startDateCalendar = Calendar.getInstance()
    private val endDateCalendar = Calendar.getInstance()

    // Тип выбранного фильтра
    private var selectedFilterType = "all_time"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Инициализируем binding
        binding = DialogDateFilterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Настраиваем размер диалога
        window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        // Инициализируем даты
        startDateCalendar.time = initialStartDate
        endDateCalendar.time = initialEndDate

        // Обновляем текст на кнопках выбора дат
        updateDateButtonsText()

        // Настраиваем обработчики событий
        setupListeners()
    }

    /**
     * Настраивает обработчики событий
     */
    private fun setupListeners() {
        // Обработчики для чипов с периодами
        binding.chipWeek.setOnClickListener {
            selectedFilterType = "week"
            binding.layoutCustomDateRange.visibility = View.GONE
            updateSelectedChip(binding.chipWeek)
        }

        binding.chipMonth.setOnClickListener {
            selectedFilterType = "month"
            binding.layoutCustomDateRange.visibility = View.GONE
            updateSelectedChip(binding.chipMonth)
        }

        binding.chipYear.setOnClickListener {
            selectedFilterType = "year"
            binding.layoutCustomDateRange.visibility = View.GONE
            updateSelectedChip(binding.chipYear)
        }

        binding.chipAllTime.setOnClickListener {
            selectedFilterType = "all_time"
            binding.layoutCustomDateRange.visibility = View.GONE
            updateSelectedChip(binding.chipAllTime)
        }

        binding.chipCustom.setOnClickListener {
            selectedFilterType = "custom"
            binding.layoutCustomDateRange.visibility = View.VISIBLE
            updateSelectedChip(binding.chipCustom)
        }

        // Обработчики для кнопок выбора дат
        binding.buttonStartDate.setOnClickListener { showStartDatePicker() }
        binding.buttonEndDate.setOnClickListener { showEndDatePicker() }

        // Обработчики для кнопок применения и отмены
        binding.buttonApply.setOnClickListener {
            applyFilter()
            dismiss()
        }

        binding.buttonCancel.setOnClickListener {
            dismiss()
        }
    }

    /**
     * Обновляет выделение для чипа
     */
    private fun updateSelectedChip(selectedChip: Chip) {
        binding.chipWeek.isChecked = selectedChip == binding.chipWeek
        binding.chipMonth.isChecked = selectedChip == binding.chipMonth
        binding.chipYear.isChecked = selectedChip == binding.chipYear
        binding.chipAllTime.isChecked = selectedChip == binding.chipAllTime
        binding.chipCustom.isChecked = selectedChip == binding.chipCustom
    }

    /**
     * Показывает диалог выбора начальной даты
     */
    private fun showStartDatePicker() {
        val year = startDateCalendar.get(Calendar.YEAR)
        val month = startDateCalendar.get(Calendar.MONTH)
        val day = startDateCalendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            context,
            { _, selectedYear, selectedMonth, selectedDay ->
                // Обновляем начальную дату
                startDateCalendar.set(selectedYear, selectedMonth, selectedDay, 0, 0, 0)

                // Если начальная дата позже конечной, устанавливаем конечную дату равной начальной
                if (startDateCalendar.after(endDateCalendar)) {
                    endDateCalendar.time = startDateCalendar.time
                }

                // Обновляем текст на кнопках
                updateDateButtonsText()
            },
            year, month, day
        )

        // Показываем диалог
        datePickerDialog.show()
    }

    /**
     * Показывает диалог выбора конечной даты
     */
    private fun showEndDatePicker() {
        val year = endDateCalendar.get(Calendar.YEAR)
        val month = endDateCalendar.get(Calendar.MONTH)
        val day = endDateCalendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            context,
            { _, selectedYear, selectedMonth, selectedDay ->
                // Обновляем конечную дату
                endDateCalendar.set(selectedYear, selectedMonth, selectedDay, 23, 59, 59)

                // Если конечная дата раньше начальной, устанавливаем начальную дату равной конечной
                if (endDateCalendar.before(startDateCalendar)) {
                    startDateCalendar.time = endDateCalendar.time
                    startDateCalendar.set(Calendar.HOUR_OF_DAY, 0)
                    startDateCalendar.set(Calendar.MINUTE, 0)
                    startDateCalendar.set(Calendar.SECOND, 0)
                }

                // Обновляем текст на кнопках
                updateDateButtonsText()
            },
            year, month, day
        )

        // Показываем диалог
        datePickerDialog.show()
    }

    /**
     * Обновляет текст на кнопках выбора даты
     */
    private fun updateDateButtonsText() {
        val startDateText = simpleDateFormat.format(startDateCalendar.time)
        val endDateText = simpleDateFormat.format(endDateCalendar.time)

        binding.buttonStartDate.text = "C: $startDateText"
        binding.buttonEndDate.text = "По: $endDateText"
    }

    /**
     * Применяет выбранный фильтр
     */
    private fun applyFilter() {
        // В зависимости от выбранного типа фильтра устанавливаем соответствующие даты
        when (selectedFilterType) {
            "week" -> {
                // Устанавливаем конец недели на текущую дату
                endDateCalendar.time = Date()

                // Устанавливаем начало недели на 7 дней назад
                startDateCalendar.time = Date()
                startDateCalendar.add(Calendar.DAY_OF_YEAR, -7)
            }
            "month" -> {
                // Устанавливаем конец месяца на текущую дату
                endDateCalendar.time = Date()

                // Устанавливаем начало месяца на 30 дней назад
                startDateCalendar.time = Date()
                startDateCalendar.add(Calendar.DAY_OF_YEAR, -30)
            }
            "year" -> {
                // Устанавливаем конец года на текущую дату
                endDateCalendar.time = Date()

                // Устанавливаем начало года на 365 дней назад
                startDateCalendar.time = Date()
                startDateCalendar.add(Calendar.DAY_OF_YEAR, -365)
            }
            "all_time" -> {
                // Устанавливаем начало на 1 января 2000 года
                startDateCalendar.set(2000, 0, 1, 0, 0, 0)

                // Устанавливаем конец на текущую дату
                endDateCalendar.time = Date()
            }
            "custom" -> {
                // Используем уже выбранные пользователем даты
            }
        }

        // Вызываем callback с выбранными датами и типом фильтра
        onFilterApplied(startDateCalendar.time, endDateCalendar.time, selectedFilterType)
    }
}