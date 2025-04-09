package com.example.driftnotes.utils

import com.example.driftnotes.R

/**
 * Утилитарный класс для валидации паролей
 */
object PasswordValidator {

    /**
     * Проверяет соответствие пароля требованиям безопасности
     *
     * @param password пароль для проверки
     * @return результат валидации
     */
    fun validate(password: String): PasswordValidationResult {
        // Проверка минимальной длины
        if (password.length < 8) {
            return PasswordValidationResult.TooShort
        }

        // Проверка наличия хотя бы одной заглавной буквы
        if (!password.any { it.isUpperCase() }) {
            return PasswordValidationResult.NoUppercase
        }

        // Проверка отсутствия специальных символов
        val specialCharPattern = Regex("[^a-zA-Z0-9]")
        if (specialCharPattern.find(password) != null) {
            return PasswordValidationResult.HasSpecialChars
        }

        // Пароль соответствует всем требованиям
        return PasswordValidationResult.Valid
    }

    /**
     * Получить описание ошибки валидации пароля
     *
     * @param result результат валидации пароля
     * @return строковый ресурс с описанием ошибки
     */
    fun getErrorMessageResId(result: PasswordValidationResult): Int {
        return when (result) {
            PasswordValidationResult.TooShort -> R.string.password_too_short
            PasswordValidationResult.NoUppercase -> R.string.password_no_uppercase
            PasswordValidationResult.HasSpecialChars -> R.string.password_has_special_chars
            PasswordValidationResult.Valid -> 0 // Для действительного пароля нет сообщения об ошибке
        }
    }

    /**
     * Возвращает все требования к паролю в виде списка
     *
     * @return список с требованиями к паролю
     */
    fun getPasswordRequirements(): List<Int> {
        return listOf(
            R.string.password_requirement_length,
            R.string.password_requirement_uppercase,
            R.string.password_requirement_no_special
        )
    }

    /**
     * Перечисление возможных результатов валидации пароля
     */
    enum class PasswordValidationResult {
        Valid,            // Пароль действителен
        TooShort,         // Пароль слишком короткий
        NoUppercase,      // Нет заглавной буквы
        HasSpecialChars   // Содержит специальные символы
    }
}