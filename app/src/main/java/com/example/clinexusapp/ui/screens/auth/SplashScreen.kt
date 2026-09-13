package com.example.clinexusapp.ui.screens.auth

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.clinexusapp.R
import com.example.clinexusapp.util.SessionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    animationReady: Boolean = true,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToHome: () -> Unit,
) {
    val reveal = remember { Animatable(0.025f) }
    val wordmarkAlpha = remember { Animatable(0.2f) }
    val wordmarkScale = remember { Animatable(1.1f) }
    val tracking = remember { Animatable(1f) }
    val glow = remember { Animatable(0f) }
    val accent = remember { Animatable(0f) }
    val taglineAlpha = remember { Animatable(0f) }
    val navigateHome by rememberUpdatedState(onNavigateToHome)
    val navigateOnboarding by rememberUpdatedState(onNavigateToOnboarding)

    LaunchedEffect(animationReady) {
        if (!animationReady) return@LaunchedEffect

        val motions = listOf(
            launch { reveal.animateTo(1f, tween(1_150, easing = FastOutSlowInEasing)) },
            launch { wordmarkAlpha.animateTo(1f, tween(420, easing = FastOutSlowInEasing)) },
            launch { wordmarkScale.animateTo(1f, tween(1_100, easing = FastOutSlowInEasing)) },
            launch { tracking.animateTo(0f, tween(1_200, easing = FastOutSlowInEasing)) },
            launch {
                glow.animateTo(0.8f, tween(430, easing = FastOutSlowInEasing))
                glow.animateTo(0f, tween(700, easing = FastOutSlowInEasing))
            },
            launch { accent.animateTo(1f, tween(620, delayMillis = 650, easing = FastOutSlowInEasing)) },
            launch { taglineAlpha.animateTo(1f, tween(520, delayMillis = 820, easing = FastOutSlowInEasing)) },
        )
        motions.forEach { it.join() }

        SessionManager.isInitialized.first { it }
        if (SessionManager.isLoggedIn) navigateHome() else navigateOnboarding()
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
        ),
        contentAlignment = Alignment.Center,
    ) {
        val wordmarkWidth = minOf(340.dp, maxWidth - 32.dp)
        val revealModifier = Modifier.width(wordmarkWidth).drawWithContent {
            val halfReveal = size.width * reveal.value / 2f
            clipRect(
                left = size.width / 2f - halfReveal,
                right = size.width / 2f + halfReveal,
            ) {
                this@drawWithContent.drawContent()
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.width(wordmarkWidth).height(76.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "CliNexus",
                    modifier = revealModifier.blur(12.dp).graphicsLayer {
                        alpha = glow.value * 0.5f
                        scaleX = wordmarkScale.value
                        scaleY = wordmarkScale.value
                    },
                    color = Color(0xFF66C7FF),
                    fontSize = 58.sp,
                    lineHeight = 64.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (6f * tracking.value - 0.8f).sp,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "CliNexus",
                    modifier = revealModifier.graphicsLayer {
                        alpha = wordmarkAlpha.value
                        scaleX = wordmarkScale.value
                        scaleY = wordmarkScale.value
                    },
                    color = Color.White,
                    fontSize = 58.sp,
                    lineHeight = 64.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (6f * tracking.value - 0.8f).sp,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(8.dp))
            Box(
                Modifier.width(72.dp * accent.value).height(2.dp)
                    .background(Color(0xFF65D8FF), CircleShape)
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "TRUSTED DENTAL CARE",
                modifier = Modifier.graphicsLayer {
                    alpha = taglineAlpha.value
                    translationY = 8f * (1f - taglineAlpha.value)
                },
                color = Color.White.copy(alpha = 0.76f),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.1.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}
