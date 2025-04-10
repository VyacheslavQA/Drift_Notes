package com.example.driftnotes

import android.app.Application

class DriftNotesApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Инициализация Firebase происходит автоматически через google-services.json
    }
}