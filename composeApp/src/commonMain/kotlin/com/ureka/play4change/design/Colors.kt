package com.ureka.play4change.design

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// U!REKA brand colours
private val UrekaViolet     = Color(0xFF6C5CB8)
private val UrekaVioletDark = Color(0xFF4A3D8F)
private val UrekaTeal       = Color(0xFF3DAA7D)
private val UrekaTealDark   = Color(0xFF277A57)

val LightColorScheme = lightColorScheme(
    primary              = UrekaViolet,
    onPrimary            = Color.White,
    primaryContainer     = Color(0xFFECE9FF),
    onPrimaryContainer   = Color(0xFF1E0F6B),
    secondary            = UrekaTeal,
    onSecondary          = Color.White,
    secondaryContainer   = Color(0xFFD4F5E7),
    onSecondaryContainer = Color(0xFF00391F),
    tertiary             = Color(0xFFBA7517),
    onTertiary           = Color.White,
    tertiaryContainer    = Color(0xFFFFDEA3),
    onTertiaryContainer  = Color(0xFF3D2000),
    background           = Color(0xFFFBFAFF),
    onBackground         = Color(0xFF1B1A22),
    surface              = Color(0xFFFBFAFF),
    onSurface            = Color(0xFF1B1A22),
    surfaceVariant       = Color(0xFFE8E5F5),
    onSurfaceVariant     = Color(0xFF49454F),
    error                = Color(0xFFB3261E),
    outline              = Color(0xFF79747E),
)

val DarkColorScheme = darkColorScheme(
    primary              = Color(0xFFCFC5FF),
    onPrimary            = Color(0xFF2D1D8C),
    primaryContainer     = Color(0xFF4A3D8F),
    onPrimaryContainer   = Color(0xFFECE9FF),
    secondary            = Color(0xFF6EDBA8),
    onSecondary          = Color(0xFF00391F),
    secondaryContainer   = Color(0xFF277A57),
    onSecondaryContainer = Color(0xFFD4F5E7),
    tertiary             = Color(0xFFFAC775),
    onTertiary           = Color(0xFF3D2000),
    tertiaryContainer    = Color(0xFF854F0B),
    onTertiaryContainer  = Color(0xFFFFDEA3),
    background           = Color(0xFF12111A),
    onBackground         = Color(0xFFE5E2F0),
    surface              = Color(0xFF12111A),
    onSurface            = Color(0xFFE5E2F0),
    surfaceVariant       = Color(0xFF49454F),
    onSurfaceVariant     = Color(0xFFCAC4D0),
    error                = Color(0xFFF2B8B5),
    outline              = Color(0xFF938F99),
)
