package day.azimuth.observer

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import day.azimuth.observer.data.local.AzimuthPreferences
import day.azimuth.observer.service.UpdateChecker
import day.azimuth.observer.ui.AzimuthNavHost
import day.azimuth.observer.ui.theme.AzimuthTheme
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var prefs: AzimuthPreferences

    @Inject
    lateinit var updateChecker: UpdateChecker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AzimuthTheme {
                AzimuthNavHost()
            }
        }

        lifecycleScope.launch {
            prefs.keepScreenOn.collect { keepOn ->
                if (keepOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
        }

        lifecycleScope.launch {
            updateChecker.checkForUpdate(BuildConfig.VERSION_CODE)
        }
    }
}
