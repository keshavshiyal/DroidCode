package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.filesystem.SafUtils
import com.example.ui.MainShell

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
