package com.example.driftnotes.fishing.markermap

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.driftnotes.R
import com.example.driftnotes.databinding.ActivityMarkerMapBinding
import com.example.driftnotes.utils.FirebaseManager
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

/**
 * Активность для работы с маркерной картой дна
 */
class MarkerMapActivity : AppCompatActivity(), MarkerMapListener {

    private lateinit var binding: ActivityMarkerMapBinding

    // ID и имя карты
    private var mapId: String = ""
    private var mapName: String = ""

    // Firestore для сохранения данных
    private val firestore: FirebaseFirestore by lazy { FirebaseManager.firestore }

    // Флаг для отслеживания изменений
    private var hasChanges = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMarkerMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Настраиваем ActionBar
        supportActionBar?.apply {
            title = getString(R.string.marker_map_title)
            setDisplayHomeAsUpEnabled(true)
        }

        // Получаем ID и имя карты из Intent или создаем новые
        mapId = intent.getStringExtra(EXTRA_MAP_ID) ?: UUID.randomUUID().toString()
        mapName = intent.getStringExtra(EXTRA_MAP_NAME) ?: getString(R.string.marker_map_title)
        supportActionBar?.title = mapName

        // Устанавливаем обработчик событий для карты
        binding.markerMapView.listener = this

        // Настраиваем обработчик нажатия кнопки "Назад"
        setupBackPressedCallback()

        // Настраиваем кнопки режимов редактирования
        setupEditButtons()

        // Если передан ID карты, загружаем её данные
        if (intent.hasExtra(EXTRA_MAP_ID)) {
            loadMapData()
        }

        // Настраиваем кнопки сохранения и настроек
        setupActionButtons()

        // Настраиваем кнопку подтверждения
        binding.buttonConfirmMap.setOnClickListener {
            saveAndConfirm()
        }
    }

    /**
     * Сохраняет карту и возвращает результат в вызвавшую активность
     */
    private fun saveAndConfirm() {
        // Показываем индикатор загрузки
        showLoading(true)

        // Сохраняем данные карты
        saveMapData()

        // Результат будет возвращен в методе saveConnections() после успешного сохранения
        Toast.makeText(this, R.string.map_saved, Toast.LENGTH_SHORT).show()
    }

    /**
     * Настраивает обработчик нажатия кнопки "Назад"
     */
    private fun setupBackPressedCallback() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Если есть изменения, предлагаем сохранить
                if (hasChanges) {
                    showSaveBeforeExitDialog()
                } else {
                    // Разрешаем стандартную обработку нажатия кнопки "Назад"
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    /**
     * Настраивает кнопки режимов редактирования
     */
    private fun setupEditButtons() {
        // Кнопка режима просмотра
        binding.buttonViewMode.setOnClickListener {
            setEditMode(EditMode.VIEW_ONLY)
        }

        // Кнопка перемещения маркеров
        binding.buttonMoveMarker.setOnClickListener {
            setEditMode(EditMode.MOVE_MARKER)
            Toast.makeText(
                this,
                "Нажмите на маркер, чтобы начать перемещение",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Кнопка соединения маркеров
        binding.buttonConnectMarkers.setOnClickListener {
            setEditMode(EditMode.CONNECT_MARKERS)
            Toast.makeText(
                this,
                getString(R.string.connect_markers_hint),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Устанавливает режим редактирования карты
     */
    private fun setEditMode(mode: EditMode) {
        binding.markerMapView.editMode = mode
        updateButtonsState()
    }

    /**
     * Обновляет состояние кнопок в зависимости от текущего режима
     */
    private fun updateButtonsState() {
        // Сначала сбрасываем стиль всех кнопок
        binding.buttonViewMode.setStrokeColorResource(R.color.button_stroke_default)
        binding.buttonMoveMarker.setStrokeColorResource(R.color.button_stroke_default)
        binding.buttonConnectMarkers.setStrokeColorResource(R.color.button_stroke_default)

        // Выделяем активную кнопку
        when (binding.markerMapView.editMode) {
            EditMode.VIEW_ONLY -> binding.buttonViewMode.setStrokeColorResource(R.color.button_stroke_active)
            EditMode.MOVE_MARKER -> binding.buttonMoveMarker.setStrokeColorResource(R.color.button_stroke_active)
            EditMode.CONNECT_MARKERS -> binding.buttonConnectMarkers.setStrokeColorResource(R.color.button_stroke_active)
        }
    }

    /**
     * Настраивает кнопки сохранения и настроек
     */
    private fun setupActionButtons() {
        // Кнопка сохранения
        binding.fabSave.setOnClickListener {
            saveMapData()
        }

        // Кнопка настроек
        binding.fabSettings.setOnClickListener {
            showSettingsDialog()
        }
    }

    /**
     * Показывает диалог настроек карты
     */
    private fun showSettingsDialog() {
        val options = arrayOf(
            getString(R.string.rename_map),
            getString(R.string.clear_markers),
            getString(R.string.reset_view)
        )

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.map_settings))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showRenameMapDialog()
                    1 -> showClearMarkersConfirmation()
                    2 -> binding.markerMapView.resetView()
                }
            }
            .show()
    }

    /**
     * Показывает диалог переименования карты
     */
    private fun showRenameMapDialog() {
        val editText = androidx.appcompat.widget.AppCompatEditText(this)
        editText.setText(mapName)
        editText.setSelection(mapName.length)

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.rename_map))
            .setView(editText)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    mapName = newName
                    supportActionBar?.title = mapName
                    hasChanges = true
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    /**
     * Показывает диалог подтверждения очистки карты
     */
    private fun showClearMarkersConfirmation() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.clear_markers))
            .setMessage(getString(R.string.confirm_clear_markers))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                binding.markerMapView.clearAllMarkers()
                hasChanges = true
            }
            .setNegativeButton(getString(R.string.no), null)
            .show()
    }

    /**
     * Загружает данные карты из Firebase
     */
    private fun loadMapData() {
        try {
            // Показываем индикатор загрузки
            showLoading(true)

            // Получаем данные карты
            firestore.collection("marker_maps")
                .document(mapId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        // Получаем название карты
                        mapName = document.getString("name") ?: getString(R.string.marker_map_title)
                        supportActionBar?.title = mapName

                        // Загружаем маркеры
                        loadMarkers()
                    } else {
                        showLoading(false)
                        Toast.makeText(
                            this,
                            getString(R.string.map_not_found),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .addOnFailureListener { e ->
                    showLoading(false)
                    Toast.makeText(
                        this,
                        getString(R.string.error_loading_map, e.message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } catch (e: Exception) {
            showLoading(false)
            Toast.makeText(
                this,
                getString(R.string.error_loading_map, e.message),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Загружает маркеры для карты
     */
    private fun loadMarkers() {
        try {
            firestore.collection("marker_maps")
                .document(mapId)
                .collection("markers")
                .get()
                .addOnSuccessListener { markerDocuments ->
                    val markers = mutableListOf<Marker>()

                    for (doc in markerDocuments) {
                        try {
                            val id = doc.id
                            val x = doc.getDouble("x")?.toFloat() ?: 0f
                            val y = doc.getDouble("y")?.toFloat() ?: 0f
                            val typeStr = doc.getString("type") ?: MarkerType.ROCK.name
                            val depth = doc.getDouble("depth")?.toFloat() ?: 0f
                            val notes = doc.getString("notes") ?: ""
                            val colorInt = doc.getLong("color")?.toInt() ?: MarkerColors.RED
                            val sizeStr = doc.getString("size") ?: MarkerSize.SMALL.name

                            // Преобразуем строку в тип маркера
                            val type = try {
                                MarkerType.valueOf(typeStr)
                            } catch (e: Exception) {
                                MarkerType.ROCK
                            }

                            // Преобразуем строку в размер маркера
                            val size = try {
                                MarkerSize.valueOf(sizeStr)
                            } catch (e: Exception) {
                                MarkerSize.SMALL
                            }

                            markers.add(Marker(id, x, y, type, depth, colorInt, size, notes))
                        } catch (e: Exception) {
                            // Пропускаем маркер с ошибкой
                        }
                    }

                    // Загружаем соединения
                    loadConnections(markers)
                }
                .addOnFailureListener { e ->
                    showLoading(false)
                    Toast.makeText(
                        this,
                        getString(R.string.error_loading_markers, e.message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } catch (e: Exception) {
            showLoading(false)
            Toast.makeText(
                this,
                getString(R.string.error_loading_markers, e.message),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Загружает соединения между маркерами
     */
    private fun loadConnections(markers: List<Marker>) {
        try {
            firestore.collection("marker_maps")
                .document(mapId)
                .collection("connections")
                .get()
                .addOnSuccessListener { connectionDocuments ->
                    val connections = mutableListOf<MarkerConnection>()

                    for (doc in connectionDocuments) {
                        try {
                            val id = doc.id
                            val marker1Id = doc.getString("marker1Id") ?: continue
                            val marker2Id = doc.getString("marker2Id") ?: continue
                            val notes = doc.getString("notes") ?: ""

                            connections.add(MarkerConnection(id, marker1Id, marker2Id, notes))
                        } catch (e: Exception) {
                            // Пропускаем соединение с ошибкой
                        }
                    }

                    // Устанавливаем данные на карту
                    binding.markerMapView.setMapData(markers, connections)

                    // Скрываем индикатор загрузки
                    showLoading(false)

                    // Сбрасываем флаг изменений
                    hasChanges = false
                }
                .addOnFailureListener { e ->
                    showLoading(false)
                    Toast.makeText(
                        this,
                        getString(R.string.error_loading_connections, e.message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } catch (e: Exception) {
            showLoading(false)
            Toast.makeText(
                this,
                getString(R.string.error_loading_connections, e.message),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Сохраняет данные карты в Firebase
     */
    private fun saveMapData() {
        try {
            // Показываем индикатор загрузки
            showLoading(true)

            // Получаем ID пользователя
            val userId = FirebaseManager.getCurrentUserId()
            if (userId == null) {
                showLoading(false)
                Toast.makeText(
                    this,
                    getString(R.string.auth_error),
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            // Данные карты
            val mapData = hashMapOf(
                "name" to mapName,
                "userId" to userId,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            // Создаем или обновляем документ карты
            firestore.collection("marker_maps")
                .document(mapId)
                .set(mapData)
                .addOnSuccessListener {
                    // Сохраняем маркеры
                    saveMarkers()
                }
                .addOnFailureListener { e ->
                    showLoading(false)
                    Toast.makeText(
                        this,
                        getString(R.string.error_saving_map, e.message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } catch (e: Exception) {
            showLoading(false)
            Toast.makeText(
                this,
                getString(R.string.error_saving_map, e.message),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Сохраняет маркеры карты
     */
    private fun saveMarkers() {
        try {
            // Получаем коллекцию маркеров
            val markersCollection = firestore.collection("marker_maps")
                .document(mapId)
                .collection("markers")

            // Удаляем существующие маркеры
            markersCollection.get()
                .addOnSuccessListener { snapshot ->
                    // Создаем пакетную операцию
                    val batch = firestore.batch()

                    // Удаляем существующие маркеры
                    for (doc in snapshot.documents) {
                        batch.delete(doc.reference)
                    }

                    // Добавляем новые маркеры
                    val markers = binding.markerMapView.getMarkers()

                    for (marker in markers) {
                        val data = hashMapOf(
                            "x" to marker.x,
                            "y" to marker.y,
                            "type" to marker.type.name,
                            "depth" to marker.depth,
                            "color" to marker.color,
                            "size" to marker.size.name,
                            "notes" to marker.notes
                        )

                        batch.set(markersCollection.document(marker.id), data)
                    }

                    // Выполняем пакетную операцию
                    batch.commit()
                        .addOnSuccessListener {
                            // Сохраняем соединения
                            saveConnections()
                        }
                        .addOnFailureListener { e ->
                            showLoading(false)
                            Toast.makeText(
                                this,
                                getString(R.string.error_saving_markers, e.message),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
                .addOnFailureListener { e ->
                    showLoading(false)
                    Toast.makeText(
                        this,
                        getString(R.string.error_saving_markers, e.message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } catch (e: Exception) {
            showLoading(false)
            Toast.makeText(
                this,
                getString(R.string.error_saving_markers, e.message),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Сохраняет соединения между маркерами
     */
    private fun saveConnections() {
        try {
            // Получаем коллекцию соединений
            val connectionsCollection = firestore.collection("marker_maps")
                .document(mapId)
                .collection("connections")

            // Удаляем существующие соединения
            connectionsCollection.get()
                .addOnSuccessListener { snapshot ->
                    // Создаем пакетную операцию
                    val batch = firestore.batch()

                    // Удаляем существующие соединения
                    for (doc in snapshot.documents) {
                        batch.delete(doc.reference)
                    }

                    // Добавляем новые соединения
                    val connections = binding.markerMapView.getConnections()

                    for (connection in connections) {
                        val data = hashMapOf(
                            "marker1Id" to connection.marker1Id,
                            "marker2Id" to connection.marker2Id,
                            "notes" to connection.notes
                        )

                        batch.set(connectionsCollection.document(connection.id), data)
                    }

                    // Выполняем пакетную операцию
                    batch.commit()
                        .addOnSuccessListener {
                            // Сохранение завершено
                            showLoading(false)

                            // Сбрасываем флаг изменений
                            hasChanges = false

                            // Показываем сообщение
                            Toast.makeText(
                                this,
                                getString(R.string.map_saved),
                                Toast.LENGTH_SHORT
                            ).show()

                            // Возвращаем результат
                            setResult(RESULT_OK, Intent().apply {
                                putExtra(EXTRA_MAP_ID, mapId)
                                putExtra(EXTRA_MAP_NAME, mapName)
                            })
                        }
                        .addOnFailureListener { e ->
                            showLoading(false)
                            Toast.makeText(
                                this,
                                getString(R.string.error_saving_connections, e.message),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
                .addOnFailureListener { e ->
                    showLoading(false)
                    Toast.makeText(
                        this,
                        getString(R.string.error_saving_connections, e.message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } catch (e: Exception) {
            showLoading(false)
            Toast.makeText(
                this,
                getString(R.string.error_saving_connections, e.message),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Показывает/скрывает индикатор загрузки
     */
    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    // Реализация методов интерфейса MarkerMapListener

    override fun onMarkerAdded(marker: Marker) {
        hasChanges = true
    }

    override fun onMarkerSelected(marker: Marker) {
        // При необходимости можно показать детали маркера
    }

    override fun onMarkerMoved(marker: Marker) {
        hasChanges = true
    }

    override fun onMarkerDeleted(marker: Marker) {
        hasChanges = true
    }

    override fun onConnectionCreated(connection: MarkerConnection) {
        hasChanges = true
    }

    override fun onLongPress(x: Float, y: Float) {
        // Показываем диалог добавления маркера
        MarkerPopupDialog(
            context = this,
            markerX = x,
            markerY = y,
            onMarkerCreated = { newMarker ->
                // Добавляем новый маркер на карту
                binding.markerMapView.addMarker(
                    x = newMarker.x,
                    y = newMarker.y,
                    type = newMarker.type,
                    depth = newMarker.depth,
                    color = newMarker.color,
                    size = newMarker.size,
                    notes = newMarker.notes
                )

                // Отмечаем, что были внесены изменения
                hasChanges = true

                // Уведомляем пользователя
                Toast.makeText(this, "Маркер добавлен", Toast.LENGTH_SHORT).show()
            },
            onMarkerUpdated = { /* Не используется для нового маркера */ },
            onMarkerDeleted = { /* Не используется для нового маркера */ }
        ).show()
    }

    override fun onMarkerLongPress(marker: Marker, x: Float, y: Float) {
        // Показываем диалог редактирования маркера
        MarkerPopupDialog(
            context = this,
            isEdit = true,
            marker = marker,
            onMarkerCreated = { /* Не используется для существующего маркера */ },
            onMarkerUpdated = { updatedMarker ->
                // Обновляем маркер
                val markers = binding.markerMapView.getMarkers().toMutableList()
                val index = markers.indexOfFirst { it.id == updatedMarker.id }
                if (index != -1) {
                    markers[index] = updatedMarker
                    binding.markerMapView.setMapData(
                        markers,
                        binding.markerMapView.getConnections()
                    )

                    // Отмечаем, что были внесены изменения
                    hasChanges = true

                    // Уведомляем пользователя
                    Toast.makeText(this, "Маркер обновлен", Toast.LENGTH_SHORT).show()
                }
            },
            onMarkerDeleted = { markerToDelete ->
                // Удаляем маркер
                binding.markerMapView.removeMarker(markerToDelete)

                // Отмечаем, что были внесены изменения
                hasChanges = true

                // Уведомляем пользователя
                Toast.makeText(this, "Маркер удален", Toast.LENGTH_SHORT).show()
            }
        ).show()
    }

    /**
     * Обработка нажатия кнопки "Назад" на ActionBar
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Показывает диалог о сохранении изменений перед выходом
     */
    private fun showSaveBeforeExitDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.unsaved_changes))
            .setMessage(getString(R.string.save_before_exit))
            .setPositiveButton(getString(R.string.save_and_exit)) { _, _ ->
                // Сохраняем карту и выходим
                saveMapData()
                // Активность завершится после успешного сохранения
            }
            .setNegativeButton(getString(R.string.discard_and_exit)) { _, _ ->
                // Выходим без сохранения
                finish()
            }
            .setNeutralButton(getString(R.string.stay), null)
            .show()
    }

    companion object {
        // Константы для передачи данных через Intent
        const val EXTRA_MAP_ID = "extra_map_id"
        const val EXTRA_MAP_NAME = "extra_map_name"
    }
}