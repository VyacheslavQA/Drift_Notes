package com.example.driftnotes.fishing.markermap

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.driftnotes.R
import com.example.driftnotes.databinding.ActivityMarkerMapBinding

class MarkerMapActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMarkerMapBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMarkerMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Добавляем кнопку "Назад" в ActionBar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.marker_map_title)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
