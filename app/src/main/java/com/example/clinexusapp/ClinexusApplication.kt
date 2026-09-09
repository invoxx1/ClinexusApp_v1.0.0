package com.example.clinexusapp

import android.app.Application
import com.example.clinexusapp.util.NotificationHelper
import com.example.clinexusapp.util.SessionManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ClinexusApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SessionManager.init(this)
        NotificationHelper.createNotificationChannel(this)
    }
}
