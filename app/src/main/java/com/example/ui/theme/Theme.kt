package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = QuramerEmeraldDark,
    onPrimary = QuramerOnEmeraldDark,
    primaryContainer = QuramerEmeraldContainerDark,
    onPrimaryContainer = QuramerOnEmeraldContainerDark,
    secondary = QuramerGoldDark,
    onSecondary = QuramerOnGoldDark,
    secondaryContainer = QuramerGoldContainerDark,
    onSecondaryContainer = QuramerOnGoldContainerDark,
    tertiary = QuramerTealTertiaryDark,
    onTertiary = QuramerOnTealDark,
    tertiaryContainer = QuramerTealContainerDark,
    onTertiaryContainer = QuramerOnTealContainerDark,
    background = QuramerDarkGreenBg,
    onBackground = Color(0xFFF6F3EB),
    surface = QuramerDarkSurface,
    onSurface = Color(0xFFF6F3EB),
    surfaceVariant = QuramerDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFB3C9BF),
    outline = QuramerDarkOutline,
    error = ErrorRedDark,
    onError = Color(0xFF450A0A)
)

private val LightColorScheme = lightColorScheme(
    primary = QuramerEmeraldLight,
    onPrimary = QuramerOnEmeraldLight,
    primaryContainer = QuramerEmeraldContainerLight,
    onPrimaryContainer = QuramerOnEmeraldContainerLight,
    secondary = QuramerGoldLight,
    onSecondary = QuramerOnGoldLight,
    secondaryContainer = QuramerGoldContainerLight,
    onSecondaryContainer = QuramerOnGoldContainerLight,
    tertiary = QuramerTealTertiaryLight,
    onTertiary = QuramerOnTealLight,
    tertiaryContainer = QuramerTealContainerLight,
    onTertiaryContainer = QuramerOnTealContainerLight,
    background = QuramerIvoryBg,
    onBackground = Color(0xFF0A221A),
    surface = QuramerIvorySurface,
    onSurface = Color(0xFF0A221A),
    surfaceVariant = QuramerMintVariant,
    onSurfaceVariant = Color(0xFF3E594E),
    outline = QuramerOutlineLight,
    error = ErrorRed,
    onError = Color.White
)

private val FocusShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    layoutDirection: LayoutDirection = LayoutDirection.Rtl,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = FocusShapes,
            content = content
        )
    }
}
