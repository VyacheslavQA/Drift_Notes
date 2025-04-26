package com.example.driftnotes.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.driftnotes.R
import com.example.driftnotes.databinding.ActivityImageCropperBinding
import com.example.driftnotes.utils.AnimationHelper
import com.yalantis.ucrop.UCrop
import java.io.File
import java.util.UUID

/**
 * Активность для обрезки изображения (аватара)
 */
class ImageCropperActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImageCropperBinding
    private var sourceUri: Uri? = null
    private var destinationUri: Uri? = null
    private val TAG = "ImageCropperActivity"

    companion object {
        const val EXTRA_SOURCE_URI = "source_uri"
        const val RESULT_CROPPED_URI = "cropped_uri"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageCropperBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Настраиваем ActionBar
        supportActionBar?.title = "Обрезать аватар"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Получаем URI изображения, которое нужно обрезать
        sourceUri = intent.getParcelableExtra(EXTRA_SOURCE_URI)

        if (sourceUri == null) {
            Toast.makeText(this, "Ошибка: изображение не найдено", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        try {
            // Создаем временный файл для результата обрезки
            val destinationFile = File(cacheDir, "cropped_${UUID.randomUUID()}.jpg")
            destinationUri = Uri.fromFile(destinationFile)

            startCrop(sourceUri!!, destinationUri!!)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при инициализации обрезчика: ${e.message}", e)
            Toast.makeText(this, "Не удалось обработать изображение", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun startCrop(sourceUri: Uri, destinationUri: Uri) {
        val options = UCrop.Options().apply {
            setCompressionQuality(80) // Качество сжатия JPEG
            setHideBottomControls(false)
            setFreeStyleCropEnabled(false) // Ограничиваем обрезку квадратом
            setCircleDimmedLayer(true) // Затемнение вокруг круга
            setCropFrameColor(resources.getColor(R.color.primary, theme))
            setCropGridColor(resources.getColor(R.color.primary, theme))
            setToolbarColor(resources.getColor(R.color.surface, theme))
            setStatusBarColor(resources.getColor(R.color.background, theme))
            setToolbarWidgetColor(resources.getColor(R.color.primary, theme))
            setLogoColor(resources.getColor(R.color.primary, theme))
            setActiveControlsWidgetColor(resources.getColor(R.color.primary, theme))
        }

        // Используем стандартный метод для задания квадратного соотношения сторон
        UCrop.of(sourceUri, destinationUri)
            .withAspectRatio(1f, 1f) // Устанавливаем соотношение сторон 1:1
            .withOptions(options)
            .start(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == UCrop.REQUEST_CROP) {
            if (resultCode == RESULT_OK) {
                val resultUri = UCrop.getOutput(data!!)
                if (resultUri != null) {
                    // Возвращаем результат обрезки
                    val resultIntent = Intent()
                    resultIntent.putExtra(RESULT_CROPPED_URI, resultUri.toString())
                    setResult(RESULT_OK, resultIntent)

                    // Завершаем активность с анимацией
                    AnimationHelper.finishWithAnimation(this)
                } else {
                    Toast.makeText(this, "Не удалось получить результат обрезки", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_CANCELED)
                    finish()
                }
            } else if (resultCode == UCrop.RESULT_ERROR) {
                val error = UCrop.getError(data!!)
                Toast.makeText(this, "Ошибка при обрезке: ${error?.message}", Toast.LENGTH_SHORT).show()
                setResult(RESULT_CANCELED)
                finish()
            } else {
                // Пользователь отменил операцию
                setResult(RESULT_CANCELED)
                finish()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        setResult(RESULT_CANCELED)
        AnimationHelper.finishWithAnimation(this)
    }
}