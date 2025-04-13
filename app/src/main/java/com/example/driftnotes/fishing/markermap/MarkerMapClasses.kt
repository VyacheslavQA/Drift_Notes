package com.example.driftnotes.fishing.markermap

import android.graphics.Color
import java.util.UUID

/**
 * Типы маркеров для карты дна
 * Используем изображения символов вместо текста
 */
enum class MarkerType(val iconResId: Int, val description: String) {
    ROCK(com.example.driftnotes.R.drawable.ic_marker_rock, "Камень"),
    SNAG(com.example.driftnotes.R.drawable.ic_marker_snag, "Коряга"),
    HOLE(com.example.driftnotes.R.drawable.ic_marker_hole, "Яма"),
    PLATEAU(com.example.driftnotes.R.drawable.ic_marker_plateau, "Плато"),
    SLOPE(com.example.driftnotes.R.drawable.ic_marker_slope, "Свал"),
    DROP_OFF(com.example.driftnotes.R.drawable.ic_marker_drop_off, "Обрыв"),
    WEED(com.example.driftnotes.R.drawable.ic_marker_weed, "Водоросли"),
    SILT(com.example.driftnotes.R.drawable.ic_marker_silt, "Ил"),
    DEEP_SILT(com.example.driftnotes.R.drawable.ic_marker_deep_silt, "Глубокий ил"),
    SHELL(com.example.driftnotes.R.drawable.ic_marker_shell, "Ракушка"),
    HILL(com.example.driftnotes.R.drawable.ic_marker_hill, "Бугор"),
    FEEDING_SPOT(com.example.driftnotes.R.drawable.ic_marker_feeding_spot, "Точка кормления")
}

/**
 * Размеры маркеров - теперь у нас только один размер
 * Оставляем класс для совместимости
 */
enum class MarkerSize(val factor: Float, val description: String) {
    LARGE(4.0f, "Стандартный") // Увеличиваем в 2 раза от предыдущего LARGE (2.0f -> 4.0f)
}

/**
 * Цвета маркеров
 */
object MarkerColors {
    val RED = Color.RED
    val GREEN = Color.GREEN
    val BLUE = Color.BLUE
    val YELLOW = Color.YELLOW
    val CYAN = Color.CYAN
    val MAGENTA = Color.MAGENTA
    val WHITE = Color.WHITE
    val ORANGE = Color.rgb(255, 165, 0) // Добавляем оранжевый цвет вместо черного

    val allColors = listOf(RED, GREEN, BLUE, YELLOW, CYAN, MAGENTA, WHITE, ORANGE)

    fun getColorName(color: Int): String {
        return when (color) {
            RED -> "Красный"
            GREEN -> "Зеленый"
            BLUE -> "Синий"
            YELLOW -> "Желтый"
            CYAN -> "Голубой"
            MAGENTA -> "Фиолетовый"
            WHITE -> "Белый"
            ORANGE -> "Оранжевый"
            else -> "Неизвестный"
        }
    }
}

/**
 * Класс для хранения информации о маркере
 */
data class Marker(
    val id: String = UUID.randomUUID().toString(),
    var x: Float,
    var y: Float,
    var type: MarkerType,
    var depth: Float,
    var color: Int = MarkerColors.RED,
    var size: MarkerSize = MarkerSize.LARGE, // Используем только один размер
    var notes: String = ""
)

/**
 * Класс для соединений между маркерами
 */
data class MarkerConnection(
    val id: String = UUID.randomUUID().toString(),
    val marker1Id: String,
    val marker2Id: String,
    var notes: String = ""
)

/**
 * Интерфейс слушателя событий карты
 */
interface MarkerMapListener {
    fun onMarkerAdded(marker: Marker)
    fun onMarkerSelected(marker: Marker)
    fun onMarkerMoved(marker: Marker)
    fun onMarkerDeleted(marker: Marker)
    fun onConnectionCreated(connection: MarkerConnection)
    fun onLongPress(x: Float, y: Float)
    fun onMarkerLongPress(marker: Marker, x: Float, y: Float)
}

/**
 * Режимы редактирования карты
 */
enum class EditMode {
    VIEW_ONLY,       // Только просмотр
    MOVE_MARKER,     // Режим перемещения маркеров
    CONNECT_MARKERS  // Режим соединения маркеров
}