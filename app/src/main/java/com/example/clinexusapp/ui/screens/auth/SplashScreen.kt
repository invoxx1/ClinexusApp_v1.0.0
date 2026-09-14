package com.example.clinexusapp.ui.screens.auth

import android.graphics.ImageDecoder
import android.graphics.drawable.Animatable2
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.clinexusapp.R
import com.example.clinexusapp.util.SessionManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    animationReady: Boolean = true,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit,
) {
    val context = LocalContext.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val resources = remember(context, configuration) {
        context.createConfigurationContext(configuration).resources
    }
    val logo = remember(resources) {
        ImageDecoder.decodeDrawable(
            ImageDecoder.createSource(resources, R.raw.launch_logo_animation)
        ) as AnimatedImageDrawable
    }
    val animationFinished = remember(logo) { CompletableDeferred<Unit>() }
    val slide = remember { Animatable(0f) }
    val entrance = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val taglineAlpha = remember { Animatable(0f) }
    val atmosphere = remember { Animatable(0f) }
    val departure = remember { Animatable(0f) }
    val silkEase = remember { CubicBezierEasing(0.22f, 1f, 0.36f, 1f) }
    val navigateHome by rememberUpdatedState(onNavigateToHome)
    val navigateOnboarding by rememberUpdatedState(onNavigateToOnboarding)
    val navigateLogin by rememberUpdatedState(onNavigateToLogin)

    DisposableEffect(logo) {
        val callback = object : Animatable2.AnimationCallback() {
            override fun onAnimationEnd(drawable: Drawable?) {
                animationFinished.complete(Unit)
            }
        }
        logo.repeatCount = 0
        logo.registerAnimationCallback(callback)
        onDispose {
            logo.unregisterAnimationCallback(callback)
            logo.stop()
        }
    }

    LaunchedEffect(animationReady, logo) {
        if (!animationReady) return@LaunchedEffect
        logo.start()
        coroutineScope {
            launch { entrance.animateTo(1f, tween(700, easing = silkEase)) }
            launch { atmosphere.animateTo(1f, tween(1_200, easing = silkEase)) }
        }
        animationFinished.await()
        delay(160)
        // Finish the logo move before introducing the name and tagline.
        slide.animateTo(1f, tween(950, easing = silkEase))
        coroutineScope {
            launch { textAlpha.animateTo(1f, tween(800, easing = silkEase)) }
            launch {
                delay(200)
                taglineAlpha.animateTo(1f, tween(700, easing = silkEase))
            }
            launch { atmosphere.animateTo(0.55f, tween(1_100, easing = FastOutSlowInEasing)) }
        }
        delay(650)
        SessionManager.isInitialized.first { it }
        departure.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
        when {
            SessionManager.isLoggedIn -> navigateHome()
            SessionManager.onboardingCompleted.value -> navigateLogin()
            else -> navigateOnboarding()
        }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(
                    colorResource(R.color.logo_background),
                    Color(0xFF001B42),
                    colorResource(R.color.logo_background),
                )
            )
        ).drawBehind {
            // Broad, feathered light keeps the backdrop quiet without rings or lines.
            val lightCenter = Offset(
                size.width * (0.5f - 0.18f * slide.value),
                size.height * 0.5f,
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF168AC2).copy(alpha = 0.18f * atmosphere.value),
                        Color(0xFF086298).copy(alpha = 0.06f * atmosphere.value),
                        Color.Transparent,
                    ),
                    center = lightCenter,
                    radius = size.width * 0.8f,
                ),
                alpha = 1f - departure.value,
            )
        },
        contentAlignment = Alignment.Center,
    ) {
        val brandWidth = minOf(340.dp, maxWidth - 32.dp)
        val logoSize = brandWidth * 0.30f
        val textWidth = brandWidth * 0.52f
        val logoTextGap = 2.dp

        val logoTravel = (textWidth + logoTextGap) / 2
        val textOffset = (logoSize + logoTextGap) / 2

        AndroidView(
            factory = { imageContext ->
                ImageView(imageContext).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    contentDescription = "CliNexus tooth logo"
                    setImageDrawable(logo)
                }
            },
            modifier = Modifier
                .size(logoSize)
                .graphicsLayer {
                    translationX = (-logoTravel * slide.value).toPx()
                    alpha = entrance.value * (1f - departure.value)
                    scaleX = (1.52f - 0.52f * slide.value) *
                        (0.94f + 0.06f * entrance.value)
                    scaleY = scaleX
                    translationY = 6.dp.toPx() * (1f - entrance.value) -
                        4.dp.toPx() * departure.value
                },
        )
        Column(
            modifier = Modifier
                .offset(x = textOffset)
                .width(textWidth)
                .graphicsLayer {
                    alpha = 1f - departure.value
                    translationY = -4.dp.toPx() * departure.value
                },
        ) {
            Text(
                text = "CliNexus",
                modifier = Modifier.graphicsLayer {
                    alpha = textAlpha.value
                    translationX = 12.dp.toPx() * (1f - textAlpha.value)
                    translationY = 5.dp.toPx() * (1f - textAlpha.value)
                },
                color = Color.White,
                fontSize = (brandWidth.value * 0.125f).sp,
                lineHeight = (brandWidth.value * 0.15f).sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.8).sp,
            )
            Text(
                text = "TRUSTED DENTAL CARE",
                modifier = Modifier.graphicsLayer {
                    alpha = taglineAlpha.value
                    translationY = 8.dp.toPx() * (1f - taglineAlpha.value)
                },
                color = Color.White.copy(alpha = 0.76f),
                fontSize = (brandWidth.value * 0.031f).sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
            )
        }
    }
}
