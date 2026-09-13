package com.example.clinexusapp.ui.screens.dashboard

import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Bell
import com.composables.icons.lucide.CalendarDays
import com.composables.icons.lucide.Lightbulb
import com.composables.icons.lucide.Megaphone
import com.composables.icons.lucide.Tag


import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import com.example.clinexusapp.R
import com.example.clinexusapp.model.AppointmentDTO
import com.example.clinexusapp.model.ClinicNewsDTO
import com.example.clinexusapp.model.HealthInsightDTO
import com.example.clinexusapp.model.PromotionDTO
import com.example.clinexusapp.ui.navigation.Screen
import com.example.clinexusapp.ui.components.shimmer
import com.example.clinexusapp.util.Resource
import com.example.clinexusapp.util.SessionManager
import com.example.clinexusapp.viewmodel.DashboardViewModel
import java.util.Locale
import kotlinx.coroutines.delay

internal object DashboardStyle {
    val Background = Color(0xFFF0FAFA)
    val Teal = Color(0xFF00A99D)
    val Navy = Color(0xFF080B36)
    val Muted = Color(0xFF7E87A4)
    val Mint = Color(0xFFD9F6F3)
    val Orange = Color(0xFFFF792A)
    val CardShape = RoundedCornerShape(18.dp)
}

private val FallbackHealthInsight = HealthInsightDTO(
    id = "local-health-insight",
    title = "Protect your smile today",
    description = "Brush twice a day, floss gently, and schedule a dental check-up when it is due.",
    category = "Daily tip",
    iconEmoji = "health",
)

private val FallbackClinicNews = ClinicNewsDTO(
    id = "local-clinic-news",
    title = "Clinic hours",
    description = "Our clinic is open Monday to Saturday for appointments and patient support.",
    date = "Mon-Sat · 8:00 AM - 5:00 PM",
)

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    rootNavController: NavController,
    onNotificationClick: () -> Unit,
) {
    val user by SessionManager.currentUser.collectAsState()
    val newsState by viewModel.newsState.collectAsState()
    val insightsState by viewModel.insightsState.collectAsState()
    val promotionsState by viewModel.promotionsState.collectAsState()
    val nextApptState by viewModel.nextAppointment.collectAsState()
    val unreadCount by viewModel.unreadNotificationsCount.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchDashboardData()
        while (true) {
            delay(30_000)
            viewModel.refreshAppointmentsAndNotifications()
        }
    }

    DashboardContent(
        firstName = user?.firstName.orEmpty(),
        newsState = newsState,
        insightsState = insightsState,
        promotionsState = promotionsState,
        nextAppointmentState = nextApptState,
        hasUnreadNotifications = unreadCount > 0,
        onNotificationClick = onNotificationClick,
        onAppointmentsClick = { rootNavController.navigate(Screen.AppointmentHistory.route) },
        onAppointmentDetailsClick = { appointmentId ->
            rootNavController.navigate(Screen.AppointmentHistory.createRoute(appointmentId))
        },
        onBookClick = { rootNavController.navigate(Screen.AppointmentBooking.route) },
        onRetry = viewModel::fetchDashboardData,
    )
}

@Composable
internal fun DashboardContent(
    firstName: String,
    newsState: Resource<List<ClinicNewsDTO>>,
    insightsState: Resource<List<HealthInsightDTO>>,
    promotionsState: Resource<List<PromotionDTO>>,
    nextAppointmentState: Resource<AppointmentDTO?>,
    hasUnreadNotifications: Boolean,
    onNotificationClick: () -> Unit,
    onAppointmentsClick: () -> Unit,
    onAppointmentDetailsClick: (Int) -> Unit,
    onBookClick: () -> Unit,
    onRetry: () -> Unit,
) {
    val listState = rememberLazyListState()
    val headerVisible by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    var selectedInsight by remember { mutableStateOf<HealthInsightDTO?>(null) }
    var showPromotions by remember { mutableStateOf(false) }
    val promotions = (promotionsState as? Resource.Success)?.data.orEmpty()

    DashboardSystemBars(headerVisible)

    selectedInsight?.let { insight ->
        AlertDialog(
            onDismissRequest = { selectedInsight = null },
            confirmButton = {
                TextButton(onClick = { selectedInsight = null }) {
                    Text("Done", color = DashboardStyle.Teal, fontWeight = FontWeight.Bold)
                }
            },
            title = { Text(insight.title, color = DashboardStyle.Navy, fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn { item { Text(insight.description, color = DashboardStyle.Muted) } }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
        )
    }

    if (showPromotions) {
        AlertDialog(
            onDismissRequest = { showPromotions = false },
            confirmButton = {
                TextButton(onClick = { showPromotions = false }) {
                    Text("Done", color = DashboardStyle.Teal, fontWeight = FontWeight.Bold)
                }
            },
            title = { Text("Promotions", color = DashboardStyle.Navy, fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(promotions, key = { it.promotionId }) { promotion ->
                        Column {
                            Text(promotion.title, color = DashboardStyle.Navy, fontWeight = FontWeight.Bold)
                            Text(
                                formatDiscount(promotion.discountType, promotion.discountValue ?: 0.0),
                                color = DashboardStyle.Orange,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(promotion.description.orEmpty(), color = DashboardStyle.Muted)
                        }
                    }
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
        )
    }

    Box(Modifier.fillMaxSize().background(DashboardStyle.Background)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 8.dp),
        ) {
            item(key = "header") {
                DashboardHeader(firstName, hasUnreadNotifications, onNotificationClick)
            }
            item(key = "appointment") {
                Column(Modifier.padding(horizontal = 22.dp)) {
                    DashboardSectionHeader(
                        title = "Upcoming Appointment",
                        icon = Lucide.CalendarDays,
                        actionText = "View All",
                        onActionClick = onAppointmentsClick,
                    )
                    Spacer(Modifier.height(4.dp))
                    when (val state = nextAppointmentState) {
                        Resource.Loading, Resource.Idle -> DashboardLoadingCard()
                        is Resource.Error -> DashboardErrorCard(state.message ?: "Unable to load appointments", onRetry)
                        is Resource.Success -> state.data?.let { appointment ->
                            UpcomingAppointmentCard(appointment) {
                                onAppointmentDetailsClick(appointment.appointmentId)
                            }
                        } ?: EmptyAppointmentCard(onBookClick)
                    }
                }
            }
            if (promotions.isNotEmpty()) {
                item(key = "promotions") {
                    Column(Modifier.padding(horizontal = 22.dp)) {
                        DashboardSectionHeader("Promotions", Lucide.Tag, "View All") {
                            showPromotions = true
                        }
                        Spacer(Modifier.height(4.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            promotions.take(2).forEach { promotion ->
                                PromotionCard(
                                    title = promotion.title,
                                    value = formatDiscount(promotion.discountType, promotion.discountValue ?: 0.0),
                                    description = promotion.description.orEmpty(),
                                )
                            }
                        }
                    }
                }
            }
            val insight = (insightsState as? Resource.Success)?.data?.firstOrNull() ?: FallbackHealthInsight
            item(key = "insights") {
                Column(Modifier.padding(horizontal = 22.dp)) {
                    DashboardSectionHeader("Health Insights", Lucide.Lightbulb)
                    Spacer(Modifier.height(4.dp))
                    InsightCard(insight.title, insight.description, insight.category) { selectedInsight = insight }
                }
            }
            val news = (newsState as? Resource.Success)?.data?.firstOrNull() ?: FallbackClinicNews
            item(key = "news") {
                Column(Modifier.padding(horizontal = 22.dp)) {
                    DashboardSectionHeader("Clinic News", Lucide.Megaphone)
                    Spacer(Modifier.height(4.dp))
                    NewsCard(news.title, news.description, news.date)
                }
            }
        }
        // Keep content from scrolling under the status icons after the header leaves the screen.
        if (!headerVisible) {
            Box(Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars).background(DashboardStyle.Background))
        }
    }
}

@Composable
fun DashboardHeader(
    firstName: String,
    hasUnreadNotifications: Boolean,
    onNotificationClick: () -> Unit,
) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val silhouette = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width, size.height - 30.dp.toPx())
                    cubicTo(size.width * 0.88f, size.height + 10.dp.toPx(), size.width * 0.12f, size.height + 10.dp.toPx(), 0f, size.height - 30.dp.toPx())
                    close()
                }
                clipPath(silhouette) {
                    drawRect(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF72DDD5), Color(0xFF09BAB1), Color(0xFF00A59C)),
                            start = Offset.Zero,
                            end = Offset(size.width * 0.82f, size.height),
                        )
                    )
                    val upperWave = Path().apply {
                        moveTo(size.width * 0.54f, 8.dp.toPx())
                        cubicTo(size.width * 0.77f, size.height * 0.35f, size.width * 0.98f, size.height * 0.22f, size.width * 1.1f, size.height * 0.26f)
                        lineTo(size.width * 1.1f, size.height * 0.72f)
                        cubicTo(size.width * 0.80f, size.height * 0.57f, size.width * 0.64f, size.height * 0.43f, size.width * 0.54f, 8.dp.toPx())
                        close()
                    }
                    drawPath(upperWave, Color.White.copy(alpha = 0.10f))
                    val lowerWave = Path().apply {
                        moveTo(0f, size.height * 0.66f)
                        quadraticTo(size.width * 0.18f, size.height * 0.86f, size.width * 0.53f, size.height)
                        lineTo(0f, size.height)
                        close()
                    }
                    drawPath(lowerWave, Color(0xFF008F98).copy(alpha = 0.10f))
                }
            }
            .statusBarsPadding(),
    ) {
        val showSlogan = maxWidth >= 380.dp && fontScale <= 1.15f
        Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 22.dp, top = 20.dp, bottom = 25.dp)) {
            Row(Modifier.fillMaxWidth().padding(end = 42.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_dashboard_tooth),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(42.dp),
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (firstName.isBlank()) "Hello!" else "Hello, ${firstName.trim()}!",
                        fontSize = 25.sp,
                        lineHeight = 29.sp,
                        letterSpacing = (-0.6).sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text("Welcome back!", fontSize = 16.sp, lineHeight = 21.sp, color = Color.White)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.padding(start = 56.dp, end = if (showSlogan) 62.dp else 0.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.width(18.dp).height(1.dp).background(Color.White.copy(alpha = 0.85f)))
                Spacer(Modifier.width(7.dp))
                Text(
                    "HEALTHY SMILES BRIGHTER TOMORROWS",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 7.sp,
                    lineHeight = 11.sp,
                    letterSpacing = 1.1.sp,
                )
            }
        }
        IconButton(
            onClick = onNotificationClick,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 3.dp, end = 12.dp).size(48.dp),
        ) {
            Box {
                Icon(
                    Lucide.Bell,
                    contentDescription = if (hasUnreadNotifications) "Notifications, unread notifications" else "Notifications",
                    tint = Color.White,
                    modifier = Modifier.size(27.dp),
                )
                if (hasUnreadNotifications) {
                    Box(Modifier.align(Alignment.TopEnd).size(8.dp).background(Color(0xFFFF575D), CircleShape))
                }
            }
        }
        if (showSlogan) {
            Text(
                "Smile\nBrighter\nToday ◡",
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 17.dp, bottom = 22.dp),
                color = Color.White.copy(alpha = 0.9f),
                fontFamily = FontFamily.Cursive,
                fontStyle = FontStyle.Italic,
                fontSize = 13.sp,
                lineHeight = 13.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun DashboardSectionHeader(
    title: String,
    icon: ImageVector,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Row(Modifier.fillMaxWidth().heightIn(min = 38.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(34.dp).background(DashboardStyle.Mint, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = DashboardStyle.Teal, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(
            title,
            modifier = Modifier.weight(1f).semantics { heading() },
            fontSize = 17.sp,
            lineHeight = 21.sp,
            letterSpacing = (-0.4).sp,
            fontWeight = FontWeight.Bold,
            color = DashboardStyle.Navy,
        )
        if (actionText != null && onActionClick != null) {
            TextButton(onClick = onActionClick, contentPadding = PaddingValues(start = 8.dp)) {
                Text(actionText, color = DashboardStyle.Teal, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DashboardLoadingCard() {
    Surface(shape = DashboardStyle.CardShape, color = Color.White, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.height(112.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.fillMaxWidth(0.55f).height(20.dp).clip(RoundedCornerShape(8.dp)).shimmer())
            Box(Modifier.fillMaxWidth(0.8f).height(16.dp).clip(RoundedCornerShape(8.dp)).shimmer())
            Box(Modifier.fillMaxWidth(0.65f).height(16.dp).clip(RoundedCornerShape(8.dp)).shimmer())
        }
    }
}

@Composable
private fun DashboardErrorCard(message: String, onRetry: () -> Unit) {
    Surface(shape = DashboardStyle.CardShape, color = Color.White, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, color = DashboardStyle.Muted, textAlign = TextAlign.Center)
            TextButton(onClick = onRetry) { Text("Try again", color = DashboardStyle.Teal) }
        }
    }
}

@Composable
private fun DashboardSystemBars(headerVisible: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, headerVisible) {
        val activity = view.context.findActivity()
        val controller = if (!view.isInEditMode && activity != null) WindowCompat.getInsetsController(activity.window, view) else null
        val previousStatus = controller?.isAppearanceLightStatusBars
        val previousNavigation = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars = !headerVisible
        controller?.isAppearanceLightNavigationBars = true
        onDispose {
            previousStatus?.let { controller.isAppearanceLightStatusBars = it }
            previousNavigation?.let { controller.isAppearanceLightNavigationBars = it }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun formatDiscount(type: String?, value: Double): String =
    if (type.equals("percentage", ignoreCase = true) || type.equals("percent", ignoreCase = true)) {
        "${value.toInt()}% OFF"
    } else {
        "PHP ${String.format(Locale.US, "%,.2f", value)} OFF"
    }
