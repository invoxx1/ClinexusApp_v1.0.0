package com.example.clinexusapp.ui.screens.auth

import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Animatable2
import android.graphics.drawable.Drawable
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import android.widget.ImageView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.clinexusapp.R
import com.example.clinexusapp.util.SessionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@Composable
fun SplashScreen(animationReady: Boolean = true, onNavigateToOnboarding: () -> Unit, onNavigateToHome: () -> Unit) {
    val textEntrance = remember { Animatable(0f) }
    val logoEntrance = remember { Animatable(0f) }
    var showLogo by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var logoDrawable by remember { mutableStateOf<Drawable?>(null) }
    var logoFinished by remember { mutableStateOf(false) }
    val navigateHome by rememberUpdatedState(onNavigateToHome)
    val navigateOnboarding by rememberUpdatedState(onNavigateToOnboarding)

    LaunchedEffect(animationReady) {
        if (!animationReady) return@LaunchedEffect
        
        // Start text animation immediately without waiting for logo decoding
        launch {
            textEntrance.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
        }
        
        // Decode logo in parallel
        val decodedLogo = withContext(Dispatchers.IO) {
            runCatching {
                ImageDecoder.decodeDrawable(ImageDecoder.createSource(context.resources, R.raw.launch_logo_animation))
            }.getOrNull()
        }
        
        logoDrawable = decodedLogo
        showLogo = decodedLogo != null
        
        if (showLogo) {
            launch {
                logoEntrance.animateTo(1f, tween(450, easing = FastOutSlowInEasing))
            }
            // Wait for logo animation to finish, but with a much shorter safety timeout
            withTimeoutOrNull(2.seconds) { 
                snapshotFlow { logoFinished }.first { it } 
            }
        } else {
            // If no logo, just a brief pause for branding impact
            kotlinx.coroutines.delay(1.seconds)
        }
        
        // Ensure SessionManager is ready before proceeding
        withTimeoutOrNull(2.seconds) {
            SessionManager.isInitialized.first { it }
        }
        
        if (SessionManager.isLoggedIn) navigateHome() else navigateOnboarding()
    }
    Box(
        Modifier.fillMaxSize().background(colorResource(R.color.logo_background))
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        val logoSize = 100.dp
        // Center the complete animated group against the full screen, not the system-bar inset area.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                Modifier.width((logoSize + 16.dp) * logoEntrance.value).height(logoSize),
                contentAlignment = Alignment.CenterStart
            ) {
                if (showLogo) {
                    AndroidView(
                        factory = { context ->
                            ImageView(context).apply {
                                contentDescription = "Animated CliNexus logo"
                                scaleType = ImageView.ScaleType.FIT_CENTER
                                val image = logoDrawable
                                setImageDrawable(image)
                                if (image is AnimatedImageDrawable) {
                                    image.registerAnimationCallback(object : Animatable2.AnimationCallback() {
                                        override fun onAnimationEnd(drawable: Drawable?) { logoFinished = true }
                                    })
                                    image.repeatCount = 0
                                    image.start()
                                } else {
                                    logoFinished = true
                                }
                            }
                        },
                        modifier = Modifier.requiredSize(logoSize).graphicsLayer {
                            alpha = logoEntrance.value
                            scaleX = 0.85f + 0.15f * logoEntrance.value
                            scaleY = scaleX
                        },
                        onRelease = { view -> (view.drawable as? AnimatedImageDrawable)?.apply { stop(); clearAnimationCallbacks() } }
                    )
                }
            }
            Column(
                Modifier.graphicsLayer {
                    alpha = textEntrance.value
                    scaleX = 0.65f + 0.35f * textEntrance.value
                    scaleY = scaleX
                },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("CliNexus", color = Color.White, fontSize = 58.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = (-1).sp)
                Spacer(Modifier.height(8.dp))
                Text("TRUSTED DENTAL CARE", color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
            }
        }
    }
}