package com.hapticrace.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.hapticrace.app.ui.HapticRaceNavHost
import com.hapticrace.app.ui.theme.DarkCarbon
import com.hapticrace.app.ui.theme.HapticRaceTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as HapticRaceApp

        setContent {
            HapticRaceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkCarbon
                ) {
                    HapticRaceNavHost(
                        repository = app.timelineRepository,
                        hapticManager = app.hapticManager
                    )
                }
            }
        }
    }
}
