package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val CareBridgeLightColorScheme = lightColorScheme(
    primary = SagePrimaryLight,
    onPrimary = SageOnPrimaryLight,
    primaryContainer = SageContainerLight,
    onPrimaryContainer = SageOnContainerLight,
    secondary = TerracottaSecondaryLight,
    onSecondary = TerracottaOnSecondaryLight,
    secondaryContainer = TerracottaContainerLight,
    onSecondaryContainer = TerracottaOnContainerLight,
    tertiary = OchreTertiaryLight,
    onTertiary = OchreOnTertiaryLight,
    tertiaryContainer = OchreContainerLight,
    onTertiaryContainer = OchreOnTertiaryContainerLight,
    background = LinenBackgroundLight,
    onBackground = CharcoalInkLight,
    surface = LinenBackgroundLight,
    onSurface = CharcoalInkLight,
    surfaceVariant = ParchmentSurfaceLight,
    onSurfaceVariant = MutedOliveLight,
    surfaceContainerLowest = SageOnPrimaryLight,
    surfaceContainerLow = ParchmentSurfaceLowLight,
    surfaceContainer = ParchmentSurfaceLight,
    surfaceContainerHigh = ParchmentSurfaceHighLight,
    outline = MutedOliveLight,
    outlineVariant = WarmDividerLight,
    error = ErrorCrimsonLight
)

private val CareBridgeDarkColorScheme = darkColorScheme(
    primary = MistSagePrimaryDark,
    onPrimary = MistSageOnPrimaryDark,
    primaryContainer = MistSageContainerDark,
    onPrimaryContainer = MistSageOnContainerDark,
    secondary = SoftTerracottaSecondaryDark,
    onSecondary = SoftTerracottaOnSecondaryDark,
    secondaryContainer = SoftTerracottaContainerDark,
    onSecondaryContainer = SoftTerracottaOnContainerDark,
    tertiary = AmberTertiaryDark,
    onTertiary = AmberOnTertiaryDark,
    tertiaryContainer = AmberContainerDark,
    onTertiaryContainer = AmberOnContainerDark,
    background = ForestSlateBackgroundDark,
    onBackground = CreamInkDark,
    surface = ForestSlateBackgroundDark,
    onSurface = CreamInkDark,
    surfaceVariant = MossSurfaceDark,
    onSurfaceVariant = SageGrayDark,
    surfaceContainerLowest = ForestSlateBackgroundDark,
    surfaceContainerLow = MossSurfaceLowDark,
    surfaceContainer = MossSurfaceDark,
    surfaceContainerHigh = MossSurfaceHighDark,
    outline = SageGrayDark,
    outlineVariant = ForestDividerDark,
    error = ErrorCoralDark
)

val CareBridgeShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun CareBridgeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) CareBridgeDarkColorScheme else CareBridgeLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = CareBridgeShapes,
        content = content
    )
}
