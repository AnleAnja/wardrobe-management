package com.example.wardrobe.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val WardrobeLightColorScheme = lightColorScheme(
    primary = Color(0xFF6E4B5B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD8E6),
    onPrimaryContainer = Color(0xFF29151F),
    secondary = Color(0xFF745B63),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD9E2),
    onSecondaryContainer = Color(0xFF2B151D),
    tertiary = Color(0xFF7B5636),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDCC3),
    onTertiaryContainer = Color(0xFF2E1500),
    background = Color(0xFFFFFBFF),
    onBackground = Color(0xFF201A1D),
    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF201A1D),
    surfaceVariant = Color(0xFFF1DDE4),
    onSurfaceVariant = Color(0xFF50434A)
)

private val WardrobeDarkColorScheme = darkColorScheme(
    primary = Color(0xFFE3B8C9),
    onPrimary = Color(0xFF402A34),
    primaryContainer = Color(0xFF563F4A),
    onPrimaryContainer = Color(0xFFFFD8E6),
    secondary = Color(0xFFE3BDC7),
    onSecondary = Color(0xFF422932),
    secondaryContainer = Color(0xFF5A3F48),
    onSecondaryContainer = Color(0xFFFFD9E2),
    tertiary = Color(0xFFEDBE96),
    onTertiary = Color(0xFF48290F),
    tertiaryContainer = Color(0xFF623F24),
    onTertiaryContainer = Color(0xFFFFDCC3),
    background = Color(0xFF171215),
    onBackground = Color(0xFFEBDFE3),
    surface = Color(0xFF171215),
    onSurface = Color(0xFFEBDFE3),
    surfaceVariant = Color(0xFF50434A),
    onSurfaceVariant = Color(0xFFD4C2CA)
)

private val WardrobeShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun WardrobeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> WardrobeDarkColorScheme
        else -> WardrobeLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = WardrobeShapes,
        content = content
    )
}
