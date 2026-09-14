package com.example.clinexusapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

enum class WalkthroughTarget { APPOINTMENT, PROMOTIONS, CLINIC_NEWS, APPOINTMENT_STATUSES, BOOK_APPOINTMENT, MESSAGES }
data class WalkthroughStep(val target: WalkthroughTarget, val title: String, val description: String)

val appWalkthroughSteps = listOf(
    WalkthroughStep(WalkthroughTarget.APPOINTMENT, "Upcoming appointment", "Check your next visit and open its complete details."),
    WalkthroughStep(WalkthroughTarget.PROMOTIONS, "Promotions", "Swipe through current clinic offers and book directly."),
    WalkthroughStep(WalkthroughTarget.CLINIC_NEWS, "Clinic news", "See clinic hours and important announcements."),
    WalkthroughStep(WalkthroughTarget.APPOINTMENT_STATUSES, "Appointment statuses", "Track pending, confirmed, completed, cancelled, reschedule, and cancellation requests here."),
    WalkthroughStep(WalkthroughTarget.BOOK_APPOINTMENT, "Book an appointment", "Tap the calendar button to schedule a new dental visit."),
    WalkthroughStep(WalkthroughTarget.MESSAGES, "Messages", "Contact the clinic, review conversations, and start a new message."),
)

@Composable
fun AppWalkthroughOverlay(stepIndex: Int, target: Rect, onNext: () -> Unit, onSkip: () -> Unit) {
    val step = appWalkthroughSteps[stepIndex]
    BoxWithConstraints(Modifier.fillMaxSize().clickable(onClick = onNext)) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val margin = with(density) { 8.dp.toPx() }
        val screenW = with(density) { maxWidth.toPx() }
        val screenH = with(density) { maxHeight.toPx() }
        val hole = Rect((target.left-margin).coerceAtLeast(margin), (target.top-margin).coerceAtLeast(margin), (target.right+margin).coerceAtMost(screenW-margin), (target.bottom+margin).coerceAtMost(screenH-margin))
        Canvas(Modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
            drawRect(Color(0xC400102A))
            drawRoundRect(Color.White, hole.topLeft, hole.size, CornerRadius(18.dp.toPx()), blendMode=BlendMode.DstOut)
            drawRoundRect(Color(0xFF72A7FF), hole.topLeft, hole.size, CornerRadius(18.dp.toPx()), style=Stroke(2.dp.toPx()))
        }
        val cardHeight = with(density) { 145.dp.toPx() }
        val below = step.target == WalkthroughTarget.MESSAGES || hole.bottom + cardHeight + margin < screenH
        val y = if (below) hole.bottom + margin else (hole.top-cardHeight-margin).coerceAtLeast(margin)
        Surface(Modifier.width(maxWidth-32.dp).offset { IntOffset(with(density){16.dp.roundToPx()}, y.roundToInt()) }, RoundedCornerShape(22.dp), Color.White, shadowElevation=12.dp) {
            Column(Modifier.padding(18.dp)) {
                Text(step.title, color=Color(0xFF080B36), fontSize=18.sp, fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(5.dp)); Text(step.description, color=Color(0xFF667085), fontSize=13.sp, lineHeight=18.sp)
                Spacer(Modifier.height(12.dp)); Row(horizontalArrangement=Arrangement.spacedBy(5.dp)) { appWalkthroughSteps.indices.forEach { i -> Box(Modifier.width(if(i==stepIndex) 20.dp else 6.dp).height(6.dp).clip(CircleShape).background(if(i==stepIndex) Color(0xFF1F3A6D) else Color(0xFFD6DEEC))) } }
            }
        }
        Text("Skip", Modifier.align(Alignment.BottomStart).navigationBarsPadding().padding(18.dp).clip(CircleShape).background(Color.White).clickable(onClick=onSkip).padding(horizontal=20.dp, vertical=12.dp), color=Color(0xFF526078), fontWeight=FontWeight.SemiBold)
        Button(onClick=onNext, modifier=Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(14.dp), shape=CircleShape, colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF1F3A6D))) { Text(if(stepIndex==appWalkthroughSteps.lastIndex) "Done" else "Next") }
    }
}
