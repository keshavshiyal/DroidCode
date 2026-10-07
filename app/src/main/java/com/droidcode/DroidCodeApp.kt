package com.droidcode

import android.app.Application
import com.droidcode.debug.StackTraceManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DroidCodeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        StackTraceManager.init(this)
    }
}
