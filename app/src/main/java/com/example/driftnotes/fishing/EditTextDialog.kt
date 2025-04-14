package com.example.driftnotes.fishing

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import com.example.driftnotes.databinding.DialogEditTextBinding

/**
 * Диалог для редактирования текстового поля
 */
class EditTextDialog(
    context: Context,
    private val title: String,
    private val hint: String,
    private val initialText: String,
    private val onTextSaved: (String) -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogEditTextBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Убираем заголовок диалога
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        // Инициализация привязки
        binding = DialogEditTextBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Устанавливаем ширину диалога
        window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        // Устанавливаем заголовок и текст подсказки
        binding.textViewDialogTitle.text = title
        binding.editText.hint = hint

        // Устанавливаем начальный текст
        binding.editText.setText(initialText)

        // Обработчик кнопки сохранения
        binding.buttonSave.setOnClickListener {
            val newText = binding.editText.text.toString()
            onTextSaved(newText)
            dismiss()
        }

        // Обработчик кнопки отмены
        binding.buttonCancel.setOnClickListener {
            dismiss()
        }
    }
}