package com.example.driftnotes.auth

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import com.example.driftnotes.R

/**
 * Пользовательский компонент EditText для отображения кода верификации
 * с разделёнными блоками для каждой цифры
 */
class VerificationCodeEditText : AppCompatEditText {
    private var onCodeCompleteListener: ((String) -> Unit)? = null

    private var spacing = DEFAULT_SPACING
    private var charSize = DEFAULT_CHAR_SIZE
    private var codeLength = DEFAULT_CODE_LENGTH
    private var lineColor = ContextCompat.getColor(context, R.color.purple_500)
    private var lineSelectedColor = ContextCompat.getColor(context, R.color.purple_700)

    private val textRect = Rect()
    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    constructor(context: Context) : super(context) {
        init(null)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        // Настройка атрибутов если необходимо

        // Настраиваем внешний вид
        background = null
        isCursorVisible = false
        maxLines = 1

        // Ограничиваем режим выделения и копирования
        customSelectionActionModeCallback = object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean = false
            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = false
            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean = false
            override fun onDestroyActionMode(mode: ActionMode?) {}
        }

        // Настраиваем формат ввода
        filters = arrayOf(android.text.InputFilter.LengthFilter(codeLength))

        // Добавляем слушатель изменений текста
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (s?.length == codeLength) {
                    onCodeCompleteListener?.invoke(s.toString())
                }
            }
        })
    }

    fun setOnCodeCompleteListener(listener: (String) -> Unit) {
        onCodeCompleteListener = listener
    }

    override fun onDraw(canvas: Canvas) {
        // Получаем введенный текст
        val text = text?.toString() ?: ""

        val availableWidth = width - paddingLeft - paddingRight
        val cellWidth = (availableWidth - spacing * (codeLength - 1)) / codeLength

        for (i in 0 until codeLength) {
            val left = paddingLeft + i * (cellWidth + spacing)
            val right = left + cellWidth
            val top = height / 2 - charSize / 2
            val bottom = top + charSize

            // Определяем цвет линии в зависимости от фокуса и позиции
            paint.color = if (i == text.length && hasFocus()) lineSelectedColor else lineColor

            // Рисуем границу ячейки
            canvas.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), paint)

            // Если есть символ для этой ячейки, отображаем его
            if (i < text.length) {
                val character = text[i].toString()
                paint.getTextBounds(character, 0, 1, textRect)

                // Центрируем текст в ячейке
                val textWidth = paint.measureText(character)
                val textHeight = textRect.height()

                // Вот здесь проблема - нужно преобразовать Int в Float
                canvas.drawText(
                    character,
                    (left + (cellWidth - textWidth) / 2).toFloat(),  // Преобразовать Int в Float
                    (top + (charSize + textHeight) / 2).toFloat(),   // Преобразовать Int в Float
                    Paint().apply {
                        color = currentTextColor
                        textSize = this@VerificationCodeEditText.textSize
                    }
                )
            }
        }
    }

    companion object {
        private const val DEFAULT_SPACING = 24
        private const val DEFAULT_CHAR_SIZE = 48
        private const val DEFAULT_CODE_LENGTH = 6
    }
}