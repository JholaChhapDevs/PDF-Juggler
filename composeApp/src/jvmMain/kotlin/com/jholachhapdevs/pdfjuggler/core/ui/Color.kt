package com.jholachhapdevs.pdfjuggler.core.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

// --- Core Palette (Existing) ---
val DarkMaroon = Color(0xFF330000)
val PaleCream = Color(0xFFFFCC99)

val PrimaryGold = Color(0xFFCC9900)
val OnPrimaryGold = Color(0xFF330000)
val PrimaryGoldContainer = Color(0xFF664D00)
val OnPrimaryGoldContainer = Color(0xFFFFCC99)

val MutedYellow = Color(0xFFDDDD99)
val OnMutedYellow = Color(0xFF333333)
val MutedYellowContainer = Color(0xFF66664D)
val OnMutedYellowContainer = Color(0xFFFFCC99)

val TertiaryMaroon = Color(0xFF993333)
val OnTertiaryMaroon = Color(0xFFFFCC99)

val BackgroundMaroon = Color(0xFF330000)
val OnBackgroundMaroon = Color(0xFFFFCC99)
val SurfaceMaroon = Color(0xFF401A1A)
val OnSurfaceMaroon = Color(0xFFFFCC99)
val SurfaceVariantMaroon = Color(0xFF593333)
val OnSurfaceVariantMaroon = Color(0xFFDDDD99)

val OutlineMaroon = Color(0xFF804D4D)

val ErrorRed = Color(0xFFFF4444)
val OnErrorRed = Color(0xFF330000)
val ErrorRedContainer = Color(0xFF660000)
val OnErrorRedContainer = Color(0xFFFFCC99)

// --- Added / Derived Tokens ---
val TertiaryMaroonContainer = Color(0xFF4D1919)
val OnTertiaryMaroonContainer = PaleCream

val OutlineVariantMaroon = Color(0xFF996666)
val InverseSurfaceMaroon = PaleCream
val InverseOnSurfaceMaroon = DarkMaroon
val InversePrimaryGold = Color(0xFFFFCC33)
val SurfaceTintGold = PrimaryGold
val ScrimColor = Color(0x66000000)

// --- Semantic (Status) Colors ---
val SuccessGreen = Color(0xFF4CAF50)
val OnSuccessGreen = Color(0xFF102612)
val SuccessGreenContainer = Color(0xFF1B5E20)
val OnSuccessGreenContainer = PaleCream

val WarningAmber = Color(0xFFFFB300)
val OnWarningAmber = Color(0xFF331F00)
val WarningAmberContainer = Color(0xFF664000)
val OnWarningAmberContainer = PaleCream

val InfoBlue = Color(0xFF42A5F5)
val OnInfoBlue = Color(0xFF002B47)
val InfoBlueContainer = Color(0xFF0D47A1)
val OnInfoBlueContainer = PaleCream

// --- Utility / States ---
val DisabledContent = Color(0xFFAA8888)
val DisabledBackground = Color(0xFF3D1F1F)
val DividerSoft = OutlineMaroon.copy(alpha = 0.50f)
val FocusGlow = PrimaryGold.copy(alpha = 0.30f)
val RippleColor = PrimaryGold.copy(alpha = 0.16f)
val SelectionColor = PrimaryGold.copy(alpha = 0.25f)
val ShadowColor = Color(0xFF000000)

// --- Neutrals / Basics ---
val Black = Color(0xFF000000)
val White = Color(0xFFFFFFFF)
val Transparent = Color(0x00000000)

// --- Material 3 Dark Color Scheme ---
val AppDarkColorScheme = darkColorScheme(
    primary = PrimaryGold,
    onPrimary = OnPrimaryGold,
    primaryContainer = PrimaryGoldContainer,
    onPrimaryContainer = OnPrimaryGoldContainer,
    secondary = MutedYellow,
    onSecondary = OnMutedYellow,
    secondaryContainer = MutedYellowContainer,
    onSecondaryContainer = OnMutedYellowContainer,
    tertiary = TertiaryMaroon,
    onTertiary = OnTertiaryMaroon,
    tertiaryContainer = TertiaryMaroonContainer,
    onTertiaryContainer = OnTertiaryMaroonContainer,
    background = BackgroundMaroon,
    onBackground = OnBackgroundMaroon,
    surface = SurfaceMaroon,
    onSurface = OnSurfaceMaroon,
    surfaceVariant = SurfaceVariantMaroon,
    onSurfaceVariant = OnSurfaceVariantMaroon,
    inverseSurface = InverseSurfaceMaroon,
    inverseOnSurface = InverseOnSurfaceMaroon,
    inversePrimary = InversePrimaryGold,
    outline = OutlineMaroon,
    outlineVariant = OutlineVariantMaroon,
    error = ErrorRed,
    onError = OnErrorRed,
    errorContainer = ErrorRedContainer,
    onErrorContainer = OnErrorRedContainer,
    scrim = ScrimColor,
    surfaceTint = SurfaceTintGold
)