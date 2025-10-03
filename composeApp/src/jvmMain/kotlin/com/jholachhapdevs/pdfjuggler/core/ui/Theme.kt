package com.jholachhapdevs.pdfjuggler.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class ExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val info: Color,
    val onInfo: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
    val disabledContent: Color,
    val disabledBackground: Color,
    val dividerSoft: Color,
    val focusGlow: Color,
    val ripple: Color,
    val selection: Color,
    val shadow: Color,
    val scrim: Color
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        success = SuccessGreen,
        onSuccess = OnSuccessGreen,
        successContainer = SuccessGreenContainer,
        onSuccessContainer = OnSuccessGreenContainer,
        warning = WarningAmber,
        onWarning = OnWarningAmber,
        warningContainer = WarningAmberContainer,
        onWarningContainer = OnWarningAmberContainer,
        info = InfoBlue,
        onInfo = OnInfoBlue,
        infoContainer = InfoBlueContainer,
        onInfoContainer = OnInfoBlueContainer,
        disabledContent = DisabledContent,
        disabledBackground = DisabledBackground,
        dividerSoft = DividerSoft,
        focusGlow = FocusGlow,
        ripple = RippleColor,
        selection = SelectionColor,
        shadow = ShadowColor,
        scrim = ScrimColor
    )
}

private val PdfShapes = Shapes()

@Composable
fun PdfJugglerTheme(
    content: @Composable () -> Unit
) {
    val extended = LocalExtendedColors.current
    MaterialTheme(
        colorScheme = AppDarkColorScheme,
        typography = PdfJugglerTypography(),
        shapes = PdfShapes
    ) {
        CompositionLocalProvider(
            LocalExtendedColors provides extended
        ) {
            content()
        }
    }
}

val MaterialTheme.extendedColors: ExtendedColors
    @Composable get() = LocalExtendedColors.current