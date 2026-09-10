package com.example.clinexusapp.ui.screens.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.clinexusapp.model.AppointmentDTO
import com.example.clinexusapp.model.PromotionDTO
import com.example.clinexusapp.ui.navigation.Screen
import com.example.clinexusapp.ui.theme.*
import com.example.clinexusapp.util.DateUtils
import com.example.clinexusapp.util.Resource
import com.example.clinexusapp.util.SessionManager
import com.example.clinexusapp.viewmodel.DashboardViewModel
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    rootNavController: NavController,
    onNotificationClick: () -> Unit,
) {
    val user by SessionManager.currentUser.collectAsState()
    val firstName = user?.firstName ?: "Patient"

    val newsState by viewModel.newsState.collectAsState()
    val insightsState by viewModel.insightsState.collectAsState()
    val promotionsState by viewModel.promotionsState.collectAsState()
    val nextApptState by viewModel.nextAppointment.collectAsState()
    val unreadCount by viewModel.unreadNotificationsCount.collectAsState()

    var showInsightDialog by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.fetchDashboardData()
    }

    if (showInsightDialog != null) {
        AlertDialog(
            onDismissRequest = { showInsightDialog = null },
            confirmButton = {
                TextButton(onClick = { showInsightDialog = null }) {
                    Text("DONE", color = DeepTeal, fontWeight = FontWeight.Bold)
                }
            },
            title = { Text("Health Insight", color = RoyalNavy, fontWeight = FontWeight.Bold) },
            text = { Text(showInsightDialog!!, color = SlateGray) },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp)
        )
    }

    Scaffold(
        containerColor = SoftMist,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                DashboardHeader(
                    firstName = if (firstName == "Patient") "" else firstName,
                    hasUnreadNotifications = unreadCount > 0,
                    onNotificationClick = onNotificationClick
                )
            }

            // 1. Upcoming Appointment Section
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    DashboardSectionHeader(
                        title = "Upcoming Appointment",
                        icon = Icons.Default.CalendarMonth,
                        actionText = "View All",
                        onActionClick = { rootNavController.navigate(Screen.AppointmentHistory.route) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    when (val apptResource = nextApptState) {
                        is Resource.Loading -> {
                            Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = DeepTeal)
                            }
                        }
                        is Resource.Error -> {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                Text(apptResource.message ?: "Error loading appointments", color = ErrorRed)
                            }
                        }
                        else -> {
                            val nextAppt = (apptResource as? Resource.Success)?.data
                            if (nextAppt != null) {
                                UpcomingAppointmentCard(
                                    appointment = nextAppt,
                                    onDetailsClick = { rootNavController.navigate(Screen.AppointmentHistory.route) }
                                )
                            } else {
                                EmptyAppointmentCard(
                                    onBookClick = { rootNavController.navigate(Screen.AppointmentBooking.route) }
                                )
                            }
                        }
                    }
                }
            }

            // 2. Promotions Section
            val promotionsResource = promotionsState
            if (promotionsResource is Resource.Success && promotionsResource.data.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        DashboardSectionHeader(
                            title = "Promotions",
                            icon = Icons.Default.LocalOffer,
                            actionText = "View All",
                            onActionClick = { /* Navigate to promotions if screen exists */ }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            promotionsResource.data.forEach { promotion ->
                                PromotionCard(
                                    title = promotion.title,
                                    value = formatDiscount(promotion.discountType, promotion.discountValue ?: 0.0),
                                    description = promotion.description ?: ""
                                )
                            }
                        }
                    }
                }
            }

            // 3. Health Insights Section
            val insightsResource = insightsState
            if (insightsResource is Resource.Success && insightsResource.data.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        DashboardSectionHeader(
                            title = "Health Insights",
                            icon = Icons.Default.Lightbulb
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val topInsight = insightsResource.data.first()
                        InsightCard(
                            title = topInsight.title,
                            subtitle = topInsight.description
                        ) {
                            showInsightDialog = topInsight.description
                        }
                    }
                }
            }

            // 4. Clinic News Section
            val newsResource = newsState
            if (newsResource is Resource.Success && newsResource.data.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        DashboardSectionHeader(
                            title = "Clinic News",
                            icon = Icons.Default.Campaign
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val firstNews = newsResource.data.first()
                        NewsCard(
                            title = firstNews.title,
                            description = firstNews.description
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardHeader(
    firstName: String,
    hasUnreadNotifications: Boolean,
    onNotificationClick: () -> Unit
) {
    val isLargeFont = LocalDensity.current.fontScale > 1.2f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                // Wave/curve matching target reference aesthetics
                val path = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width, size.height * 0.82f)
                    quadraticTo(size.width * 0.5f, size.height, 0f, size.height * 0.82f)
                    close()
                }
                drawPath(path, brush = WavyTealGradient)
                
                // Extra translucent highlight glow overlay
                val glowPath = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width, size.height * 0.65f)
                    quadraticTo(size.width * 0.5f, size.height * 0.78f, 0f, size.height * 0.62f)
                    close()
                }
                drawPath(glowPath, color = Color.White.copy(alpha = 0.08f))
            }
            .statusBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 48.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            ) {
                // Placeholder for Clinic Logo PNG
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.clinexusapp.R.drawable.ic_person_placeholder),
                        contentDescription = "Clinic Logo",
                        modifier = Modifier.size(36.dp).clip(CircleShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = if (firstName.isNotBlank()) "Hello, $firstName!" else "Hello!",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Welcome back!",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "HEALTHY SMILES BRIGHTER TOMORROWS",
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Box(modifier = Modifier.size(48.dp)) {
                    IconButton(onClick = onNotificationClick, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    if (hasUnreadNotifications) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(ErrorRed, CircleShape)
                                .align(Alignment.TopEnd)
                                .offset(x = (-4).dp, y = 4.dp)
                        )
                    }
                }
                if (!isLargeFont) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Smile\nBrighter\nToday \u263A",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontStyle = FontStyle.Italic,
                        lineHeight = 16.sp,
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardSectionHeader(
    title: String,
    icon: ImageVector,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(Color(0xFFE0F7F4), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = DeepTeal,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = RoyalNavy
            )
        }
        if (actionText != null && onActionClick != null) {
            TextButton(
                onClick = onActionClick,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = actionText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepTeal
                )
            }
        }
    }
}

@Composable
fun UpcomingAppointmentCard(
    appointment: AppointmentDTO,
    onDetailsClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(alpha = 0.03f)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color(0xFFE0F7F4), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = DeepTeal,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appointment.serviceName ?: appointment.treatment,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = RoyalNavy
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarMonth, null, tint = SlateGray, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = DateUtils.formatDisplayDate(appointment.appointmentDate),
                            fontSize = 13.sp,
                            color = SlateGray
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(Icons.Default.Schedule, null, tint = SlateGray, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = DateUtils.formatDisplayTime(appointment.startTime),
                            fontSize = 13.sp,
                            color = SlateGray
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    val status = com.example.clinexusapp.viewmodel.mapAppointmentStatus(appointment.appointmentStatus)
                    val (label, color, icon) = when (status) {
                        com.example.clinexusapp.viewmodel.AppointmentStatus.PENDING -> Triple("PENDING", Color(0xFFD97706), Icons.Default.Schedule)
                        com.example.clinexusapp.viewmodel.AppointmentStatus.CONFIRMED -> Triple("CONFIRMED", Color(0xFF168C2F), Icons.Default.CheckCircle)
                        com.example.clinexusapp.viewmodel.AppointmentStatus.RESCHEDULE_REQUESTED -> Triple("RESCHEDULE REQUESTED", Color(0xFF7C3AED), Icons.Default.EventRepeat)
                        com.example.clinexusapp.viewmodel.AppointmentStatus.COMPLETED -> Triple("COMPLETED", Color(0xFF2563EB), Icons.Default.TaskAlt)
                        com.example.clinexusapp.viewmodel.AppointmentStatus.CANCELLED -> Triple("CANCELLED", Color(0xFFD92D38), Icons.Default.Cancel)
                        else -> Triple("UNKNOWN", Color.Gray, Icons.Default.Help)
                    }
                    Surface(
                        color = color.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(icon, null, tint = color, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = label,
                                color = color,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                // Circular design visual accent decoration on the right
                Box(
                    modifier = Modifier.size(54.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = DeepTeal.copy(alpha = 0.1f),
                            radius = size.minDimension / 2f,
                            style = Stroke(width = 3.dp.toPx())
                        )
                        drawArc(
                            color = DeepTeal,
                            startAngle = -90f,
                            sweepAngle = 280f,
                            useCenter = false,
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = DeepTeal,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onDetailsClick,
                modifier = Modifier
                    .align(Alignment.End)
                    .height(40.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DeepTeal),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp)
            ) {
                Text(
                    text = "View Details",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun EmptyAppointmentCard(onBookClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No upcoming appointment",
                color = SlateGray,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onBookClick,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DeepTeal)
            ) {
                Text("Book appointment", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun PromotionCard(
    title: String,
    value: String,
    description: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFFFFE8D5), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalOffer,
                    contentDescription = null,
                    tint = Color(0xFFE77A35),
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoyalNavy
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    color = Color(0xFFE77A35),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    color = SlateGray,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun InsightCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(22.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFFFFEAE3), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoFixHigh,
                    contentDescription = null,
                    tint = Color(0xFFFF8A65),
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoyalNavy
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = SlateGray,
                    fontSize = 13.sp
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = LightSlate,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun NewsCard(
    title: String,
    description: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFFE1F5FE), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Business,
                    contentDescription = null,
                    tint = Color(0xFF0288D1),
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoyalNavy
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    color = SlateGray,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

private fun formatDiscount(type: String?, value: Double): String {
    return if (type.equals("percentage", ignoreCase = true) || type.equals("percent", ignoreCase = true)) {
        "${value.toInt()}% OFF"
    } else {
        "PHP ${String.format(Locale.US, "%,.2f", value)} OFF"
    }
}
