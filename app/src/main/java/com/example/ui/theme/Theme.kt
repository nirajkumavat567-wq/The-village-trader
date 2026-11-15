package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class VillageSemanticColors(
    val profit: Color,
    val profitContainer: Color,
    val loss: Color,
    val lossContainer: Color,
    val gold: Color,
    val goldContainer: Color,
    val liveAccent: Color,
    val cardBorder: Color,
    val elevatedSurface: Color
)

val LocalVillageColors = staticCompositionLocalOf {
    VillageSemanticColors(
        profit = EmeraldBull,
        profitContainer = EmeraldDarkContainer,
        loss = CrimsonBear,
        lossContainer = CrimsonDarkContainer,
        gold = VillageGold,
        goldContainer = GoldDarkContainer,
        liveAccent = CyanLive,
        cardBorder = CardStrokeDark,
        elevatedSurface = ObsidianElevated
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldBull,
    onPrimary = Color(0xFF002114),
    primaryContainer = EmeraldDarkContainer,
    onPrimaryContainer = EmeraldOnContainer,
    secondary = VillageGold,
    onSecondary = Color(0xFF281A00),
    secondaryContainer = GoldDarkContainer,
    onSecondaryContainer = GoldOnContainer,
    tertiary = CyanLive,
    onTertiary = Color(0xFF001E2B),
    background = ObsidianBg,
    onBackground = TextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ObsidianElevated,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerHigh = ObsidianContainerHigh,
    outline = CardStrokeDark,
    error = CrimsonBear,
    onError = Color(0xFF33000B),
    errorContainer = CrimsonDarkContainer,
    onErrorContainer = CrimsonOnContainer
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD5F9EA),
    onPrimaryContainer = Color(0xFF003823),
    secondary = GoldLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFEFC7),
    onSecondaryContainer = Color(0xFF3B2400),
    tertiary = Color(0xFF0284C7),
    onTertiary = Color.White,
    background = LightBg,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightElevated,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerHigh = Color(0xFFE2E8F0),
    outline = CardStrokeLight,
    error = CrimsonLight,
    onError = Color.White,
    errorContainer = Color(0xFFFFDCE2),
    onErrorContainer = Color(0xFF4C0011)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun VillageTradersTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val semanticColors = if (darkTheme) {
        VillageSemanticColors(
            profit = EmeraldBull,
            profitContainer = EmeraldDarkContainer,
            loss = CrimsonBear,
            lossContainer = CrimsonDarkContainer,
            gold = VillageGold,
            goldContainer = GoldDarkContainer,
            liveAccent = CyanLive,
            cardBorder = CardStrokeDark,
            elevatedSurface = ObsidianElevated
        )
    } else {
        VillageSemanticColors(
            profit = EmeraldLight,
            profitContainer = Color(0xFFD5F9EA),
            loss = CrimsonLight,
            lossContainer = Color(0xFFFFDCE2),
            gold = GoldLight,
            goldContainer = Color(0xFFFFEFC7),
            liveAccent = Color(0xFF0284C7),
            cardBorder = CardStrokeLight,
            elevatedSurface = LightElevated
        )
    }

    CompositionLocalProvider(LocalVillageColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
