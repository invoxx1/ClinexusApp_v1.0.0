package com.example.clinexusapp.ui.screens.dashboard

import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.CalendarDays
import com.composables.icons.lucide.Clock
import com.composables.icons.lucide.Hospital
import com.composables.icons.lucide.Sparkles
import com.composables.icons.lucide.Tag


import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.clinexusapp.model.AppointmentDTO
import com.example.clinexusapp.util.DateUtils
import com.example.clinexusapp.viewmodel.AppointmentStatus
import com.example.clinexusapp.viewmodel.mapAppointmentStatus
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UpcomingAppointmentCard(
    appointment: AppointmentDTO,
    onDetailsClick: () -> Unit
) {
    DashboardCard {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 13.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DashboardCardIcon(Lucide.CalendarDays, DashboardStyle.Mint, DashboardStyle.Teal)
                    Column(Modifier.weight(1f)) {
                        DashboardCardTitle(appointment.serviceName ?: appointment.treatment)
                        Spacer(Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            AppointmentDetail(Lucide.CalendarDays, DateUtils.formatDisplayDate(appointment.appointmentDate))
                            AppointmentDetail(Lucide.Clock, DateUtils.formatDisplayTime(appointment.startTime))
                        }
                    }
                    AppointmentClock()
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth().padding(start = 70.dp), verticalAlignment = Alignment.CenterVertically) {
                    AppointmentStatusField(appointment.appointmentStatus, Modifier.weight(1f))
                    DashboardPillButton(
                        text = "View Details",
                        onClick = onDetailsClick,
                        modifier = Modifier.widthIn(min = 116.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyAppointmentCard(onBookClick: () -> Unit) {
    DashboardCard {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DashboardCardIcon(Lucide.CalendarDays, DashboardStyle.Mint, DashboardStyle.Teal)
            Column(Modifier.weight(1f)) {
                DashboardCardTitle("No upcoming appointment")
                DashboardPillButton("Book appointment", onBookClick)
            }
        }
    }
}

@Composable
fun PromotionCard(
    title: String,
    value: String,
    description: String,
    onBookClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.shadow(4.dp, DashboardStyle.CardShape),
        shape = DashboardStyle.CardShape,
        color = Color(0xFFEAF5FF),
    ) {
        Box(Modifier.background(Brush.horizontalGradient(listOf(Color(0xFFF3F9FF), Color(0xFFDDEEFF))))) {
            Column(Modifier.fillMaxSize().padding(start = 14.dp, top = 12.dp, end = 48.dp, bottom = 10.dp)) {
                Text(title, color = DashboardStyle.Navy, fontSize = 14.sp, lineHeight = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(value, color = Color(0xFF1769C2), fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                if (description.isNotBlank()) {
                    Text(description, color = Color(0xFF667085), fontSize = 10.sp, lineHeight = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.weight(1f))
                DashboardPillButton("Book Now", onBookClick, Modifier.widthIn(min = 108.dp))
            }
            Text(
                "✦",
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 22.dp),
                color = Color(0xFF62A7EC),
                fontSize = 32.sp,
            )
        }
    }
}

@Composable
fun InsightCard(title: String, subtitle: String, category: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(112.dp).shadow(4.dp, DashboardStyle.CardShape),
        shape = DashboardStyle.CardShape,
        color = Color(0xFFE9FAF5),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DashboardCardIcon(Lucide.Sparkles, Color(0xFFD8F5EC), Color(0xFF1769D2))
            Column(Modifier.weight(1f)) {
                Text(title, color = DashboardStyle.Navy, fontSize = 14.sp, lineHeight = 17.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (category.isNotBlank()) {
                    Text(category.uppercase(), color = Color(0xFF16853C), fontSize = 9.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
                if (subtitle.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(subtitle, color = DashboardStyle.Muted, fontSize = 11.sp, lineHeight = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Surface(onClick = onClick, shape = CircleShape, color = Color(0xFFDCEAFF)) {
                Text("Read more", Modifier.padding(horizontal = 10.dp, vertical = 8.dp), color = DashboardStyle.Teal, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun NewsCard(title: String, description: String, date: String) {
    DashboardCard(Modifier.height(108.dp)) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DashboardCardIcon(Lucide.Hospital, Color(0xFFE5F3FF), Color(0xFF2479CE))
            Column(Modifier.weight(1f)) {
                Text(title, color = DashboardStyle.Navy, fontSize = 14.sp, lineHeight = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (date.isNotBlank()) {
                    Text(date, color = DashboardStyle.Teal, fontSize = 10.sp, lineHeight = 13.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                if (description.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(description, color = DashboardStyle.Muted, fontSize = 10.sp, lineHeight = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Surface(shape = CircleShape, color = Color(0xFFE1F8E8)) {
                Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).background(Color(0xFF16A53A), CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Text("OPEN TODAY", color = Color(0xFF087C29), fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
    }
}

@Composable
internal fun DashboardCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = DashboardStyle.CardShape,
                ambientColor = DashboardStyle.Teal.copy(alpha = 0.06f),
                spotColor = DashboardStyle.Teal.copy(alpha = 0.08f)
            )
            .clip(DashboardStyle.CardShape)
            .then(modifier),
        color = Color.White,
        shape = DashboardStyle.CardShape
    ) {
        Column(content = content)
    }
}

@Composable
private fun DashboardCardIcon(icon: ImageVector, background: Color, tint: Color) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(background, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(30.dp))
    }
}

@Composable
private fun DashboardCardTitle(text: String) {
    Text(
        text = text,
        color = DashboardStyle.Navy,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun DashboardCardDescription(text: String) {
    Text(
        text = text,
        color = DashboardStyle.Muted,
        fontSize = 13.sp,
        lineHeight = 18.5.sp
    )
}

@Composable
private fun AppointmentDetail(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = DashboardStyle.Muted, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(5.dp))
        Text(text, fontSize = 12.sp, lineHeight = 16.sp, color = DashboardStyle.Muted)
    }
}

@Composable
private fun AppointmentStatusField(rawStatus: String, modifier: Modifier = Modifier) {
    val status = mapAppointmentStatus(rawStatus)
    val (label, color) = when (status) {
        AppointmentStatus.PENDING -> "Pending" to Color(0xFFD97706)
        AppointmentStatus.CONFIRMED -> "Confirmed" to Color(0xFF008A13)
        AppointmentStatus.RESCHEDULE_REQUESTED -> "Reschedule requested" to Color(0xFF7C3AED)
        AppointmentStatus.CANCELLATION_REQUESTED -> "Cancellation requested" to Color(0xFFC2415A)
        AppointmentStatus.COMPLETED -> "Completed" to Color(0xFF2563EB)
        AppointmentStatus.CANCELLED -> "Cancelled" to Color(0xFFD92D38)
        AppointmentStatus.UNKNOWN -> "Unavailable" to Color(0xFF687080)
    }

    Column(
        modifier = modifier.semantics { contentDescription = "Appointment status: $label" },
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).background(color, CircleShape))
            Spacer(Modifier.width(5.dp))
            Text(
                text = "Status",
                color = DashboardStyle.Muted,
                fontSize = 9.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Text(
            modifier = Modifier.padding(start = 11.dp),
            text = label,
            color = color,
            fontSize = 10.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AppointmentClock() {
    Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = size.minDimension / 2f
            drawCircle(Color(0xFFDCE8FF), radius = outerRadius, style = Fill)
            drawCircle(Color.White, radius = outerRadius - 3.dp.toPx(), style = Fill)
            drawCircle(DashboardStyle.Teal, radius = outerRadius - 3.dp.toPx(), style = Stroke(2.dp.toPx()))

            repeat(12) { tick ->
                val angle = Math.toRadians((tick * 30.0) - 90.0)
                val tickOuter = outerRadius - 7.dp.toPx()
                val tickInner = tickOuter - if (tick % 3 == 0) 3.dp.toPx() else 1.5.dp.toPx()
                val start = Offset(
                    center.x + cos(angle).toFloat() * tickInner,
                    center.y + sin(angle).toFloat() * tickInner,
                )
                val end = Offset(
                    center.x + cos(angle).toFloat() * tickOuter,
                    center.y + sin(angle).toFloat() * tickOuter,
                )
                drawLine(DashboardStyle.Teal.copy(alpha = if (tick % 3 == 0) 0.9f else 0.45f), start, end, strokeWidth = 1.3.dp.toPx())
            }

            drawLine(DashboardStyle.Teal, center, Offset(center.x, center.y - 8.dp.toPx()), strokeWidth = 2.dp.toPx())
            drawLine(DashboardStyle.Teal, center, Offset(center.x + 6.dp.toPx(), center.y + 3.dp.toPx()), strokeWidth = 2.dp.toPx())
            drawCircle(Color.White, radius = 2.6.dp.toPx(), center = center)
            drawCircle(DashboardStyle.Teal, radius = 1.7.dp.toPx(), center = center)
        }
    }
}

@Composable
private fun DashboardPillButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .widthIn(min = 96.dp)
            .heightIn(min = 48.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .heightIn(min = 32.dp)
                .background(
                    brush = Brush.horizontalGradient(listOf(Color(0xFF1F3A6D), Color(0xFF1F3A6D))),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(horizontal = 16.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}
