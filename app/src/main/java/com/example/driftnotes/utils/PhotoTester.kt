package com.example.driftnotes.utils

import android.content.Context
import android.util.Log

object PhotoTester {
    private const val TAG = "PhotoTester"

    fun testPhotoUrls(context: Context, urls: List<String>) {
        Log.d(TAG, "Тестирование URL фотографий: ${urls.size}")
        for (url in urls) {
            Log.d(TAG, "URL фото: $url")
        }
    }
}