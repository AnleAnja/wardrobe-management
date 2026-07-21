package com.anleanja.wardrobe

import android.app.Application
import com.anleanja.wardrobe.crash.CrashReporter
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class WardrobeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashReporter.initialize()
    }
}
