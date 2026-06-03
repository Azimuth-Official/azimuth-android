package day.azimuth.observer.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val Navy = Color(0xFF0A0F1E)
private val Surface = Color(0xFF111827)
private val SurfaceVariant = Color(0xFF1E293B)
private val Amber = Color(0xFFF59E0B)
private val Teal = Color(0xFF14B8A6)

private val DarkColorScheme = darkColorScheme(
    primary = Amber,
    secondary = Teal,
    background = Navy,
    surface = Surface,
    surfaceVariant = SurfaceVariant,
    onPrimary = Navy,
    onSecondary = Navy,
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF94A3B8),
)

private val LightColorScheme = lightColorScheme(
    primary = Amber,
    secondary = Teal,
)

@Composable
fun AzimuthTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
