package com.example.driftnotes.fishing

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.driftnotes.R
import com.example.driftnotes.databinding.ActivityFullscreenPhotoBinding
import com.example.driftnotes.utils.AnimationHelper

class FullscreenPhotoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFullscreenPhotoBinding
    private lateinit var photoAdapter: PhotoPagerAdapter
    private var photoUrls: ArrayList<String> = arrayListOf()
    private var initialPosition: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFullscreenPhotoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Скрываем системный UI для полноэкранного режима
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        supportActionBar?.hide()

        // Получаем список фотографий и начальную позицию из Intent
        photoUrls = intent.getStringArrayListExtra("photos") ?: arrayListOf()
        initialPosition = intent.getIntExtra("position", 0)

        // Настраиваем ViewPager для фотографий
        photoAdapter = PhotoPagerAdapter(photoUrls)
        binding.viewPagerFullscreen.adapter = photoAdapter
        binding.viewPagerFullscreen.setCurrentItem(initialPosition, false)

        // Настраиваем индикатор страниц
        binding.dotsIndicator.attachTo(binding.viewPagerFullscreen)
        if (photoUrls.size <= 1) {
            binding.dotsIndicator.visibility = View.GONE
        }

        // Настраиваем кнопку закрытия
        binding.buttonClose.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }

        // Отслеживаем изменения страницы
        binding.viewPagerFullscreen.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.textViewCounter.text = "${position + 1}/${photoUrls.size}"
            }
        })

        // Начальное значение счетчика
        binding.textViewCounter.text = "${initialPosition + 1}/${photoUrls.size}"

        // Скрываем счетчик, если только одно фото
        if (photoUrls.size <= 1) {
            binding.textViewCounter.visibility = View.GONE
        }
    }

    override fun onBackPressed() {
        finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
}