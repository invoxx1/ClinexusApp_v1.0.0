package com.example.clinexusapp.ui.screens.dashboard

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.clinexusapp.model.AppointmentDTO
import com.example.clinexusapp.util.DateUtils
import com.example.clinexusapp.viewmodel.AppointmentStatus
import com.example.clinexusapp.viewmodel.mapAppointmentStatus

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UpcomingAppointmentCard(
    appointment: AppointmentDTO,
    onDetailsClick: () -> Unit
) {
    DashboardCard {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val expandedLayout = maxWidth < 330.dp || LocalDensity.current.fontScale > 1.15f
            Row(
                modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                DashboardCardIcon(Icons.Default.EventNote, DashboardStyle.Mint, DashboardStyle.Teal)
                Column(Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(Modifier.weight(1f)) {
                            DashboardCardTitle(appointment.serviceName ?: appointment.treatment)
                            Spacer(Modifier.height(4.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                AppointmentDetail(
                                    Icons.Default.CalendarMonth,
                                    DateUtils.formatDisplayDate(appointment.appointmentDate)
                                )
                                AppointmentDetail(
                                    Icons.Default.Schedule,
                                    DateUtils.formatDisplayTime(appointment.startTime)
                                )
                            }
                        }
                        if (!expandedLayout) AppointmentClock()
                    }
                    if (expandedLayout) {
                        Spacer(Modifier.height(8.dp))
                        AppointmentStatusPill(appointment.appointmentStatus)
                        DashboardPillButton(
                            text = "View Details",
                            onClick = onDetailsClick,
                            modifier = Modifier.align(Alignment.End)
                        )
                    } else {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Box(Modifier.heightIn(min = 48.dp), contentAlignment = Alignment.CenterStart) {
                                AppointmentStatusPill(appointment.appointmentStatus)
                            }
                            DashboardPillButton("View Details", onDetailsClick)
                        }
                    }
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
            DashboardCardIcon(Icons.Default.EventNote, DashboardStyle.Mint, DashboardStyle.Teal)
            Column(Modifier.weight(1f)) {
                DashboardCardTitle("No upcoming appointment")
                DashboardPillButton("Book appointment", onBookClick)
            }
        }
    }
}

@Composable
fun PromotionCard(title: String, value: String, description: String) {
    DashboardCard {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.Top
        ) {
            DashboardCardIcon(Icons.Default.LocalOffer, Color(0xFFFFEDDF), DashboardStyle.Orange)
            Column(Modifier.weight(1f)) {
                DashboardCardTitle(title)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = value,
                    color = DashboardStyle.Orange,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                if (description.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    DashboardCardDescription(description)
                }
            }
        }
    }
}

@Composable
fun InsightCard(title: String, subtitle: String, category: String, onClick: () -> Unit) {
    DashboardCard(Modifier.clickable(role = Role.Button, onClick = onClick)) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DashboardCardIcon(Icons.Default.AutoFixHigh, Color(0xFFFFE9EA), Color(0xFFFF6468))
            Column(Modifier.weight(1f)) {
                DashboardCardTitle(title)
                if (category.isNotBlank()) {
                    Text(category, color = Color(0xFFFF6468), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                if (subtitle.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    DashboardCardDescription(subtitle)
                }
            }
        }
    }
}

@Composable
fun NewsCard(title: String, description: String, date: String) {
    DashboardCard {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DashboardCardIcon(Icons.Default.Business, Color(0xFFE5F3FF), Color(0xFF70AFD0))
            Column(Modifier.weight(1f)) {
                DashboardCardTitle(title)
                if (date.isNotBlank()) {
                    Text(date, color = DashboardStyle.Teal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                if (description.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    DashboardCardDescription(description)
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
private fun AppointmentStatusPill(rawStatus: String) {
    val status = mapAppointmentStatus(rawStatus)
    val (label, color) = when (status) {
        AppointmentStatus.PENDING -> "PENDING" to Color(0xFFD97706)
        AppointmentStatus.CONFIRMED -> "CONFIRMED" to Color(0xFF008A13)
        AppointmentStatus.RESCHEDULE_REQUESTED -> "RESCHEDULE " to Color(0xFF7C3AED)
        AppointmentStatus.CANCELLATION_REQUESTED -> "CANCELLATION " to Color(0xFFC2415A)
        AppointmentStatus.COMPLETED -> "COMPLETED" to Color(0xFF2563EB)
        AppointmentStatus.CANCELLED -> "CANCELLED" to Color(0xFFD92D38)
        AppointmentStatus.UNKNOWN -> "UNKNOWN" to Color(0xFF687080)
    }
    Surface(
        color = if (status == AppointmentStatus.CONFIRMED) {
            Color(0xFFDEF7DE)
        } else {
            color.copy(alpha = 0.1f)
        },
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            color = color,
            fontSize = 10.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AppointmentClock() {
    Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 4.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = Color(0xFF60DDD0),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke)
            )
            drawArc(
                color = DashboardStyle.Teal,
                startAngle = 180f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke)
            )
        }
        Icon(Icons.Default.Schedule, contentDescription = null, tint = DashboardStyle.Teal, modifier = Modifier.size(24.dp))
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
                    brush = Brush.horizontalGradient(listOf(Color(0xFF00A298), Color(0xFF00B5A6))),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(horizontal = 16.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}
