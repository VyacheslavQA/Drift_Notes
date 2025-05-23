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
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.example.driftnotes.R
import kotlin.math.atan2
import kotlin.math.hypot
import java.util.UUID

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

    // Список групп маркеров (для отдельных сегментов лучей)
    private val markerGroups = mutableListOf<MutableList<Marker>>()
    private var currentGroupIndex = -1 // Индекс текущей активной группы

    // Для автоматического соединения маркеров лучами
    private var lastAddedMarker: Marker? = null

    // Флаг, указывающий, следует ли продолжать рисование лучей
    private var continueDrawingRays = true

    // Слушатель событий
    var listener: MarkerMapListener? = null

    // Текущий режим редактирования - всегда VIEW_ONLY, так как кнопки режимов удалены
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
        strokeWidth = 2f // Уменьшаем ширину контура
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val connectionPaint = Paint().apply {
        color = Color.RED
        strokeWidth = 3f // Уменьшаем ширину линии соединения
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    // Новая кисть для автоматических соединений (лучей)
    private val rayConnectionPaint = Paint().apply {
        color = Color.RED  // Изменили цвет на красный
        strokeWidth = 8f   // Увеличили толщину в два раза (было 4f)
        style = Paint.Style.STROKE
        isAntiAlias = true
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(20f, 10f), 0f) // Более редкий пунктир
    }

    private val infoTextPaint = Paint().apply {
        color = Color.BLACK
        textSize = 24f // Немного уменьшаем размер текста для информации
        isAntiAlias = true
    }

    private val infoBgPaint = Paint().apply {
        color = Color.WHITE
        alpha = 200
        style = Paint.Style.FILL
    }

    // Обновленная кисть для отображения названия типа маркера
    private val typeTextPaint = Paint().apply {
        color = Color.BLACK
        textSize = 24f // Уменьшаем размер текста
        isAntiAlias = true
        textAlign =
            Paint.Align.LEFT // Выравнивание по левому краю для отображения справа от маркера
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    // Обновленная кисть для отображения глубины
    private val depthTextPaint = Paint().apply {
        color = Color.BLUE
        textSize = 36f  // Уменьшаем размер текста для глубины
        isAntiAlias = true
        textAlign =
            Paint.Align.RIGHT // Выравнивание по правому краю для отображения слева от маркера
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    init {
        // Загружаем карту из ресурсов
        loadMapBitmap()

        // Загружаем иконки маркеров
        loadMarkerIcons()

        // Инициализируем детекторы жестов
        scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
        gestureDetector = GestureDetector(context, GestureListener())

        // Инициализируем первую группу маркеров
        startNewGroup()
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
                val bitmap = drawable?.toBitmap(width = 48, height = 48) // Уменьшаем размер иконок
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

    // Переменная для отслеживания времени последнего нажатия (для определения двойного нажатия)
    private var lastTapTime: Long = 0

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

                if (!scaleHandled && hasMovedWhileDown) {
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
                        // Если двойное нажатие на маркер, останавливаем рисование лучей
                        if (marker == lastAddedMarker && System.currentTimeMillis() - lastTapTime < 500) {
                            // Останавливаем рисование лучей
                            stopDrawingRays()
                            Toast.makeText(context, "Рисование лучей остановлено", Toast.LENGTH_SHORT).show()
                        } else {
                            // Одиночное нажатие - обычная обработка
                            listener?.onMarkerSelected(marker)
                        }
                        lastTapTime = System.currentTimeMillis()
                    }
                }

                return true
            }
        }

        return scaleHandled || gestureHandled || true
    }

    /**
     * Создает новую группу маркеров
     */
    fun startNewGroup() {
        markerGroups.add(mutableListOf())
        currentGroupIndex = markerGroups.size - 1
        lastAddedMarker = null
    }

    /**
     * Останавливает рисование лучей, сбрасывая lastAddedMarker
     */
    fun stopDrawingRays() {
        lastAddedMarker = null
        continueDrawingRays = false
        // Завершаем текущую группу и создаем новую
        startNewGroup()
        invalidate()
    }

    /**
     * Возобновляет рисование лучей
     */
    fun resumeDrawingRays() {
        continueDrawingRays = true
        startNewGroup() // Создаем новую группу для новых лучей
        // lastAddedMarker установится при добавлении нового маркера
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

        // Рисуем автоматические соединения (лучи) маркеров по группам
        drawRayConnections(canvas)

        // Рисуем ручные соединения между маркерами
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
     * Рисует автоматические соединения (лучи) между маркерами в группах
     */
    private fun drawRayConnections(canvas: Canvas) {
        if (!continueDrawingRays && markerGroups.isEmpty()) return

        // Рисуем лучи для каждой группы маркеров
        for (group in markerGroups) {
            if (group.size < 2) continue

            // Рисуем линии между последовательными маркерами в группе
            for (i in 0 until group.size - 1) {
                val marker1 = group[i]
                val marker2 = group[i + 1]
                canvas.drawLine(marker1.x, marker1.y, marker2.x, marker2.y, rayConnectionPaint)
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

            // Размер маркера - уменьшаем в 2 раза
            val markerSize =
                40f * (if (marker == selectedMarker || marker == firstConnectionMarker) 1.2f else 1.0f)

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

            // Рисуем тип маркера справа от круга
            val typeText = marker.type.description
            val typeOffset = markerSize + 10f  // Отступ от края маркера

            // Фон для текста типа маркера
            val typeBgWidth = typeTextPaint.measureText(typeText) + 20f
            val typeBgHeight = typeTextPaint.textSize + 10f

            canvas.drawRect(
                marker.x + typeOffset,
                marker.y - typeBgHeight / 2,
                marker.x + typeOffset + typeBgWidth,
                marker.y + typeBgHeight / 2,
                infoBgPaint
            )

            // Текст типа маркера (справа от маркера)
            canvas.drawText(
                typeText,
                marker.x + typeOffset + 10f,  // Добавляем отступ внутри фона
                marker.y + typeTextPaint.textSize / 3,
                typeTextPaint
            )

            // Рисуем глубину слева от маркера
            val depthText = String.format("%.1f м", marker.depth)
            val depthOffset = markerSize + 10f  // Отступ от края маркера

            // Фон для текста глубины
            val depthBgWidth = depthTextPaint.measureText(depthText) + 20f
            val depthBgHeight = depthTextPaint.textSize + 10f

            canvas.drawRect(
                marker.x - depthOffset - depthBgWidth,
                marker.y - depthBgHeight / 2,
                marker.x - depthOffset,
                marker.y + depthBgHeight / 2,
                infoBgPaint
            )

            // Текст глубины (слева от маркера)
            canvas.drawText(
                depthText,
                marker.x - depthOffset - 10f,  // Отступ внутри фона
                marker.y + depthTextPaint.textSize / 3,
                depthTextPaint
            )

            // Если маркер выбран, показываем дополнительную информацию о нем
            if (marker == selectedMarker || marker == firstConnectionMarker) {
                // Информационный текст
                val distanceText = String.format(
                    "%.1f м от центра",
                    calculateDistanceFromCenter(marker.x, marker.y)
                )
                val angleText = String.format(
                    "%.1f°",
                    calculateAngleFromCenter(marker.x, marker.y)
                )

                val infoText = "$distanceText\n$angleText"
                val lines = infoText.split("\n")

                // Фон для информации - отображаем над маркером
                val maxTextWidth = lines.maxOf { infoTextPaint.measureText(it) }
                val textHeight = infoTextPaint.textSize
                val padding = 10f

                canvas.drawRect(
                    marker.x - maxTextWidth / 2 - padding,
                    marker.y - markerSize - textHeight * lines.size - padding * 2,
                    marker.x + maxTextWidth / 2 + padding,
                    marker.y - markerSize,
                    infoBgPaint
                )

                // Рисуем каждую строку текста
                for (i in lines.indices) {
                    canvas.drawText(
                        lines[i],
                        marker.x - maxTextWidth / 2 + maxTextWidth / 2,
                        marker.y - markerSize - padding - textHeight * (lines.size - i - 1) + textHeight / 2,
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

            // Уменьшаем зону касания для уменьшенных маркеров
            val touchZone = 40f // Уменьшаем зону касания

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
    fun addMarker(
        x: Float,
        y: Float,
        type: MarkerType,
        depth: Float = 0f,
        color: Int = MarkerColors.RED,
        size: MarkerSize = MarkerSize.LARGE,
        notes: String = ""
    ): Marker {
        val marker = Marker(
            x = x,
            y = y,
            type = type,
            depth = depth,
            color = color,
            size = MarkerSize.LARGE, // Всегда используем стандартный размер
            notes = notes
        )

        // Добавляем маркер в список
        markers.add(marker)

        // Добавляем маркер в текущую группу, если она существует
        if (currentGroupIndex >= 0 && currentGroupIndex < markerGroups.size) {
            markerGroups[currentGroupIndex].add(marker)
        }

        // Если режим рисования лучей активен, устанавливаем текущий маркер как последний добавленный
        if (continueDrawingRays) {
            lastAddedMarker = marker
        }

        invalidate()
        listener?.onMarkerAdded(marker)
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

        // Удаляем маркер из всех групп
        for (group in markerGroups) {
            group.remove(marker)
        }

        if (selectedMarker == marker) {
            selectedMarker = null
        }

        if (firstConnectionMarker == marker) {
            firstConnectionMarker = null
        }

        // Если это был последний добавленный маркер, сбрасываем его
        if (lastAddedMarker == marker) {
            lastAddedMarker = null
        }

        invalidate()
        listener?.onMarkerDeleted(marker)
    }

    /**
     * Очищает все маркеры и соединения
     */
    fun clearAllMarkers() {
        markers.clear()
        connections.clear()
        markerGroups.clear()
        selectedMarker = null
        firstConnectionMarker = null
        lastAddedMarker = null
        continueDrawingRays = true // Сбрасываем флаг при очистке
        startNewGroup() // Создаем новую пустую группу
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
        lastAddedMarker = null

        // Очищаем имеющиеся группы
        markerGroups.clear()

        // Добавляем все маркеры в одну группу (исторические данные)
        if (markers.isNotEmpty()) {
            val initialGroup = mutableListOf<Marker>()
            initialGroup.addAll(markers)
            markerGroups.add(initialGroup)
        }

        // Создаем новую пустую группу для новых маркеров
        startNewGroup()

        continueDrawingRays = true

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
}