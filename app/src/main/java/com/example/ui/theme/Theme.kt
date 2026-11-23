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
    primary = SapphirePrimaryDark,
    onPrimary = SapphireOnPrimaryDark,
    primaryContainer = SapphireContainerDark,
    onPrimaryContainer = SapphireOnContainerDark,
    secondary = StudyGoldSecondaryDark,
    onSecondary = StudyGoldOnSecondaryDark,
    secondaryContainer = StudyGoldContainerDark,
    onSecondaryContainer = StudyGoldOnContainerDark,
    tertiary = SageTertiaryDark,
    onTertiary = SageOnTertiaryDark,
    tertiaryContainer = SageContainerDark,
    onTertiaryContainer = SageOnContainerDark,
    background = OxfordNightBg,
    onBackground = Color(0xFFF1F5F9),
    surface = OxfordSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = OxfordSurfaceVariant,
    onSurfaceVariant = Color(0xFFA5B4CE),
    outline = OxfordOutline,
    error = ErrorRedDark,
    onError = Color(0xFF450A0A)
)

private val LightColorScheme = lightColorScheme(
    primary = SapphirePrimaryLight,
    onPrimary = SapphireOnPrimaryLight,
    primaryContainer = SapphireContainerLight,
    onPrimaryContainer = SapphireOnContainerLight,
    secondary = StudyGoldSecondaryLight,
    onSecondary = StudyGoldOnSecondaryLight,
    secondaryContainer = StudyGoldContainerLight,
    onSecondaryContainer = StudyGoldOnContainerLight,
    tertiary = SageTertiaryLight,
    onTertiary = SageOnTertiaryLight,
    tertiaryContainer = SageContainerLight,
    onTertiaryContainer = SageOnContainerLight,
    background = StudyIvoryBg,
    onBackground = Color(0xFF0F172A),
    surface = StudyPaperSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = StudySageSurfaceVariant,
    onSurfaceVariant = Color(0xFF475569),
    outline = StudyOutlineLight,
    error = ErrorRed,
    onError = Color.White
)

private val FocusShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = FocusShapes,
            content = content
        )
    }
}
