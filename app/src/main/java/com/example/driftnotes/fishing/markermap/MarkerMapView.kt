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
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import java.util.UUID

/**
 * Типы маркеров для карты дна
 */
enum class MarkerType(val symbol: String, val color: Int, val description: String) {
    SNAG("С", Color.YELLOW, "Коряга"),
    ROCK("К", Color.LTGRAY, "Камень"),
    HOLE("Я", Color.CYAN, "Яма"),
    PLATEAU("П", Color.GREEN, "Плато"),
    SLOPE("С", Color.MAGENTA, "Свал"),
    DROP_OFF("О", Color.RED, "Обрыв"),
    WEED("В", Color.parseColor("#7CFC00"), "Водоросли")
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
}

/**
 * Режимы редактирования карты
 */
enum class EditMode {
    NONE,               // Просмотр без редактирования
    ADD_MARKER,         // Режим добавления маркеров
    MOVE_MARKER,        // Режим перемещения маркеров
    CONNECT_MARKERS,    // Режим соединения маркеров
    DELETE_MARKER       // Режим удаления маркеров
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

    // Матрица трансформации (для масштабирования и перемещения)
    private val matrix = Matrix()

    // Список маркеров
    private val markers = mutableListOf<Marker>()

    // Список соединений между маркерами
    private val connections = mutableListOf<MarkerConnection>()

    // Слушатель событий
    var listener: MarkerMapListener? = null

    // Текущий режим редактирования
    var editMode = EditMode.NONE

    // Текущий выбранный тип маркера
    var currentMarkerType = MarkerType.ROCK

    // Выбранный маркер
    private var selectedMarker: Marker? = null

    // Первый маркер для соединения в режиме CONNECT_MARKERS
    private var firstConnectionMarker: Marker? = null

    // Переменные для управления перемещением и масштабированием
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var scaleFactor = 1.0f
    private val minScale = 0.5f
    private val maxScale = 5.0f

    // Детекторы жестов
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private lateinit var gestureDetector: GestureDetector

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

    private val markerTextPaint = Paint().apply {
        color = Color.BLACK
        textSize = 30f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
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

        // Инициализируем детекторы жестов
        scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
        gestureDetector = GestureDetector(context, GestureListener())
    }

    /**
     * Загружает bitmap карты из ресурсов
     */
    private fun loadMapBitmap() {
        try {
            val drawable = ContextCompat.getDrawable(context, R.drawable.marker_map_template)
            mapBitmap = drawable?.toBitmap()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading map bitmap", e)
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

            scaleFactor = scale


        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Обрабатываем события масштабирования и жестов
        scaleGestureDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)

        // Обработка прочих событий в зависимости от режима
        when (editMode) {
            EditMode.ADD_MARKER -> handleAddMarkerTouch(event)
            EditMode.MOVE_MARKER -> handleMoveMarkerTouch(event)
            EditMode.CONNECT_MARKERS -> handleConnectMarkersTouch(event)
            EditMode.DELETE_MARKER -> handleDeleteMarkerTouch(event)
            EditMode.NONE -> handleViewModeTouch(event)
        }

        return true
    }

    /**
     * Обработка касаний в режиме просмотра
     */
    private fun handleViewModeTouch(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Проверяем, нажал ли пользователь на маркер
                selectedMarker = findMarkerAtPoint(event.x, event.y)
                selectedMarker?.let {
                    listener?.onMarkerSelected(it)
                }
                invalidate()
            }
        }
    }

    /**
     * Обработка касаний в режиме добавления маркеров
     */
    private fun handleAddMarkerTouch(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_UP -> {
                // Преобразуем координаты касания в координаты карты
                val mappedPoint = mapTouchPointToMapCoordinates(event.x, event.y)

                // Получаем информацию о глубине на основе расстояния от центра
                val depth = calculateDepthForPoint(mappedPoint.x, mappedPoint.y)

                // Создаем новый маркер
                val marker = Marker(
                    x = mappedPoint.x,
                    y = mappedPoint.y,
                    type = currentMarkerType,
                    depth = depth
                )

                // Добавляем маркер в список
                markers.add(marker)

                // Уведомляем слушателя
                listener?.onMarkerAdded(marker)

                // Перерисовываем карту
                invalidate()
            }
        }
    }

    /**
     * Обработка касаний в режиме перемещения маркеров
     */
    private fun handleMoveMarkerTouch(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Выбираем маркер для перемещения
                selectedMarker = findMarkerAtPoint(event.x, event.y)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                // Перемещаем выбранный маркер
                selectedMarker?.let {
                    val mappedPoint = mapTouchPointToMapCoordinates(event.x, event.y)
                    it.x = mappedPoint.x
                    it.y = mappedPoint.y

                    // Обновляем глубину
                    it.depth = calculateDepthForPoint(mappedPoint.x, mappedPoint.y)

                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                // Уведомляем слушателя о перемещении маркера
                selectedMarker?.let {
                    listener?.onMarkerMoved(it)
                }
            }
        }
    }

    /**
     * Обработка касаний в режиме соединения маркеров
     */
    private fun handleConnectMarkersTouch(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Ищем маркер под точкой касания
                val marker = findMarkerAtPoint(event.x, event.y)

                if (marker != null) {
                    if (firstConnectionMarker == null) {
                        // Выбираем первый маркер для соединения
                        firstConnectionMarker = marker
                        selectedMarker = marker
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

                        // Перерисовываем карту
                        invalidate()
                    }
                }
            }
        }
    }

    /**
     * Обработка касаний в режиме удаления маркеров
     */
    private fun handleDeleteMarkerTouch(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_UP -> {
                // Ищем маркер под точкой касания
                val marker = findMarkerAtPoint(event.x, event.y)

                // Удаляем маркер, если найден
                if (marker != null) {
                    // Удаляем соединения, содержащие этот маркер
                    connections.removeAll { it.marker1Id == marker.id || it.marker2Id == marker.id }

                    // Удаляем сам маркер
                    markers.remove(marker)

                    // Уведомляем слушателя
                    listener?.onMarkerDeleted(marker)

                    // Перерисовываем карту
                    invalidate()
                }
            }
        }
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
            markerPaint.color = marker.type.color

            // Размер маркера (больше для выбранного)
            val markerSize = if (marker == selectedMarker || marker == firstConnectionMarker) 25f else 20f

            // Рисуем круг маркера
            canvas.drawCircle(marker.x, marker.y, markerSize, markerPaint)
            canvas.drawCircle(marker.x, marker.y, markerSize, markerStrokePaint)

            // Рисуем символ маркера
            canvas.drawText(marker.type.symbol, marker.x, marker.y + markerTextPaint.textSize / 3, markerTextPaint)

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
            if (distance <= 30f) { // Зона касания 30 пикселей
                return marker
            }
        }

        return null
    }

    /**
     * Преобразует координаты касания в координаты карты
     */
    private fun mapTouchPointToMapCoordinates(touchX: Float, touchY: Float): PointF {
        val inverse = Matrix()
        matrix.invert(inverse)

        val mappedPoints = floatArrayOf(touchX, touchY)
        inverse.mapPoints(mappedPoints)

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

            scaleFactor = scale
        }

        invalidate()
    }

    /**
     * Добавляет маркер программно
     */
    fun addMarker(x: Float, y: Float, type: MarkerType, depth: Float = 0f, notes: String = ""): Marker {
        val marker = Marker(
            x = x,
            y = y,
            type = type,
            depth = depth,
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
     * Устанавливает данные карты
     */
    fun setMapData(newMarkers: List<Marker>, newConnections: List<MarkerConnection>) {
        markers.clear()
        markers.addAll(newMarkers)

        connections.clear()
        connections.addAll(newConnections)

        selectedMarker = null
        firstConnectionMarker = null

        invalidate()
    }

    companion object {
        private const val TAG = "MarkerMapView"
    }
}