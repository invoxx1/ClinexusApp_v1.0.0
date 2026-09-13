package com.example.clinexusapp.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Navy Blue Palette (legacy names retained for existing screens)
val DeepTeal = Color(0xFF1F3A6D)
val VibrantTeal = Color(0xFF1F3A6D)
val MintSparkle = Color(0xFFE8EEF8)
val SoftMist = Color(0xFFF5F7FB)
val RoyalNavy = Color(0xFF172B4D)
val PureWhite = Color(0xFFFFFFFF)

// Additional UI Tones
val TealMuted = Color(0xFF1F3A6D)
val SlateGray = Color(0xFF526078)
val LightSlate = Color(0xFF94A3B8)
val ErrorRed = Color(0xFFEF4444)
val DarkRed = Color(0xFF8B0000)

// Standard Colors (mapped for compatibility)
val BluePrimary = DeepTeal
val BlueSecondary = VibrantTeal
val BlueDark = RoyalNavy
val White = PureWhite
val Black = RoyalNavy
val GrayDark = SlateGray
val GrayMedium = Color(0xFFD8E1EF)

// Premium Gradients
val WavyTealGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF1F3A6D), // Soft Navy
        VibrantTeal,       // Primary Navy
        DeepTeal           // Primary Navy
    )
)

val ActionButtonGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF1F3A6D), // Soft Navy
        VibrantTeal        // Primary Navy
    )
)

// Wave Translucency
val WaveLayerTop = Color(0xFFFFFFFF).copy(alpha = 0.15f)
val WaveLayerMid = Color(0xFFE8EEF8).copy(alpha = 0.25f)
val WaveLayerBase = VibrantTeal.copy(alpha = 0.35f)

val SoftMintGradient = Brush.verticalGradient(
    colors = listOf(PureWhite, SoftMist)
)

val PremiumBlueGradient = WavyTealGradient
val BlueExtraLight = MintSparkle
val LuminousAzureGradient = WavyTealGradient
