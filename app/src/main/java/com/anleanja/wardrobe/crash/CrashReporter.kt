package com.anleanja.wardrobe.crash

import android.util.Log
import com.anleanja.wardrobe.BuildConfig
import com.google.firebase.crashlytics.FirebaseCrashlytics

object CrashReporter {
    private const val TAG = "WardrobeCrash"

    fun initialize() {
        if (BuildConfig.DEBUG) return

        runCatching {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        }.onFailure {
            Log.w(TAG, "Firebase Crashlytics unavailable. Add google-services.json to enable crash reporting.")
        }

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                FirebaseCrashlytics.getInstance().recordException(throwable)
            }
            Log.e(TAG, "Uncaught exception on thread ${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
