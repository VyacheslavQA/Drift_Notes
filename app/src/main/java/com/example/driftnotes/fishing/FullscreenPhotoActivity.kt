package com.example.driftnotes.fishing

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
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

    private val TAG = "FullscreenPhotoActivity"

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

        Log.d(TAG, "Полученные URL фотографий: $photoUrls")
        Log.d(TAG, "Начальная позиция: $initialPosition")

        if (photoUrls.isEmpty()) {
            Toast.makeText(this, "Нет фотографий для отображения", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Проверяем, что начальная позиция в пределах списка
        if (initialPosition >= photoUrls.size) {
            initialPosition = 0
            Log.w(TAG, "Начальная позиция была за пределами списка, сброшена на 0")
        }

        try {
            // Настраиваем ViewPager для фотографий с режимом полноэкранного просмотра
            photoAdapter = PhotoPagerAdapter(
                photoUrls,
                true, // Указываем, что это полноэкранный режим
                null  // В полноэкранном режиме нет обработчика клика
            )
            binding.viewPagerFullscreen.adapter = photoAdapter
            binding.viewPagerFullscreen.setCurrentItem(initialPosition, false)

            // Устанавливаем offscreenPageLimit для предзагрузки соседних страниц
            binding.viewPagerFullscreen.offscreenPageLimit = 1

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
                    Log.d(TAG, "Выбрана страница $position")

                    // Принудительно обновляем текущую страницу для повторной загрузки изображения
                    photoAdapter.notifyItemChanged(position)
                }
            })

            // Начальное значение счетчика
            binding.textViewCounter.text = "${initialPosition + 1}/${photoUrls.size}"

            // Скрываем счетчик, если только одно фото
            if (photoUrls.size <= 1) {
                binding.textViewCounter.visibility = View.GONE
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при настройке просмотрщика фотографий", e)
            Toast.makeText(this, "Ошибка при отображении фотографий", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onBackPressed() {
        finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
}