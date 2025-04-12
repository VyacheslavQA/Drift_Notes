package com.example.driftnotes.fishing.markermap

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.example.driftnotes.R
import kotlin.math.atan2
import kotlin.math.hypot
import java.util.UUID

/**
 * Типы маркеров для карты дна
 * Используем изображения символов вместо текста
 */
enum class MarkerType(val iconResId: Int, val description: String) {
    ROCK(R.drawable.ic_marker_rock, "Камень"),
    SNAG(R.drawable.ic_marker_snag, "Коряга"),
    HOLE(R.drawable.ic_marker_hole, "Яма"),
    PLATEAU(R.drawable.ic_marker_plateau, "Плато"),
    SLOPE(R.drawable.ic_marker_slope, "Свал"),
    DROP_OFF(R.drawable.ic_marker_drop_off, "Обрыв"),
    WEED(R.drawable.ic_marker_weed, "Водоросли"),
    SILT(R.drawable.ic_marker_silt, "Ил"),
    DEEP_SILT(R.drawable.ic_marker_deep_silt, "Глубокий ил"),
    SHELL(R.drawable.ic_marker_shell, "Ракушка"),
    HILL(R.drawable.ic_marker_hill, "Бугор"),
    FEEDING_SPOT(R.drawable.ic_marker_feeding_spot, "Точка кормления")
}

/**
 * Размеры маркеров
 */
enum class MarkerSize(val factor: Float, val description: String) {
    SMALL(1.0f, "Маленький"),
    MEDIUM(1.5f, "Средний"),
    LARGE(2.0f, "Большой")
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
    val BLACK = Color.BLACK

    val allColors = listOf(RED, GREEN, BLUE, YELLOW, CYAN, MAGENTA, WHITE, BLACK)

    fun getColorName(color: Int): String {
        return when (color) {
            RED -> "Красный"
            GREEN -> "Зеленый"
            BLUE -> "Синий"
            YELLOW -> "Желтый"
            CYAN -> "Голубой"
            MAGENTA -> "Фиолетовый"
            WHITE -> "Белый"
            BLACK -> "Черный"
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
    var size: MarkerSize = MarkerSize.SMALL,
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

/**
 * Кастомный View для маркерной карты дна
 */
class MarkerMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Базовая карта
    private var mapBitmap: Bitmap? = null

    // Иконки маркеров
    private val markerIcons = mutableMapOf<MarkerType, Bitmap>()

    // Матрица трансформации (для масштабирования и перемещения)
    private val matrix = Matrix()
    private val inverseMatrix = Matrix()

    // Список маркеров
    private val markers = mutableListOf<Marker>()

    // Список соединений между маркерами
    private val connections = mutableListOf<MarkerConnection>()

    // Слушатель событий
    var listener: MarkerMapListener? = null

    // Текущий режим редактирования
    var editMode = EditMode.VIEW_ONLY

    // Выбранный маркер
    private var selectedMarker: Marker? = null

    // Первый маркер для соединения в режиме CONNECT_MARKERS
    var firstConnectionMarker: Marker? = null

    // Переменные для управления перемещением и масштабированием
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var scaleFactor = 1.0f
    private val minScale = 0.5f
    private val maxScale = 5.0f

    // Переменная для определения перемещения
    private var isDragging = false
    private var hasMovedWhileDown = false
    private var touchStartX = 0f
    private var touchStartY = 0f

    // Долгое нажатие
    private var isLongPressActive = false

    // Детекторы жестов
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private lateinit var gestureDetector: GestureDetector

    // Обработчик длительного нажатия
    private val longPressRunnable = Runnable {
        isLongPressActive = true
        val mappedPoint = mapTouchPointToMapCoordinates(touchStartX, touchStartY)

        // Проверяем, не попал ли пользователь на существующий маркер
        val marker = findMarkerAtPoint(touchStartX, touchStartY)
        if (marker != null) {
            // Если маркер существует, вызываем обработчик длительного нажатия на маркере
            listener?.onMarkerLongPress(marker, touchStartX, touchStartY)
        } else {
            // Иначе вызываем обработчик длительного нажатия на карте
            listener?.onLongPress(mappedPoint.x, mappedPoint.y)
        }
    }

    // Краски для рисования
    private val markerPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val markerStrokePaint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 2f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val connectionPaint = Paint().apply {
        color = Color.RED
        strokeWidth = 3f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val infoTextPaint = Paint().apply {
        color = Color.BLACK
        textSize = 24f
        isAntiAlias = true
    }

    private val infoBgPaint = Paint().apply {
        color = Color.WHITE
        alpha = 200
        style = Paint.Style.FILL
    }

    init {
        // Загружаем карту из ресурсов
        loadMapBitmap()

        // Загружаем иконки маркеров
        loadMarkerIcons()

        // Инициализируем детекторы жестов
        scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
        gestureDetector = GestureDetector(context, GestureListener())
    }

    /**
     * Загружает bitmap карты из ресурсов
     */
    private fun loadMapBitmap() {
        try {
            val drawable = ContextCompat.getDrawable(context, R.drawable.marker_map_vector)
            mapBitmap = drawable?.toBitmap()

            // Если маркерная карта не найдена, создаем пустую карту
            if (mapBitmap == null) {
                mapBitmap = Bitmap.createBitmap(1000, 1000, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(mapBitmap!!)
                canvas.drawColor(Color.WHITE)

                // Рисуем сетку
                val gridPaint = Paint().apply {
                    color = Color.LTGRAY
                    strokeWidth = 1f
                    style = Paint.Style.STROKE
                }

                for (i in 0..10) {
                    val position = i * 100f
                    canvas.drawLine(position, 0f, position, 1000f, gridPaint)
                    canvas.drawLine(0f, position, 1000f, position, gridPaint)
                }

                // Рисуем центральную точку
                canvas.drawCircle(500f, 500f, 10f, Paint().apply {
                    color = Color.RED
                    style = Paint.Style.FILL
                })
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading map bitmap", e)

            // Создаем пустую карту
            mapBitmap = Bitmap.createBitmap(1000, 1000, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(mapBitmap!!)
            canvas.drawColor(Color.WHITE)
        }
    }

    /**
     * Загружает иконки для типов маркеров
     */
    private fun loadMarkerIcons() {
        MarkerType.values().forEach { type ->
            try {
                val drawable = ContextCompat.getDrawable(context, type.iconResId)
                val bitmap = drawable?.toBitmap(width = 48, height = 48)
                if (bitmap != null) {
                    markerIcons[type] = bitmap
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка загрузки иконки для ${type.name}", e)
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // Инициализируем матрицу трансформации при изменении размера view
        if (mapBitmap != null) {
            val bitmap = mapBitmap!!

            // Рассчитываем масштаб для подгонки карты к размерам view
            val scaleX = w.toFloat() / bitmap.width
            val scaleY = h.toFloat() / bitmap.height
            val scale = minOf(scaleX, scaleY)

            // Центрируем карту
            val dx = (w - bitmap.width * scale) / 2f
            val dy = (h - bitmap.height * scale) / 2f

            matrix.reset()
            matrix.postScale(scale, scale)
            matrix.postTranslate(dx, dy)

            // Обновляем инверсную матрицу
            matrix.invert(inverseMatrix)

            scaleFactor = scale
        }

        // Перезагружаем иконки маркеров, если нужно
        if (markerIcons.isEmpty()) {
            loadMarkerIcons()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Проверяем, не обрабатывает ли масштабирование
        val scaleHandled = scaleGestureDetector.onTouchEvent(event)

        // Проверяем, обрабатывает ли жесты (тапы, свайпы)
        val gestureHandled = gestureDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Запоминаем начальные координаты касания
                touchStartX = event.x
                touchStartY = event.y
                lastTouchX = event.x
                lastTouchY = event.y
                hasMovedWhileDown = false
                isLongPressActive = false

                // Ищем маркер под пальцем
                val marker = findMarkerAtPoint(event.x, event.y)
                if (marker != null) {
                    selectedMarker = marker
                    invalidate()

                    if (editMode == EditMode.CONNECT_MARKERS) {
                        if (firstConnectionMarker == null) {
                            firstConnectionMarker = marker
                        } else if (firstConnectionMarker != marker) {
                            // Создаем соединение между маркерами
                            val connection = MarkerConnection(
                                marker1Id = firstConnectionMarker!!.id,
                                marker2Id = marker.id
                            )

                            // Добавляем соединение
                            connections.add(connection)

                            // Уведомляем слушателя
                            listener?.onConnectionCreated(connection)

                            // Сбрасываем выбранные маркеры
                            firstConnectionMarker = null
                            selectedMarker = null

                            invalidate()
                        }
                    }
                }

                // Устанавливаем планировщик длительного нажатия (700 мс)
                postDelayed(longPressRunnable, 700)

                return true
            }

            MotionEvent.ACTION_MOVE -> {
                // Проверяем, было ли движение значительным
                val dx = event.x - touchStartX
                val dy = event.y - touchStartY
                val moveDistance = hypot(dx, dy)

                // Если значительное движение, отменяем длительное нажатие
                if (moveDistance > 10) {
                    hasMovedWhileDown = true
                    removeCallbacks(longPressRunnable)
                }

                // Если в режиме перемещения маркера и маркер выбран
                if (editMode == EditMode.MOVE_MARKER && selectedMarker != null && !isLongPressActive) {
                    // Перемещаем выбранный маркер
                    val mappedPoint = mapTouchPointToMapCoordinates(event.x, event.y)
                    selectedMarker?.let {
                        it.x = mappedPoint.x
                        it.y = mappedPoint.y

                        // Обновляем глубину на основе расстояния от центра
                        it.depth = calculateDepthForPoint(mappedPoint.x, mappedPoint.y)

                        invalidate()
                        listener?.onMarkerMoved(it)
                    }
                } else if (!scaleHandled && hasMovedWhileDown) {
                    // Перемещение карты
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY

                    matrix.postTranslate(dx, dy)
                    matrix.invert(inverseMatrix)
                    invalidate()
                }

                lastTouchX = event.x
                lastTouchY = event.y

                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // Убираем планировщик длительного нажатия
                removeCallbacks(longPressRunnable)
                isLongPressActive = false

                // Проверяем, был ли простой клик (без перемещения)
                if (!hasMovedWhileDown) {
                    // Обработка нажатия на маркер
                    val marker = findMarkerAtPoint(event.x, event.y)
                    if (marker != null) {
                        listener?.onMarkerSelected(marker)
                    }
                }

                return true
            }
        }

        return scaleHandled || gestureHandled || true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Рисуем фон (белый)
        canvas.drawColor(Color.WHITE)

        // Применяем матрицу трансформации
        canvas.save()
        canvas.concat(matrix)

        // Рисуем базовую карту
        mapBitmap?.let {
            canvas.drawBitmap(it, 0f, 0f, null)
        }

        // Рисуем соединения между маркерами
        drawConnections(canvas)

        // Рисуем маркеры
        drawMarkers(canvas)

        // Восстанавливаем canvas
        canvas.restore()
    }

    /**
     * Рисует соединения между маркерами
     */
    private fun drawConnections(canvas: Canvas) {
        for (connection in connections) {
            // Находим маркеры соединения
            val marker1 = markers.find { it.id == connection.marker1Id }
            val marker2 = markers.find { it.id == connection.marker2Id }

            if (marker1 != null && marker2 != null) {
                // Рисуем линию между маркерами
                canvas.drawLine(marker1.x, marker1.y, marker2.x, marker2.y, connectionPaint)

                // Рисуем расстояние между маркерами
                val midX = (marker1.x + marker2.x) / 2
                val midY = (marker1.y + marker2.y) / 2

                val distanceMeters = calculateDistanceBetweenPoints(
                    marker1.x, marker1.y, marker2.x, marker2.y
                )

                val distanceText = String.format("%.1f м", distanceMeters)

                // Фон для текста
                val textWidth = infoTextPaint.measureText(distanceText)
                val textHeight = infoTextPaint.textSize
                canvas.drawRect(
                    midX - textWidth / 2 - 5,
                    midY - textHeight / 2 - 5,
                    midX + textWidth / 2 + 5,
                    midY + textHeight / 2 + 5,
                    infoBgPaint
                )

                // Текст расстояния
                canvas.drawText(distanceText, midX, midY + textHeight / 3, infoTextPaint)
            }
        }
    }

    /**
     * Рисует маркеры на карте
     */
    private fun drawMarkers(canvas: Canvas) {
        for (marker in markers) {
            // Определяем цвет маркера
            markerPaint.color = marker.color

            // Размер маркера
            val markerSize = when (marker.size) {
                MarkerSize.SMALL -> 20f
                MarkerSize.MEDIUM -> 30f
                MarkerSize.LARGE -> 40f
            } * (if (marker == selectedMarker || marker == firstConnectionMarker) 1.2f else 1.0f)

            // Рисуем круг маркера
            canvas.drawCircle(marker.x, marker.y, markerSize, markerPaint)
            canvas.drawCircle(marker.x, marker.y, markerSize, markerStrokePaint)

            // Рисуем иконку маркера
            val iconBitmap = markerIcons[marker.type]
            if (iconBitmap != null) {
                val iconWidth = markerSize * 1.5f
                val iconHeight = markerSize * 1.5f
                val rectF = RectF(
                    marker.x - iconWidth / 2,
                    marker.y - iconHeight / 2,
                    marker.x + iconWidth / 2,
                    marker.y + iconHeight / 2
                )
                canvas.drawBitmap(iconBitmap, null, rectF, null)
            }

            // Если маркер выбран, показываем информацию о нем
            if (marker == selectedMarker || marker == firstConnectionMarker) {
                // Информационный текст
                val depthText = String.format("%.1f м", marker.depth)
                val typeText = marker.type.description
                val distanceText = String.format("%.1f м от центра",
                    calculateDistanceFromCenter(marker.x, marker.y))
                val angleText = String.format("%.1f°",
                    calculateAngleFromCenter(marker.x, marker.y))

                val infoText = "$typeText\n$depthText\n$distanceText\n$angleText"
                val lines = infoText.split("\n")

                // Фон для информации
                val maxTextWidth = lines.maxOf { infoTextPaint.measureText(it) }
                val textHeight = infoTextPaint.textSize
                val padding = 10f

                canvas.drawRect(
                    marker.x + markerSize + 5,
                    marker.y - textHeight * lines.size / 2 - padding,
                    marker.x + markerSize + 5 + maxTextWidth + padding * 2,
                    marker.y + textHeight * lines.size / 2 + padding,
                    infoBgPaint
                )

                // Рисуем каждую строку текста
                for (i in lines.indices) {
                    canvas.drawText(
                        lines[i],
                        marker.x + markerSize + 5 + padding,
                        marker.y - textHeight * lines.size / 2 + textHeight * (i + 0.8f),
                        infoTextPaint
                    )
                }
            }
        }
    }

    /**
     * Находит маркер в указанной точке экрана
     */
    private fun findMarkerAtPoint(x: Float, y: Float): Marker? {
        // Преобразуем координаты экрана в координаты карты
        val mappedPoint = mapTouchPointToMapCoordinates(x, y)

        // Проверяем каждый маркер
        for (marker in markers) {
            val distance = hypot(marker.x - mappedPoint.x, marker.y - mappedPoint.y)

            // Размер зоны касания зависит от размера маркера
            val touchZone = when (marker.size) {
                MarkerSize.SMALL -> 25f
                MarkerSize.MEDIUM -> 35f
                MarkerSize.LARGE -> 45f
            }

            if (distance <= touchZone) {
                return marker
            }
        }

        return null
    }

    /**
     * Преобразует координаты касания в координаты карты
     */
    private fun mapTouchPointToMapCoordinates(touchX: Float, touchY: Float): PointF {
        val mappedPoints = floatArrayOf(touchX, touchY)
        inverseMatrix.mapPoints(mappedPoints)
        return PointF(mappedPoints[0], mappedPoints[1])
    }

    /**
     * Рассчитывает глубину для точки на карте на основе расстояния от центра
     */
    private fun calculateDepthForPoint(x: Float, y: Float): Float {
        // Предполагаем, что центр карты - это координаты (500, 500)
        val centerX = 500f
        val centerY = 500f

        // Рассчитываем расстояние от центра
        val distance = hypot(x - centerX, y - centerY)

        // Преобразуем расстояние в глубину
        // Предполагаем, что каждые 20 единиц расстояния = 10 единиц глубины
        return (distance / 20f) * 10f
    }

    /**
     * Рассчитывает расстояние от центра карты
     */
    private fun calculateDistanceFromCenter(x: Float, y: Float): Float {
        val centerX = 500f
        val centerY = 500f
        val distance = hypot(x - centerX, y - centerY)

        // Преобразуем расстояние в метры (каждые 20 единиц = 10 метров)
        return (distance / 20f) * 10f
    }

    /**
     * Рассчитывает угол от центра карты (в градусах, 0° сверху, по часовой стрелке)
     */
    private fun calculateAngleFromCenter(x: Float, y: Float): Float {
        val centerX = 500f
        val centerY = 500f

        val dx = x - centerX
        val dy = y - centerY

        // Рассчитываем угол в радианах, затем преобразуем в градусы
        var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()

        // Приводим к диапазону 0-360°, причем 0° - вверх
        angle = (angle + 90) % 360
        if (angle < 0) angle += 360

        return angle
    }

    /**
     * Рассчитывает расстояние между двумя точками в метрах
     */
    private fun calculateDistanceBetweenPoints(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val distance = hypot(x2 - x1, y2 - y1)

        // Преобразуем расстояние в метры (каждые 20 единиц = 10 метров)
        return (distance / 20f) * 10f
    }

    /**
     * Внутренний класс для обработки жестов масштабирования
     */
    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            // Изменяем масштаб с учетом фокуса жеста
            scaleFactor *= detector.scaleFactor
            scaleFactor = scaleFactor.coerceIn(minScale, maxScale)

            // Применяем масштабирование с фокусом в точке жеста
            matrix.postScale(
                detector.scaleFactor, detector.scaleFactor,
                detector.focusX, detector.focusY
            )

            // Обновляем инверсную матрицу
            matrix.invert(inverseMatrix)

            invalidate()
            return true
        }
    }

    /**
     * Внутренний класс для обработки жестов перемещения
     */
    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            // Перемещаем карту
            matrix.postTranslate(-distanceX, -distanceY)
            matrix.invert(inverseMatrix)
            invalidate()
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            // Двойное нажатие - сброс масштаба и положения
            resetView()
            return true
        }
    }

    /**
     * Сбрасывает вид карты к исходному положению и масштабу
     */
    fun resetView() {
        // Сбрасываем матрицу трансформации
        matrix.reset()

        // Центрируем карту
        val width = width.toFloat()
        val height = height.toFloat()

        mapBitmap?.let {
            val scaleX = width / it.width
            val scaleY = height / it.height
            val scale = minOf(scaleX, scaleY)

            val dx = (width - it.width * scale) / 2f
            val dy = (height - it.height * scale) / 2f

            matrix.postScale(scale, scale)
            matrix.postTranslate(dx, dy)
            matrix.invert(inverseMatrix)

            scaleFactor = scale
        }

        invalidate()
    }

    /**
     * Добавляет маркер программно
     */
    fun addMarker(x: Float, y: Float, type: MarkerType, depth: Float = 0f, color: Int = MarkerColors.RED, size: MarkerSize = MarkerSize.SMALL, notes: String = ""): Marker {
        val marker = Marker(
            x = x,
            y = y,
            type = type,
            depth = depth,
            color = color,
            size = size,
            notes = notes
        )

        markers.add(marker)
        invalidate()
        return marker
    }

    /**
     * Удаляет маркер
     */
    fun removeMarker(marker: Marker) {
        // Удаляем соединения, содержащие этот маркер
        connections.removeAll { it.marker1Id == marker.id || it.marker2Id == marker.id }

        // Удаляем сам маркер
        markers.remove(marker)

        if (selectedMarker == marker) {
            selectedMarker = null
        }

        if (firstConnectionMarker == marker) {
            firstConnectionMarker = null
        }

        invalidate()
    }

    /**
     * Очищает все маркеры и соединения
     */
    fun clearAllMarkers() {
        markers.clear()
        connections.clear()
        selectedMarker = null
        firstConnectionMarker = null
        invalidate()
    }

    /**
     * Устанавливает данные на карту
     */
    fun setMapData(markers: List<Marker>, connections: List<MarkerConnection>) {
        this.markers.clear()
        this.markers.addAll(markers)

        this.connections.clear()
        this.connections.addAll(connections)

        selectedMarker = null
        firstConnectionMarker = null

        invalidate()
    }

    /**
     * Получает список всех маркеров на карте
     */
    fun getMarkers(): List<Marker> {
        return markers.toList()
    }

    /**
     * Получает список всех соединений между маркерами
     */
    fun getConnections(): List<MarkerConnection> {
        return connections.toList()
    }

    companion object {
        private const val TAG = "MarkerMapView"
    }