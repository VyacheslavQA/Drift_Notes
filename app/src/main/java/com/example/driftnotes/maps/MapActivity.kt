package com.example.driftnotes.maps

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.driftnotes.R
import com.example.driftnotes.databinding.ActivityMapBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import java.io.IOException
import java.util.Locale

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapBinding
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var currentMarker: Marker? = null
    private var selectedLatLng: LatLng? = null
    private var selectedLocationName: String = ""

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    // Переменная для отслеживания текущего типа карты
    private var currentMapType = GoogleMap.MAP_TYPE_NORMAL

    // Новые переменные для режима просмотра
    private var isViewOnlyMode = false
    private var viewLatitude = 0.0
    private var viewLongitude = 0.0
    private var viewTitle = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MapActivity", "onCreate: Starting MapActivity")
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Проверяем режим просмотра
        isViewOnlyMode = intent.getBooleanExtra("view_only", false)
        if (isViewOnlyMode) {
            viewLatitude = intent.getDoubleExtra("latitude", 0.0)
            viewLongitude = intent.getDoubleExtra("longitude", 0.0)
            viewTitle = intent.getStringExtra("title") ?: ""

            // В режиме просмотра скрываем нижнюю панель с выбором местоположения
            binding.bottomPanel.visibility = View.GONE
        }

        try {
            // Получаем SupportMapFragment и запрашиваем уведомление при готовности карты
            val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as? SupportMapFragment
            Log.d("MapActivity", "Map fragment found: ${mapFragment != null}")
            mapFragment?.getMapAsync(this)
        } catch (e: Exception) {
            Log.e("MapActivity", "Error initializing map: ${e.message}", e)
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Настраиваем слушатели, только если не в режиме просмотра
        binding.fabMapType.setOnClickListener {
            toggleMapType()
        }

        if (!isViewOnlyMode) {
            binding.fabMyLocation.setOnClickListener {
                checkLocationPermissionAndGetLocation()
            }

            binding.fabSearch.setOnClickListener {
                showSearchDialog()
            }

            binding.buttonConfirmLocation.setOnClickListener {
                confirmSelectedLocation()
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        Log.d("MapActivity", "onMapReady called")
        googleMap = map

        // Настраиваем карту
        googleMap.uiSettings.isZoomControlsEnabled = true
        // Улучшаем юзабилити карты
        googleMap.uiSettings.isCompassEnabled = true
        googleMap.uiSettings.isMapToolbarEnabled = true

        // Устанавливаем тип карты по умолчанию
        googleMap.mapType = currentMapType

        if (isViewOnlyMode) {
            // В режиме просмотра сразу показываем место с заголовком
            val location = LatLng(viewLatitude, viewLongitude)
            currentMarker = googleMap.addMarker(
                MarkerOptions()
                    .position(location)
                    .title(viewTitle)
            )
            currentMarker?.showInfoWindow()
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
        } else {
            // В обычном режиме позволяем выбирать место на карте
            googleMap.setOnMapClickListener { latLng ->
                Log.d("MapActivity", "Map clicked at lat: ${latLng.latitude}, lng: ${latLng.longitude}")
                updateSelectedLocation(latLng)
            }

            // Проверяем разрешение на местоположение и пытаемся показать текущее местоположение
            checkLocationPermissionAndGetLocation()
        }
    }

    private fun toggleMapType() {
        currentMapType = if (currentMapType == GoogleMap.MAP_TYPE_NORMAL) {
            // Переключаем на спутник
            GoogleMap.MAP_TYPE_HYBRID
        } else {
            // Переключаем на обычную карту
            GoogleMap.MAP_TYPE_NORMAL
        }

        // Применяем новый тип карты
        if (::googleMap.isInitialized) {
            googleMap.mapType = currentMapType
            // Показываем уведомление о смене типа карты
            val mapTypeName = if (currentMapType == GoogleMap.MAP_TYPE_NORMAL)
                getString(R.string.map_type_normal) else getString(R.string.map_type_satellite)
            Toast.makeText(this, mapTypeName, Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkLocationPermissionAndGetLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            enableMyLocation()
        } else {
            // Запрашиваем разрешение
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        try {
            googleMap.isMyLocationEnabled = true
            Log.d("MapActivity", "My location enabled successfully")

            // Получаем последнее известное местоположение
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val currentLatLng = LatLng(it.latitude, it.longitude)
                    Log.d("MapActivity", "Last location: lat=${it.latitude}, lng=${it.longitude}")
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12f))

                    // Если еще не выбрана локация, устанавливаем маркер на текущее местоположение
                    if (selectedLatLng == null && !isViewOnlyMode) {
                        updateSelectedLocation(currentLatLng)
                    }
                } ?: Log.d("MapActivity", "Last location is null")
            }.addOnFailureListener { e ->
                Log.e("MapActivity", "Failed to get last location: ${e.message}", e)
            }
        } catch (e: Exception) {
            Log.e("MapActivity", "Error enabling location: ${e.message}", e)
        }
    }

    private fun updateSelectedLocation(latLng: LatLng) {
        // Обновляем выбранные координаты
        selectedLatLng = latLng

        // Удаляем предыдущий маркер, если он существует
        currentMarker?.remove()

        // Добавляем новый маркер
        currentMarker = googleMap.addMarker(
            MarkerOptions()
                .position(latLng)
                .title(getString(R.string.selected_location))
        )

        // Обновляем отображаемую информацию
        binding.textViewCoordinates.text = getString(
            R.string.coordinates_format,
            latLng.latitude,
            latLng.longitude
        )

        // Получаем название места по координатам
        getAddressFromLocation(latLng)
    }

    private fun getAddressFromLocation(latLng: LatLng) {
        try {
            val geocoder = Geocoder(this, Locale.getDefault())

            // Начиная с Android 9, рекомендуется использовать асинхронный getFromLocation
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)

            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                val addressLines = mutableListOf<String>()

                // Собираем компоненты адреса
                for (i in 0..address.maxAddressLineIndex) {
                    addressLines.add(address.getAddressLine(i))
                }

                // Используем название населенного пункта или полный адрес
                selectedLocationName = address.locality ?: address.getAddressLine(0) ?:
                        getString(R.string.unknown_location)

                binding.textViewSelectedLocation.text = getString(
                    R.string.selected_place_format,
                    selectedLocationName
                )
            } else {
                selectedLocationName = getString(R.string.unknown_location)
                binding.textViewSelectedLocation.text = getString(
                    R.string.selected_place_format,
                    selectedLocationName
                )
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("MapActivity", "Error getting address: ${e.message}")
            selectedLocationName = getString(R.string.unknown_location)
            binding.textViewSelectedLocation.text = getString(
                R.string.selected_place_format,
                selectedLocationName
            )
        }
    }

    private fun showSearchDialog() {
        // Здесь можно реализовать диалог поиска места
        // В простом варианте можно использовать EditText в AlertDialog
        // Для более сложного варианта можно использовать Places API от Google

        // Пример простого диалога:
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.search_location)

        // ToDo: Добавить поле ввода для поиска
        // В данном примере просто показываем заглушку

        Toast.makeText(
            this,
            "Функция поиска будет реализована в следующем обновлении",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun confirmSelectedLocation() {
        if (selectedLatLng == null) {
            Toast.makeText(
                this,
                R.string.please_select_location,
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Создаем Intent для возврата выбранной локации
        val resultIntent = Intent()
        resultIntent.putExtra("latitude", selectedLatLng?.latitude)
        resultIntent.putExtra("longitude", selectedLatLng?.longitude)
        resultIntent.putExtra("location_name", selectedLocationName)

        setResult(RESULT_OK, resultIntent)
        finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation()
            } else {
                Toast.makeText(
                    this,
                    R.string.location_permission_denied,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}