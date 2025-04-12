package com.example.driftnotes.fishing.markermap

/**
 * Расширение класса MarkerMapView для добавления необходимых методов,
 * которые используются в MarkerMapActivity
 */
interface MarkerMapExtensions {
    /**
     * Получает список всех маркеров на карте
     */
    fun getMarkers(): List<Marker>

    /**
     * Получает список всех соединений между маркерами
     */
    fun getConnections(): List<MarkerConnection>

    /**
     * Сбрасывает вид карты к исходному состоянию
     */
    fun resetView()

    /**
     * Очищает все маркеры с карты
     */
    fun clearAllMarkers()

    /**
     * Устанавливает данные на карту
     * @param markers список маркеров
     * @param connections список соединений между маркерами
     */
    fun setMapData(markers: List<Marker>, connections: List<MarkerConnection>)
}

/**
 * Добавьте эти методы в класс MarkerMapView:
 */

/*
class MarkerMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), MarkerMapExtensions {

    // Остальной код класса без изменений

    // Добавить эти методы:

    override fun getMarkers(): List<Marker> {
        return markers.toList()
    }

    override fun getConnections(): List<MarkerConnection> {
        return connections.toList()
    }

    override fun resetView() {
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

    override fun clearAllMarkers() {
        markers.clear()
        connections.clear()
        selectedMarker = null
        firstConnectionMarker = null
        invalidate()
    }

    override fun setMapData(markers: List<Marker>, connections: List<MarkerConnection>) {
        this.markers.clear()
        this.markers.addAll(markers)

        this.connections.clear()
        this.connections.addAll(connections)

        selectedMarker = null
        firstConnectionMarker = null

        invalidate()
    }
}
*/