package com.droidcode

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DroidCodeApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
