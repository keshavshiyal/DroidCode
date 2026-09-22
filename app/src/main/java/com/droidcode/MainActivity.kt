package com.droidcode

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.droidcode.filesystem.SafUtils
import com.droidcode.ui.MainShell

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SafUtils.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            MainShell()
        }
    }
}
