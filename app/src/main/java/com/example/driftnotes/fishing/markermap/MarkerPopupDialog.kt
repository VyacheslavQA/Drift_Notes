package com.example.driftnotes.fishing.markermap

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.driftnotes.R
import com.example.driftnotes.databinding.DialogMarkerPopupBinding
import com.google.android.material.slider.Slider

/**
 * Диалог для добавления/редактирования маркера при долгом нажатии на карту
 */
class MarkerPopupDialog(
    context: Context,
    private val isEdit: Boolean = false,
    private val marker: Marker? = null,
    private val markerX: Float = 0f,
    private val markerY: Float = 0f,
    private val onMarkerCreated: (Marker) -> Unit,
    private val onMarkerUpdated: (Marker) -> Unit,
    private val onMarkerDeleted: (Marker) -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogMarkerPopupBinding

    // Выбранные значения
    private var selectedType = marker?.type ?: MarkerType.ROCK
    private var selectedColor = marker?.color ?: MarkerColors.RED
    private var selectedDepth = marker?.depth ?: 0f
    private var notes = marker?.notes ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Убираем заголовок диалога
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        binding = DialogMarkerPopupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Увеличиваем размер диалога
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        // Скрываем заголовок диалогового окна
        binding.textViewTitle.visibility = View.GONE

        // Скрываем выбор размера маркера - он теперь всегда стандартный
        binding.textViewSizeTitle.visibility = View.GONE
        binding.spinnerSize.visibility = View.GONE

        // Настраиваем адаптеры для типов маркеров
        setupMarkerTypeGrid()

        // Настраиваем выбор цвета
        setupColorGrid()

        // Устанавливаем начальные значения при редактировании
        if (isEdit && marker != null) {
            // Заполняем поля данными существующего маркера
            binding.editTextNotes.setText(marker.notes)

            // Устанавливаем глубину в поле для ввода
            binding.editTextDepth.setText(String.format("%.1f", marker.depth))

            // Выбираем тип маркера
            updateSelectedMarkerType(marker.type)

            // Показываем кнопку удаления
            binding.buttonDelete.visibility = View.VISIBLE
        } else {
            // Скрываем кнопку удаления для нового маркера
            binding.buttonDelete.visibility = View.GONE

            // Устанавливаем начальное значение глубины
            binding.editTextDepth.setText(String.format("%.1f", selectedDepth))
        }

        // Настраиваем обработчики кнопок
        setupButtons()
    }

    /**
     * Настраивает сетку типов маркеров
     */
    private fun setupMarkerTypeGrid() {
        val markerTypes = MarkerType.values()

        // Создаем адаптер для типов маркеров
        val adapter = MarkerTypeAdapter(markerTypes, selectedType) { type ->
            selectedType = type
            updateSelectedMarkerType(type)
        }

        binding.recyclerViewMarkerTypes.apply {
            layoutManager = GridLayoutManager(context, 4)
            this.adapter = adapter
        }
    }

    /**
     * Обновляет отображение выбранного типа маркера
     */
    private fun updateSelectedMarkerType(type: MarkerType) {
        selectedType = type
        binding.textViewSelectedType.text = "Тип: ${type.description}"

        // Загружаем изображение символа
        val drawable = ContextCompat.getDrawable(context, type.iconResId)
        binding.imageViewSelectedType.setImageDrawable(drawable)
    }

    /**
     * Настраивает сетку выбора цвета
     */
    private fun setupColorGrid() {
        val colors = MarkerColors.allColors

        // Создаем адаптер для цветов
        val adapter = ColorAdapter(colors, selectedColor) { color ->
            selectedColor = color
        }

        binding.recyclerViewColors.apply {
            layoutManager = GridLayoutManager(context, 4)
            this.adapter = adapter
        }
    }

    /**
     * Настраивает обработчики кнопок
     */
    private fun setupButtons() {
        // Кнопка Сохранить
        binding.buttonSave.setOnClickListener {
            notes = binding.editTextNotes.text.toString()

            // Получаем введенную глубину (с проверкой на корректность ввода)
            val depthText = binding.editTextDepth.text.toString()
            selectedDepth = try {
                depthText.toFloat()
            } catch (e: NumberFormatException) {
                0f
            }

            if (isEdit && marker != null) {
                // Обновляем существующий маркер
                val updatedMarker = marker.copy(
                    type = selectedType,
                    depth = selectedDepth,
                    color = selectedColor,
                    size = MarkerSize.LARGE, // Всегда используем стандартный размер
                    notes = notes
                )
                onMarkerUpdated(updatedMarker)
            } else {
                // Создаем новый маркер
                val newMarker = Marker(
                    x = markerX,
                    y = markerY,
                    type = selectedType,
                    depth = selectedDepth,
                    color = selectedColor,
                    size = MarkerSize.LARGE, // Всегда используем стандартный размер
                    notes = notes
                )
                onMarkerCreated(newMarker)
            }

            dismiss()
        }

        // Кнопка Отмена
        binding.buttonCancel.setOnClickListener {
            dismiss()
        }

        // Кнопка Удалить
        binding.buttonDelete.setOnClickListener {
            if (isEdit && marker != null) {
                // Спрашиваем подтверждение удаления
                AlertDialog.Builder(context)
                    .setTitle("Удаление маркера")
                    .setMessage("Вы уверены, что хотите удалить этот маркер?")
                    .setPositiveButton("Да") { _, _ ->
                        onMarkerDeleted(marker)
                        dismiss()
                    }
                    .setNegativeButton("Нет", null)
                    .show()
            }
        }
    }

    /**
     * Адаптер для отображения типов маркеров
     */
    private inner class MarkerTypeAdapter(
        private val types: Array<MarkerType>,
        private var selectedType: MarkerType,
        private val onTypeSelected: (MarkerType) -> Unit
    ) : RecyclerView.Adapter<MarkerTypeAdapter.MarkerTypeViewHolder>() {

        inner class MarkerTypeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val imageView: ImageView = itemView.findViewById(R.id.imageViewMarkerType)
            val textView: TextView = itemView.findViewById(R.id.textViewMarkerType)
            val container: View = itemView.findViewById(R.id.containerMarkerType)

            fun bind(type: MarkerType) {
                // Устанавливаем изображение с уменьшенным размером
                imageView.layoutParams.width = 40 // уменьшаем размер иконки
                imageView.layoutParams.height = 40 // уменьшаем размер иконки
                imageView.setImageResource(type.iconResId)

                // Устанавливаем текст
                textView.textSize = 14f // уменьшаем размер текста
                textView.text = type.description

                // Выделяем выбранный тип
                if (type == selectedType) {
                    container.setBackgroundResource(R.drawable.bg_selected_item)
                } else {
                    container.setBackgroundResource(R.drawable.bg_normal_item)
                }

                // Обработчик нажатия
                itemView.setOnClickListener {
                    val previousSelectedType = selectedType
                    selectedType = type

                    // Обновляем выделение
                    notifyItemChanged(types.indexOf(previousSelectedType))
                    notifyItemChanged(types.indexOf(selectedType))

                    // Вызываем обработчик
                    onTypeSelected(type)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MarkerTypeViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_marker_type, parent, false)
            return MarkerTypeViewHolder(view)
        }

        override fun onBindViewHolder(holder: MarkerTypeViewHolder, position: Int) {
            holder.bind(types[position])
        }

        override fun getItemCount(): Int = types.size
    }

    /**
     * Адаптер для отображения цветов
     */
    private inner class ColorAdapter(
        private val colors: List<Int>,
        private var selectedColor: Int,
        private val onColorSelected: (Int) -> Unit
    ) : RecyclerView.Adapter<ColorAdapter.ColorViewHolder>() {

        inner class ColorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val colorView: View = itemView.findViewById(R.id.viewColor)
            val container: View = itemView.findViewById(R.id.containerColor)

            fun bind(color: Int) {
                // Устанавливаем цвет
                colorView.setBackgroundColor(color)

                // Выделяем выбранный цвет
                if (color == selectedColor) {
                    container.setBackgroundResource(R.drawable.bg_selected_item)
                } else {
                    container.setBackgroundResource(R.drawable.bg_normal_item)
                }

                // Обработчик нажатия
                itemView.setOnClickListener {
                    val previousSelectedColor = selectedColor
                    selectedColor = color

                    // Обновляем выделение
                    notifyItemChanged(colors.indexOf(previousSelectedColor))
                    notifyItemChanged(colors.indexOf(selectedColor))

                    // Вызываем обработчик
                    onColorSelected(color)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_color, parent, false)
            return ColorViewHolder(view)
        }

        override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
            holder.bind(colors[position])
        }

        override fun getItemCount(): Int = colors.size
    }
}