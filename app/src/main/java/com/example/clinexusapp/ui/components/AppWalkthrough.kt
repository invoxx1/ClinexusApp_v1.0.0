package com.example.clinexusapp.ui.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

enum class WalkthroughTarget {
    APPOINTMENT,
    PROMOTIONS,
    CLINIC_NEWS,
    APPOINTMENT_STATUSES,
    BOOK_APPOINTMENT,
    MESSAGES,
}

data class WalkthroughStep(
    val target: WalkthroughTarget,
    val title: String,
    val description: String,
)

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
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    val step = dashboardWalkthroughSteps[stepIndex]
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .clickable {},
    ) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val screenHeightPx = with(density) { maxHeight.toPx() }
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val marginPx = with(density) { 8.dp.toPx() }
        val padded = Rect(
            left = (targetBounds.left - marginPx).coerceAtLeast(marginPx),
            top = (targetBounds.top - marginPx).coerceAtLeast(marginPx),
            right = (targetBounds.right + marginPx).coerceAtMost(screenWidthPx - marginPx),
            bottom = (targetBounds.bottom + marginPx).coerceAtMost(screenHeightPx - marginPx),
        )

        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
        ) {
            drawRect(Color(0xB800102A))
            drawRoundRect(
                color = Color.Transparent,
                topLeft = padded.topLeft,
                size = padded.size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()),
                blendMode = BlendMode.Clear,
            )
            drawRoundRect(
                color = Color(0xFF77A8FF),
                topLeft = padded.topLeft,
                size = padded.size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()),
                style = androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx()),
            )
        }

        val cardWidth = maxWidth - 32.dp
        val cardHeightPx = with(density) { 166.dp.toPx() }
        val showBelow = padded.bottom + cardHeightPx + marginPx < screenHeightPx
        val cardYPx = if (showBelow) padded.bottom + marginPx else (padded.top - cardHeightPx - marginPx).coerceAtLeast(marginPx)

        Surface(
            modifier = Modifier
                .width(cardWidth)
                .offset { IntOffset(with(density) { 16.dp.roundToPx() }, cardYPx.roundToInt()) },
            shape = RoundedCornerShape(22.dp),
            color = Color.White,
            shadowElevation = 12.dp,
        ) {
            Column(Modifier.padding(18.dp)) {
                Text(step.title, color = Color(0xFF080B36), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Text(step.description, color = Color(0xFF667085), fontSize = 13.sp, lineHeight = 18.sp)
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        dashboardWalkthroughSteps.indices.forEach { index ->
                            Box(
                                Modifier
                                    .width(if (index == stepIndex) 20.dp else 6.dp)
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(if (index == stepIndex) Color(0xFF1F3A6D) else Color(0xFFD6DEEC)),
                            )
                        }
                    }
                    Text(
                        "Skip",
                        modifier = Modifier.clickable(onClick = onSkip).padding(10.dp),
                        color = Color(0xFF7E87A4),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                    )
                    Button(
                        onClick = onNext,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F3A6D)),
                    ) {
                        Text(if (stepIndex == dashboardWalkthroughSteps.lastIndex) "Done" else "Next")
                    }
                }
            }
        }
    }
}
