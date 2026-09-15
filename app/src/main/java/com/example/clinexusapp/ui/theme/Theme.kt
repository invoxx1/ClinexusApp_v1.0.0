package com.example.clinexusapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFAFCBFF),
    secondary = Color(0xFFB9C8E3),
    tertiary = Color(0xFF8BD5E5),
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B),
    onPrimary = Color(0xFF102F5C),
    onSecondary = Color(0xFF233249),
    onBackground = SoftMist,
    onSurface = SoftMist,
    surfaceVariant = Color(0xFF2B3950),
    onSurfaceVariant = Color(0xFFC1CDDF),
    primaryContainer = Color(0xFF294777),
    onPrimaryContainer = Color(0xFFD7E5FF),
    outline = Color(0xFF8898AE),
    outlineVariant = Color(0xFF3B4B63),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF5B2024),
    onErrorContainer = Color(0xFFFFDAD6),
)

private val LightColorScheme = lightColorScheme(
    primary = DeepTeal,
    secondary = VibrantTeal,
    tertiary = MintSparkle,
    background = SoftMist,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = RoyalNavy,
    onSurface = RoyalNavy,
    surfaceVariant = MintSparkle,
    onSurfaceVariant = SlateGray,
    primaryContainer = MintSparkle,
    onPrimaryContainer = RoyalNavy,
    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFFE2E8F0),
)

@Composable
fun ClinexusAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    val deviceDensity = LocalDensity.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    // Keep the app's typography at its designed size even if the phone uses a
    // larger system font scale. Display density still follows the device.
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = deviceDensity.density,
            fontScale = 1f,
        ),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}
