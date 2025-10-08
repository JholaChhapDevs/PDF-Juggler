// File: `Font.kt`
package com.jholachhapdevs.pdfjuggler.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font
import pdf_juggler.composeapp.generated.resources.Res
import pdf_juggler.composeapp.generated.resources.Chord_Brights
import pdf_juggler.composeapp.generated.resources.Bogue_Semibold_Regular


val ChordBright: FontFamily
    @Composable
    get() = FontFamily(
        Font(
            Res.font.Chord_Brights,
            weight = FontWeight.Normal,
            style = FontStyle.Normal
        )
    )

val BogueSemibold: FontFamily
    @Composable
    get() = FontFamily(
        Font(
            Res.font.Bogue_Semibold_Regular,
            weight = FontWeight.SemiBold,
            style = FontStyle.Normal
        )
    )