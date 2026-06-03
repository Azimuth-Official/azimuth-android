package day.azimuth.observer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import day.azimuth.observer.ui.AzimuthNavHost
import day.azimuth.observer.ui.theme.AzimuthTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AzimuthTheme {
                AzimuthNavHost()
            }
        }
    }
}
