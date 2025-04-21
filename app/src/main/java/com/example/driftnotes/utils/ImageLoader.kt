package com.example.driftnotes.utils

import android.content.Context
import android.util.Log
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.driftnotes.R

/**
 * Утилитарный класс для улучшенной загрузки изображений
 */
object ImageLoader {
    private const val TAG = "ImageLoader"

    /**
     * Загружает изображение с URL в ImageView с улучшенными параметрами
     */
    fun loadImage(context: Context, url: String, imageView: ImageView, isFullscreen: Boolean = false) {
        try {
            Log.d(TAG, "Загрузка изображения: $url")

            val requestBuilder = Glide.with(context)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .timeout(30000) // 30 секунд
                .error(R.drawable.ic_photo_placeholder)
                .placeholder(R.drawable.ic_photo_placeholder)

            // Применяем разный scaleType в зависимости от режима
            if (isFullscreen) {
                requestBuilder.fitCenter()
            } else {
                requestBuilder.centerCrop()
            }

            requestBuilder.into(imageView)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка загрузки изображения: ${e.message}", e)
            imageView.setImageResource(R.drawable.ic_photo_placeholder)
        }
    }
}