package com.example.clinexusapp.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

enum class WalkthroughTarget {
    APPOINTMENT, PROMOTIONS, CLINIC_NEWS, APPOINTMENT_STATUSES, BOOK_APPOINTMENT, MESSAGES,
}

data class WalkthroughStep(val target: WalkthroughTarget, val title: String, val description: String)

val dashboardWalkthroughSteps = listOf(
    WalkthroughStep(WalkthroughTarget.APPOINTMENT, "Upcoming appointment", "Check your next visit and open its complete details."),
    WalkthroughStep(WalkthroughTarget.PROMOTIONS, "Promotions", "Swipe through current clinic offers and book directly."),
    WalkthroughStep(WalkthroughTarget.CLINIC_NEWS, "Clinic news", "See clinic hours and important announcements."),
    WalkthroughStep(WalkthroughTarget.APPOINTMENT_STATUSES, "Appointment statuses", "Pending awaits approval, Confirmed is scheduled, Completed is finished, and Cancelled will not proceed. Purple reschedule and red cancellation labels mean a request is being processed."),
    WalkthroughStep(WalkthroughTarget.BOOK_APPOINTMENT, "Book an appointment", "Tap this calendar button whenever you are ready to schedule a dental visit."),
    WalkthroughStep(WalkthroughTarget.MESSAGES, "Messages", "Contact the clinic, review conversations, and start a new message here."),
)

@Composable
fun AppWalkthroughOverlay(
    stepIndex: Int,
    targetBounds: Rect,
    transitioning: Boolean,
    animationsEnabled: Boolean = true,
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    val step = dashboardWalkthroughSteps[stepIndex]

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .clickable(enabled = !transitioning, onClick = onNext),
    ) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val screenHeightPx = with(density) { maxHeight.toPx() }
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val marginPx = with(density) { 8.dp.toPx() }

        // INSTANT BOUNDS: Tinanggal ang layout animation.
        // Kapag nagbago ang size dahil nawala na ang gray skeleton loader, instant na susunod ang spotlight nang walang glitch.
        val padded = Rect(
            left = (targetBounds.left - marginPx).coerceAtLeast(marginPx),
            top = (targetBounds.top - marginPx).coerceAtLeast(marginPx),
            right = (targetBounds.right + marginPx).coerceAtMost(screenWidthPx - marginPx),
            bottom = (targetBounds.bottom + marginPx).coerceAtMost(screenHeightPx - marginPx),
        )

        val cardWidth = maxWidth - 32.dp
        val cardHeightPx = with(density) { 166.dp.toPx() }
        val showBelow = step.target == WalkthroughTarget.MESSAGES || padded.bottom + cardHeightPx + marginPx < screenHeightPx
        val cardYPx = if (showBelow) padded.bottom + marginPx else (padded.top - cardHeightPx - marginPx).coerceAtLeast(marginPx)

        val pulseTransition = rememberInfiniteTransition(label = "spotlightPulse")
        val animatedAlphaState = pulseTransition.animateFloat(
            initialValue = 0.55f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(900, easing = EaseInOut), RepeatMode.Reverse),
            label = "spotlightBorder",
        )

        val spotlightRevealState = animateFloatAsState(
            targetValue = if (transitioning) 0f else 1f,
            animationSpec = tween(if (!animationsEnabled) 0 else if (transitioning) 130 else 260, easing = FastOutSlowInEasing),
            label = "spotlightReveal",
        )

        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
        ) {
            val currentReveal = spotlightRevealState.value
            val currentBorderAlpha = if (animationsEnabled) animatedAlphaState.value else 1f

            drawRect(Color(0xB800102A))
            drawRoundRect(
                color = Color.White.copy(alpha = currentReveal),
                topLeft = padded.topLeft,
                size = padded.size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()),
                blendMode = BlendMode.DstOut,
            )
            drawRoundRect(
                color = Color(0xFF77A8FF).copy(alpha = currentBorderAlpha * currentReveal),
                topLeft = padded.topLeft,
                size = padded.size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()),
                style = androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx()),
            )
        }

        AnimatedContent(
            targetState = stepIndex,
            modifier = Modifier
                .width(cardWidth)
                .offset {
                    IntOffset(with(density) { 16.dp.roundToPx() }, cardYPx.roundToInt())
                }
                .graphicsLayer {
                    alpha = spotlightRevealState.value
                },
            transitionSpec = {
                // Mabilis na crossfade imbes na nag-i-slide in/out habang naglo-load ang tab
                fadeIn(tween(150)) togetherWith fadeOut(tween(150))
            },
            label = "walkthroughContent",
        ) {
            val animatedStep = dashboardWalkthroughSteps[it]
            Surface(shape = RoundedCornerShape(22.dp), color = Color.White, shadowElevation = 14.dp) {
                Column(Modifier.padding(18.dp)) {
                    Text(animatedStep.title, color = Color(0xFF080B36), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(5.dp))
                    Text(animatedStep.description, color = Color(0xFF667085), fontSize = 13.sp, lineHeight = 18.sp)
                    Spacer(Modifier.height(13.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        dashboardWalkthroughSteps.indices.forEach { index ->
                            val width by animateDpAsState(if (index == stepIndex) 20.dp else 6.dp, tween(if (animationsEnabled) 250 else 0), label = "progress")
                            Box(Modifier.width(width).height(6.dp).clip(CircleShape).background(if (index == stepIndex) Color(0xFF1F3A6D) else Color(0xFFD6DEEC)))
                        }
                    }
                }
            }
        }

        Text(
            "Skip",
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 18.dp, bottom = 16.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.96f))
                .clickable(onClick = onSkip)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            color = Color(0xFF526078), fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
        )

        Button(
            onClick = onNext,
            enabled = !transitioning,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 18.dp, bottom = 12.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F3A6D)),
        ) {
            Text(if (stepIndex == dashboardWalkthroughSteps.lastIndex) "Done" else "Next")
        }
    }
}