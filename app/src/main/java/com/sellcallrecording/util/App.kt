package com.sellcallrecording.util

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application() {

    override fun onCreate() {
        super.onCreate()

        Thread.setDefaultUncaughtExceptionHandler(
            TopExceptionHandler(
                "/mnt/sdcard/",
                applicationContext
            )
        )
    }
}