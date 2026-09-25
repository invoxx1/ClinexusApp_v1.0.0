package com.example.clinexusapp.ui.screens.appointments

import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.CalendarCheck
import com.composables.icons.lucide.CalendarDays
import com.composables.icons.lucide.CalendarSync
import com.composables.icons.lucide.CalendarX
import com.composables.icons.lucide.CircleCheck
import com.composables.icons.lucide.CircleQuestionMark
import com.composables.icons.lucide.CircleX
import com.composables.icons.lucide.Clock
import com.composables.icons.lucide.Info
import com.composables.icons.lucide.MapPin
import com.composables.icons.lucide.Stethoscope
import com.composables.icons.lucide.Tag
import com.composables.icons.lucide.UserRound
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.X


import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import com.example.clinexusapp.ui.components.WalkthroughTarget
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.scale
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.clinexusapp.model.AppointmentDTO
import com.example.clinexusapp.model.AvailableSlotDTO
import com.example.clinexusapp.ui.theme.*
import com.example.clinexusapp.ui.components.dentalServiceIcon
import com.example.clinexusapp.ui.components.shimmer
import com.example.clinexusapp.util.DateUtils
import com.example.clinexusapp.util.Resource
import com.example.clinexusapp.viewmodel.*
import java.text.SimpleDateFormat
import java.util.Calendar
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

private val AppointmentCardShape = RoundedCornerShape(18.dp)

private data class StatusStyle(val label: String, val icon: ImageVector, val foreground: Color, val background: Color, val message: String)

@Composable
private fun statusStyle(status: AppointmentStatus, needsPatientChoice: Boolean = false): StatusStyle = when (status) {
    AppointmentStatus.PENDING -> StatusStyle("Pending", Lucide.Clock, Color(0xFFD97706), Color(0xFFFFF7E6), "Waiting for confirmation from the clinic.")
    AppointmentStatus.CONFIRMED -> StatusStyle("Confirmed", Lucide.CircleCheck, Color(0xFF15803D), Color(0xFFECFDF3), "Your appointment is confirmed.")
    AppointmentStatus.IN_PROGRESS -> StatusStyle("In progress", Lucide.Clock, Color(0xFF0369A1), Color(0xFFE0F2FE), "Your dental visit is now in progress.")
    AppointmentStatus.COMPLETED -> StatusStyle("Completed", Lucide.CircleCheck, Color(0xFF2563EB), Color(0xFFEFF6FF), "This appointment has been completed.")
    AppointmentStatus.NO_SHOW -> StatusStyle("No show", Lucide.CalendarX, Color(0xFFD97706), Color(0xFFFFF7E6), "You were marked as absent for this appointment.")
    AppointmentStatus.CANCELLED -> StatusStyle("Cancelled", Lucide.CircleX, Color(0xFFDC2626), Color(0xFFFFF2F2), "This appointment was cancelled.")
    AppointmentStatus.RESCHEDULE_REQUESTED -> if (needsPatientChoice) {
        StatusStyle("Action needed", Lucide.CalendarSync, Color(0xFF7C3AED), Color(0xFFF5F3FF), "The clinic requested a new schedule. Choose another available date and time.")
    } else {
        StatusStyle("Reschedule", Lucide.CalendarSync, Color(0xFF7C3AED), Color(0xFFF5F3FF), "Your selected schedule is waiting for clinic approval.")
    }
    AppointmentStatus.CANCELLATION_REQUESTED -> StatusStyle("Cancellation", Lucide.CircleX, Color(0xFFDC2626), Color(0xFFFFF2F2), "Your cancellation request is being processed.")
    AppointmentStatus.UNKNOWN -> StatusStyle("Status unavailable", Lucide.CircleQuestionMark, MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.surfaceVariant, "This appointment has an unrecognized status.")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AppointmentHistoryScreen(
    onBack: () -> Unit,
    onNavigateToBooking: () -> Unit,
    viewModel: HistoryViewModel,
    initialAppointmentId: Int? = null,
    @Suppress("UNUSED_PARAMETER") activeWalkthroughTarget: WalkthroughTarget? = null,
    onWalkthroughTarget: (WalkthroughTarget, Rect) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var screenActive by remember { mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
    val state by viewModel.uiState.collectAsState()
    var selected by remember { mutableStateOf<AppointmentDTO?>(null) }
    var cancelTarget by remember { mutableStateOf<AppointmentDTO?>(null) }
    var rescheduleTarget by remember { mutableStateOf<AppointmentDTO?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(initialAppointmentId, state.appointments) {
        val appointments = (state.appointments as? Resource.Success)?.data.orEmpty()
        if (initialAppointmentId != null && selected == null) {
            selected = appointments.firstOrNull { it.appointmentId == initialAppointmentId }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            screenActive = event != Lifecycle.Event.ON_STOP
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(screenActive) {
        if (!screenActive) return@LaunchedEffect
        viewModel.fetchHistory(clearOperation = false)
        while (true) {
            delay(15_000)
            viewModel.fetchHistory(clearOperation = false, showLoading = false)
        }
    }

    LaunchedEffect(state.operation) {
        when (val operation = state.operation) {
            is Resource.Success -> {
                if (cancelTarget != null || rescheduleTarget != null) {
                    cancelTarget = null
                    rescheduleTarget = null
                }
                viewModel.clearOperation()
            }
            is Resource.Error -> {
                errorMessage = operation.message ?: "Request failed. Please try again."
                viewModel.clearOperation()
            }
            else -> Unit
        }
    }

    LaunchedEffect(state.completedRequest) {
        val message = state.completedRequest ?: return@LaunchedEffect
        successMessage = message
        cancelTarget = null
        rescheduleTarget = null
        viewModel.clearCompletedRequest()
        delay(4500)
        successMessage = null
    }

    if (cancelTarget != null) {
        CancellationSheet(cancelTarget!!, state.cancellationSubmitting, onDismiss = { cancelTarget = null }, onConfirm = { reason -> viewModel.cancelAppointment(cancelTarget!!.appointmentId, reason) })
    }
    if (rescheduleTarget != null) {
        RescheduleSheet(rescheduleTarget!!, state, viewModel, onDismiss = { rescheduleTarget = null })
    }
    errorMessage?.let { message ->
        AppointmentErrorDialog(
            message = message,
            onDismiss = { errorMessage = null }
        )
    }
    if (selected != null) AppointmentDetailsDialog(
        appointment = selected!!,
        onDismiss = { selected = null },
        onCheckIn = { viewModel.selfCheckIn(selected!!.appointmentId) },
        onQueue = { viewModel.loadQueueStatus(selected!!.appointmentId) },
        checkingIn = state.checkingInAppointmentId == selected!!.appointmentId,
        checkedIn = !selected!!.checkedInAt.isNullOrBlank() || selected!!.appointmentId in state.checkedInAppointmentIds,
    )
    if (state.queueAppointmentId != null) {
        QueueStatusDialog(
            status = state.queueStatus,
            onRetry = { viewModel.loadQueueStatus(state.queueAppointmentId!!) },
            onDismiss = viewModel::closeQueueStatus,
        )
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp, 8.dp, 20.dp, 120.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            stickyHeader {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(bottom = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AppointmentHeader(onBack, onNavigateToBooking, onWalkthroughTarget)
                    AppointmentTabs(state, viewModel, onWalkthroughTarget)
                    Text(
                        "${tabLabel(state.selectedTab)} Appointments",
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            item {
                AnimatedVisibility(
                    visible = successMessage != null,
                    enter = fadeIn() + scaleIn(initialScale = 0.96f),
                    exit = fadeOut(),
                ) {
                    successMessage?.let { AppointmentSuccessBanner(it) }
                }
            }
            when (val appointments = state.appointments) {
                Resource.Idle, Resource.Loading -> item { LoadingState() }
                is Resource.Error -> item { ErrorState(appointments.message ?: "Unable to load appointments.", viewModel::fetchHistory) }
                is Resource.Success -> {
                    val visible = viewModel.filteredAppointments(state.selectedTab, appointments.data)
                    if (visible.isEmpty()) item { EmptyAppointments() }
                    else items(visible, key = { it.appointmentId }) { appointment ->
                        AppointmentCard(
                            appointment = appointment,
                            onClick = { selected = appointment },
                            onCancel = { cancelTarget = appointment },
                            onReschedule = { rescheduleTarget = appointment },
                            onBook = onNavigateToBooking,
                            onAddToCalendar = { addAppointmentToCalendar(context, appointment) },
                            onCheckIn = { viewModel.selfCheckIn(appointment.appointmentId) },
                            onQueue = { viewModel.loadQueueStatus(appointment.appointmentId) },
                            checkingIn = state.checkingInAppointmentId == appointment.appointmentId,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppointmentErrorDialog(message: String, onDismiss: () -> Unit) {
    val alreadyCheckedIn = message.trim().trimEnd('.').equals("You are already checked in", ignoreCase = true)
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(26.dp),
            shadowElevation = 18.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFEEEE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Lucide.CalendarX,
                        contentDescription = null,
                        tint = ErrorRed,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Text(
                    if (alreadyCheckedIn) "Already checked in" else "Schedule not submitted",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 22.sp,
                    lineHeight = 27.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    if (alreadyCheckedIn) "You have already checked in for this appointment." else message,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(contentColor = Color.White, containerColor = VibrantTeal)
                ) {
                    Text(if (alreadyCheckedIn) "OK" else "Choose Another Time", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun AppointmentSuccessBanner(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFE9FAF3),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF9ADFC3)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                Lucide.CircleCheck,
                contentDescription = "Request completed",
                tint = Color(0xFF078A5B),
                modifier = Modifier.size(26.dp),
            )
            Column(Modifier.weight(1f)) {
                Text("Request submitted", color = Color(0xFF05603A), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(message, color = Color(0xFF28745A), fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
private fun AppointmentHeader(
    onBack: () -> Unit,
    onNavigateToBooking: () -> Unit,
    onWalkthroughTarget: (WalkthroughTarget, Rect) -> Unit = { _, _ -> },
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(40.dp)
                .offset(x = (-8).dp)
        ) {
            Icon(
                Lucide.ArrowLeft,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(25.dp)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
        ) {
            Text(
                "My Appointments",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Manage your visits and schedules",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }

        Surface(
            onClick = onNavigateToBooking,
            modifier = Modifier
                .size(56.dp)
                .onGloballyPositioned { onWalkthroughTarget(WalkthroughTarget.BOOK_APPOINTMENT, it.boundsInRoot()) },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Lucide.CalendarDays,
                    contentDescription = "Book appointment",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}

@Composable
private fun AppointmentTabs(
    state: HistoryUiState,
    viewModel: HistoryViewModel,
    onWalkthroughTarget: (WalkthroughTarget, Rect) -> Unit = { _, _ -> },
) {
    val appointments =
        (state.appointments as? Resource.Success)?.data.orEmpty()
    val tabs = AppointmentTab.values().toList()
    val labels = tabs.associateWith { tab ->
        val count = appointments.count {
            mapAppointmentStatus(it.appointmentStatus).toTab() == tab
        }
        "${tabLabel(tab)} ($count)"
    }
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val labelStyle = LocalTextStyle.current.copy(
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        letterSpacing = 0.sp,
    )

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { onWalkthroughTarget(WalkthroughTarget.APPOINTMENT_STATUSES, it.boundsInRoot()) }
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val horizontalPadding = 6.dp
            val availableLabelWidth = with(density) {
                (maxWidth / tabs.size - horizontalPadding * 2).toPx()
            }.toInt().coerceAtLeast(1)
            // Fit every label using the actual device font scale. Measure bold so
            // selecting a different tab never changes the shared size or clips it.
            val fittedFontSize = remember(labels, availableLabelWidth, textMeasurer, labelStyle, density) {
                fun fits(size: Float) = labels.values.all { label ->
                    textMeasurer.measure(
                        text = label,
                        style = labelStyle.copy(fontSize = size.sp, fontWeight = FontWeight.Bold),
                        softWrap = false,
                        maxLines = 1,
                    ).size.width <= availableLabelWidth
                }
                if (fits(12f)) 12f else {
                    var lower = 0.1f
                    var upper = 12f
                    repeat(14) {
                        val candidate = (lower + upper) / 2
                        if (fits(candidate)) lower = candidate else upper = candidate
                    }
                    lower
                }
            }
                    Row(
                        Modifier.fillMaxWidth().height(IntrinsicSize.Min).selectableGroup(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        tabs.forEach { tab ->
                            val selected = state.selectedTab == tab
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(22.dp))
                                    .selectable(
                                        selected = selected,
                                        role = Role.Tab,
                                        onClick = { viewModel.selectTab(tab) },
                                    ),
                                shape = RoundedCornerShape(22.dp),
                                color = if (selected) VibrantTeal else Color.Transparent,
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 10.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = labels.getValue(tab),
                                        color = if (selected) White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = labelStyle,
                                        fontSize = fittedFontSize.sp,
                                        lineHeight = (fittedFontSize * 1.3f).sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        softWrap = false,
                                    )
                                }
                            }
                        }
                    }
        }
    }
}

private fun tabLabel(tab: AppointmentTab) = when (tab) { AppointmentTab.PENDING -> "Pending"; AppointmentTab.CONFIRMED -> "Confirmed"; AppointmentTab.COMPLETED -> "Completed"; AppointmentTab.CANCELLED -> "Cancelled" }

@Composable
private fun AppointmentCard(
    appointment: AppointmentDTO,
    onClick: () -> Unit,
    onCancel: () -> Unit,
    onReschedule: () -> Unit,
    onBook: () -> Unit,
    onAddToCalendar: () -> Unit,
    onCheckIn: () -> Unit,
    onQueue: () -> Unit,
    checkingIn: Boolean,
) {
    val status = mapAppointmentStatus(appointment.appointmentStatus)
    val style = statusStyle(status, appointment.needsPatientScheduleChoice)
    val rescheduleColor = if (isSystemInDarkTheme()) Color.White else DeepTeal
    val rescheduleContainer = if (isSystemInDarkTheme()) Color.Transparent else MaterialTheme.colorScheme.surface
    Surface(
        onClick = onClick,
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = "View appointment details for ${appointment.doctor}"
            role = Role.Button
        },
        color = MaterialTheme.colorScheme.surface,
        shape = AppointmentCardShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppointmentAvatar(appointment)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(appointment.doctor, color = MaterialTheme.colorScheme.onSurface, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(appointment.dentistSpecialty ?: "Dental care", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
                StatusBadge(style)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Lucide.CalendarDays, "Appointment date", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                Text(DateUtils.formatDisplayDate(appointment.appointmentDate), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp))
                Spacer(Modifier.width(14.dp))
                Box(Modifier.width(1.dp).height(22.dp).background(Color(0xFFD5E1E3)))
                Spacer(Modifier.width(14.dp))
                Icon(Lucide.Clock, "Appointment time", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                Text(DateUtils.formatDisplayTime(appointment.startTime), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp))
            }
            Text("${appointment.serviceName ?: appointment.treatment}  •  ${appointment.clinicName ?: "Rivera Dental Clinic"}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            if (isSystemInDarkTheme()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.Transparent,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.56f)),
                ) {
                    Box(
                        Modifier.fillMaxWidth().background(
                            Brush.linearGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.White.copy(alpha = 0.38f),
                                    0.22f to style.foreground.copy(alpha = 0.56f),
                                    0.72f to style.foreground.copy(alpha = 0.46f),
                                    1.0f to Color.White.copy(alpha = 0.16f),
                                ),
                            )
                        )
                    ) {
                        Text(
                            style.message,
                            color = Color.White.copy(alpha = 0.96f),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
            } else {
                Surface(modifier = Modifier.fillMaxWidth(), color = style.background, shape = RoundedCornerShape(12.dp)) {
                    Text(style.message, color = style.foreground, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                when (status) {

                    AppointmentStatus.PENDING -> {
                        OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f).height(38.dp), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, ErrorRed), colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = ErrorRed)) {
                            Icon(Lucide.CircleX, "Cancel appointment", modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Cancel", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(onClick = onReschedule, modifier = Modifier.weight(1f).height(38.dp), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, rescheduleColor), colors = ButtonDefaults.outlinedButtonColors(containerColor = rescheduleContainer, contentColor = rescheduleColor), contentPadding = PaddingValues(horizontal = 8.dp)) {
                            Icon(Lucide.CalendarSync, "Reschedule appointment", modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Reschedule", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }

                    AppointmentStatus.CONFIRMED -> {
                        OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f).height(38.dp), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, ErrorRed), colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = ErrorRed)) {
                            Icon(Lucide.CircleX, "Cancel appointment", modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Cancel", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(onClick = onReschedule, modifier = Modifier.weight(1f).height(38.dp), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, rescheduleColor), colors = ButtonDefaults.outlinedButtonColors(containerColor = rescheduleContainer, contentColor = rescheduleColor), contentPadding = PaddingValues(horizontal = 8.dp)) {
                            Icon(Lucide.CalendarSync, "Reschedule appointment", modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Reschedule", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }

                    AppointmentStatus.CANCELLED ->
                        OutlinedButton(onClick = onBook, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, DeepTeal), colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = DeepTeal)) {
                            Text("Book Again", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }

                    AppointmentStatus.RESCHEDULE_REQUESTED -> if (appointment.needsPatientScheduleChoice) {
                        Button(
                            onClick = onReschedule,
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                        ) {
                            Icon(Lucide.CalendarSync, "Choose new appointment schedule", modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(7.dp))
                            Text("Choose New Schedule", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    AppointmentStatus.COMPLETED,
                    AppointmentStatus.NO_SHOW,
                    AppointmentStatus.IN_PROGRESS,
                    AppointmentStatus.CANCELLATION_REQUESTED,
                    AppointmentStatus.UNKNOWN -> Unit
                }
            }
            if (status == AppointmentStatus.CONFIRMED || status == AppointmentStatus.IN_PROGRESS) {
                val isToday = DateUtils.appointmentDateOnly(appointment.appointmentDate) ==
                    java.time.LocalDate.now(BookingRules.clinicZone).toString()
                if (isToday) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (status == AppointmentStatus.CONFIRMED) {
                            Button(
                                onClick = onCheckIn,
                                modifier = Modifier.weight(1f).height(44.dp),
                                enabled = !checkingIn,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = VibrantTeal),
                            ) {
                                if (checkingIn) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                else Text("Check In", fontWeight = FontWeight.Bold)
                            }
                        }
                        OutlinedButton(
                            onClick = onQueue,
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(14.dp),
                        ) { Text("View Queue", fontWeight = FontWeight.Bold) }
                    }
                }
                if (status == AppointmentStatus.CONFIRMED) {
                    TextButton(onClick = onAddToCalendar, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Icon(Lucide.Plus, contentDescription = null, modifier = Modifier.size(19.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Add to phone calendar", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
internal fun QueueStatusDialog(
    status: Resource<com.example.clinexusapp.model.PatientQueueDTO>,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(26.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 20.dp) {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.fillMaxWidth().background(
                        Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.80f)))
                    ).padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = Color.White.copy(alpha = 0.15f), shape = CircleShape) {
                            Icon(Lucide.Clock, null, tint = Color.White, modifier = Modifier.padding(11.dp).size(26.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("CLINIC QUEUE", color = Color.White.copy(alpha = 0.72f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Text("Queue Status", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                        IconButton(onClick = onDismiss) { Icon(Lucide.X, "Close queue", tint = Color.White) }
                    }
                }
                when (status) {
                    Resource.Idle, Resource.Loading -> Column(
                        Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator()
                        Text("Checking your place in line…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    is Resource.Error -> {
                        Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Lucide.CircleX, null, tint = ErrorRed, modifier = Modifier.size(38.dp))
                            Text(status.message ?: "Unable to load queue status.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("Try Again") }
                        }
                    }
                    is Resource.Success -> {
                        val queue = status.data
                        val normalized = queue.queueStatus.lowercase()
                        val label = normalized.replace('_', ' ').replaceFirstChar { it.uppercase() }
                        val accent = when (normalized) {
                            "ready", "in_progress" -> Color(0xFF168354)
                            "delayed" -> Color(0xFFD97706)
                            "cancelled" -> ErrorRed
                            else -> MaterialTheme.colorScheme.primary
                        }
                        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(color = Color(0xFFE8F7EF), shape = RoundedCornerShape(50)) {
                                Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF16A364)))
                                    Spacer(Modifier.width(6.dp))
                                    Text("LIVE · Updates every 10 sec", color = Color(0xFF168354), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Surface(color = accent.copy(alpha = 0.10f), shape = RoundedCornerShape(50), border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))) {
                                Text(label, color = accent, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                            }
                            Text(
                                when (normalized) {
                                    "ready" -> "You're next! Please stay nearby."
                                    "in_progress" -> "Your dental visit is now in progress."
                                    "waiting" -> "You're checked in and waiting in line."
                                    "delayed" -> "The clinic is running a little behind."
                                    "not_checked_in" -> "Check in first to join today's queue."
                                    "not_available" -> "Queue tracking is not available yet."
                                    "completed" -> "Your appointment has been completed."
                                    "cancelled" -> "This appointment was cancelled."
                                    else -> "Your latest queue information is shown below."
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp,
                            )
                            if (queue.queuePosition != null || queue.patientsAhead != null || queue.estimatedWaitMinutes != null) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    QueueMetric("Position", queue.queuePosition?.let { "#$it" } ?: "—", Modifier.weight(1f))
                                    QueueMetric("Ahead", queue.patientsAhead?.toString() ?: "—", Modifier.weight(1f))
                                    AnimatedWaitMetric(queue.estimatedWaitMinutes, Modifier.weight(1f))
                                }
                            }
                            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(46.dp), shape = RoundedCornerShape(14.dp)) { Text("Done", fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(horizontal = 6.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, maxLines = 1)
        }
    }
}

@Composable
private fun AnimatedWaitMetric(minutes: Int?, modifier: Modifier = Modifier) {
    val animatedMinutes by animateIntAsState(targetValue = minutes ?: 0, animationSpec = tween(700), label = "estimated wait")
    val pulse = rememberInfiniteTransition(label = "wait pulse")
    val scale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.045f,
        animationSpec = infiniteRepeatable(animation = tween(900), repeatMode = RepeatMode.Reverse),
        label = "wait scale",
    )
    Surface(
        modifier = modifier.scale(scale),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)),
    ) {
        Column(Modifier.padding(horizontal = 6.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (minutes == null) "—" else "$animatedMinutes min", color = MaterialTheme.colorScheme.primary, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text("Est. wait", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, maxLines = 1)
        }
    }
}

private fun addAppointmentToCalendar(context: android.content.Context, appointment: AppointmentDTO) {
    val parser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    val date = DateUtils.appointmentDateOnly(appointment.appointmentDate)
    val start = parser.parse("$date ${appointment.startTime}")?.time ?: return
    val end = parser.parse("$date ${appointment.endTime}")?.time ?: (start + 60 * 60 * 1000)
    val intent = Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end)
        .putExtra(CalendarContract.Events.TITLE, "Dental appointment – ${appointment.doctor}")
        .putExtra(CalendarContract.Events.DESCRIPTION, appointment.serviceName ?: appointment.treatment)
        .putExtra(CalendarContract.Events.EVENT_LOCATION, appointment.clinicName ?: "Rivera Dental Clinic")
    context.startActivity(intent)
}

@Composable
private fun StatusBadge(style: StatusStyle) { Surface(color = style.background, shape = RoundedCornerShape(20.dp)) { Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) { Icon(style.icon, style.label, tint = style.foreground, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text(style.label.uppercase(), color = style.foreground, fontSize = 11.sp, fontWeight = FontWeight.Bold) } } }

@Composable
private fun AppointmentAvatar(appointment: AppointmentDTO, size: androidx.compose.ui.unit.Dp = 52.dp) {
    var imageFailed by remember(appointment.dentistProfileImage) { mutableStateOf(false) }
    if (appointment.dentistProfileImage.isNullOrBlank() || imageFailed) {
        Box(Modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) { Icon(Lucide.UserRound, "Dentist profile", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(size * 0.55f)) }
    } else AsyncImage(model = appointment.dentistProfileImage, contentDescription = "Dentist profile image", modifier = Modifier.size(size).clip(CircleShape), onError = { imageFailed = true })
}

@Composable
private fun DetailLine(icon: ImageVector, label: String, value: String) { Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) { Icon(icon, label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(10.dp)); Text("$label: ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp); Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium) } }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CancellationSheet(appointment: AppointmentDTO, submitting: Boolean, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var reason by remember { mutableStateOf<String?>(null) }
    var details by remember { mutableStateOf("") }
    val other = reason == "Other"
    val valid = !reason.isNullOrBlank() && (!other || details.isNotBlank())
    val isDark = isSystemInDarkTheme()
    ModalBottomSheet(
        onDismissRequest = { if (details.isBlank()) onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color(0x990B1820),
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        dragHandle = { SheetDragHandle() }
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 24.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Cancel visit", color = MaterialTheme.colorScheme.onSurface, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text("Tell us why you want to cancel this appointment.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 17.sp, lineHeight = 23.sp)
            AppointmentMiniSummary(appointment)
            listOf("Schedule conflict", "Feeling unwell", "Need a different time", "Other").forEach { option ->
                Surface(onClick = { reason = option }, color = if (reason == option) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, if (reason == option) VibrantTeal else Color(0xFFD8E2E6))) { Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) { RadioButton(reason == option, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = DeepTeal, unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant)); Spacer(Modifier.width(8.dp)); Text(option, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp) } }
            }
            if (other) OutlinedTextField(value = details, onValueChange = { if (it.length <= 250) details = it }, label = { Text("Please specify your reason") }, supportingText = { Text("${details.length}/250") }, modifier = Modifier.fillMaxWidth())
            Surface(color = Color(0xFFEAF5FF), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF1689E8)), contentAlignment = Alignment.Center) {
                        Icon(Lucide.Info, "Reschedule information", tint = White, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("You can also reschedule instead", color = Color(0xFF15558C), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("If you still need a visit, you can choose a new date and time.", color = Color(0xFF3976A8), fontSize = 13.sp)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(38.dp), enabled = !submitting, shape = RoundedCornerShape(16.dp), border = BorderStroke(2.dp, if (isDark) Color.White else DeepTeal), contentPadding = PaddingValues(horizontal = 6.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isDark) Color.White else DeepTeal)) { Text("Keep Appointment", fontSize = 13.sp, maxLines = 1, softWrap = false, fontWeight = FontWeight.Bold) }
                Button(onClick = { onConfirm(if (other) details else reason.orEmpty()) }, modifier = Modifier.weight(1f).height(38.dp), enabled = valid && !submitting, shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(horizontal = 6.dp), colors = ButtonDefaults.buttonColors(contentColor = Color.White, containerColor = ErrorRed, disabledContainerColor = Color(0xFFE7B7B7), disabledContentColor = if (isDark) Color(0xFF1F3A6D) else Color.White)) { if (submitting) CircularProgressIndicator(Modifier.size(18.dp), color = White) else Text("Confirm Cancellation", fontSize = 13.sp, maxLines = 1, softWrap = false, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RescheduleSheet(appointment: AppointmentDTO, state: HistoryUiState, viewModel: HistoryViewModel, onDismiss: () -> Unit) {
    var date by remember { mutableStateOf("") }
    var selectedSlot by remember { mutableStateOf<AvailableSlotDTO?>(null) }
    var note by remember { mutableStateOf("") }
    var showCalendar by remember { mutableStateOf(false) }
    DisposableEffect(appointment.appointmentId) {
        onDispose { viewModel.stopRescheduleSlots() }
    }
    LaunchedEffect(state.rescheduleSlots) {
        val current = (state.rescheduleSlots as? Resource.Success)?.data.orEmpty()
        if (selectedSlot != null && current.none {
            it.startTime?.take(5) == selectedSlot?.startTime?.take(5) && it.endTime?.take(5) == selectedSlot?.endTime?.take(5)
        }) selectedSlot = null
    }
    val dates = remember { (1..14).map { Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, it) } } }
    val dateValues = remember(dates) { dates.map { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(it.time) } }
    LaunchedEffect(appointment.appointmentId) {
        date = ""
        selectedSlot = null
        viewModel.loadRescheduleWorkingDays(appointment.dentistId)
        viewModel.loadRescheduleDates(appointment, dateValues)
    }
    if (showCalendar) {
        // Recreate the picker once the dentist schedule arrives; otherwise its
        // SelectableDates policy can remain the initial permissive one.
        key(state.rescheduleWorkingDays, state.rescheduleScheduleLoaded) {
        val initialMillis = remember(date) {
            runCatching {
                java.time.LocalDate.parse(date.ifBlank { java.time.LocalDate.now().plusDays(1).toString() })
                    .atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
            }.getOrNull()
        }
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val selectedDate = java.time.Instant.ofEpochMilli(utcTimeMillis)
                        .atZone(java.time.ZoneOffset.UTC).toLocalDate()
                    val dayName = selectedDate.dayOfWeek.name
                    return (state.rescheduleWorkingDays.isEmpty() || dayName in state.rescheduleWorkingDays) && selectedDate.isAfter(java.time.LocalDate.now()) &&
                        !selectedDate.isAfter(java.time.LocalDate.now().plusMonths(3)) &&
                        true
                }
            },
        )
        DatePickerDialog(
            onDismissRequest = { showCalendar = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val selectedDate = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneOffset.UTC).toLocalDate().toString()
                        date = selectedDate
                        selectedSlot = null
                        viewModel.loadCalendarRescheduleDate(appointment, selectedDate)
                    }
                    showCalendar = false
                }) { Text("Select") }
            },
            dismissButton = { TextButton(onClick = { showCalendar = false }) { Text("Cancel") } },
        ) {
            DatePicker(
                state = pickerState,
                colors = DatePickerDefaults.colors(disabledDayContentColor = ErrorRed)
            )
        }
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color(0x990B1820),
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        dragHandle = { SheetDragHandle() }
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 24.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(if (appointment.needsPatientScheduleChoice) "Choose New Schedule" else "Request Reschedule", color = MaterialTheme.colorScheme.onSurface, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(
                if (appointment.needsPatientScheduleChoice) "The clinic asked you to reschedule. Choose an available date and time for approval." else "Choose your preferred new schedule and we will send your request to the clinic.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 16.sp,
                lineHeight = 22.sp
            )
            AppointmentMiniSummary(appointment)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Lucide.CalendarDays, "Preferred date", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("Preferred Date", color = MaterialTheme.colorScheme.onSurface, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = { showCalendar = true }, enabled = !state.rescheduleSubmitting) {
                    Text("View Calendar", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(dates) { calendar ->
                    val value = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
                    val availability = state.rescheduleDates[value]
                    val available = availability is Resource.Success && availability.data.isNotEmpty()
                    val failed = availability is Resource.Error
                    val selected = date == value && available
                    Surface(
                        onClick = {
                            if (failed) { date = value; selectedSlot = null; viewModel.loadCalendarRescheduleDate(appointment, value) }
                            else { date = value; selectedSlot = null; viewModel.loadCalendarRescheduleDate(appointment, value) }
                        },
                        enabled = (available || failed) && !state.rescheduleSubmitting,
                        modifier = Modifier.widthIn(min = 76.dp).heightIn(min = 90.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = if (selected) BluePrimary else if (!available) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, if (selected) BluePrimary else MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            val textColor = if (selected) White else if (available) MaterialTheme.colorScheme.onSurface else ErrorRed
                            Text(SimpleDateFormat("EEE", Locale.US).format(calendar.time), color = textColor, fontSize = 12.sp)
                            Text(SimpleDateFormat("MMM d", Locale.US).format(calendar.time), color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            if (!available) Text(
                                when (availability) {
                                    is Resource.Success -> "Unavailable"
                                    is Resource.Error -> "Retry"
                                    else -> "Checking…"
                                }, color = ErrorRed, fontSize = 10.sp
                            )
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Lucide.Clock, "Preferred time", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp)); Spacer(Modifier.width(8.dp)); Text("Preferred Time", color = MaterialTheme.colorScheme.onSurface, fontSize = 17.sp, fontWeight = FontWeight.Bold) }
            when (val slots = state.rescheduleSlots) {
                Resource.Loading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                is Resource.Error -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(slots.message ?: "Unable to load availability.", color = ErrorRed, modifier = Modifier.weight(1f))
                    TextButton(onClick = { if (date.isNotBlank()) viewModel.loadCalendarRescheduleDate(appointment, date) }) { Text("Try again") }
                }
                is Resource.Success -> if (slots.data.isEmpty()) Text("No available times for this date.", color = MaterialTheme.colorScheme.onSurfaceVariant) else LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(slots.data) { slot -> FilterChip(selected = selectedSlot?.startTime == slot.startTime, onClick = { selectedSlot = slot }, enabled = !state.rescheduleSubmitting, label = { Text(slot.startTime?.let(DateUtils::formatDisplayTime) ?: "Time") }) } }
                Resource.Idle -> Text("Choose a date first.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedTextField(value = note, onValueChange = { if (it.length <= 200) note = it }, label = { Text("Add note") }, supportingText = { Text("${note.length}/200") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), minLines = 3)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(38.dp), enabled = !state.rescheduleSubmitting, shape = RoundedCornerShape(16.dp), border = BorderStroke(2.dp, Color(0xFF1689E8)), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1689E8))) { Text("Back", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                Button(onClick = { selectedSlot?.let { viewModel.requestReschedule(appointment, date, it, note) } }, modifier = Modifier.weight(1f).height(38.dp), enabled = date.isNotBlank() && selectedSlot != null && !state.rescheduleSubmitting, shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(contentColor = Color.White, containerColor = VibrantTeal)) { if (state.rescheduleSubmitting) CircularProgressIndicator(Modifier.size(18.dp), color = White) else Text(if (appointment.needsPatientScheduleChoice) "Submit Schedule" else "Send Request", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun AppointmentMiniSummary(appointment: AppointmentDTO) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.Top) {
            AppointmentAvatar(appointment, size = 64.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(appointment.doctor, color = MaterialTheme.colorScheme.onSurface, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(appointment.dentistSpecialty ?: "Dental care", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                SummaryMetaLine(Lucide.CalendarDays, "Appointment date", DateUtils.formatDisplayDate(appointment.appointmentDate))
                SummaryMetaLine(Lucide.Clock, "Appointment time", DateUtils.formatDisplayTime(appointment.startTime))
                SummaryMetaLine(Lucide.MapPin, "Appointment clinic", appointment.clinicName ?: "Rivera Dental Clinic")
            }
            Box(Modifier.padding(start = 6.dp)) { StatusBadge(statusStyle(mapAppointmentStatus(appointment.appointmentStatus))) }
        }
    }
}

@Composable
private fun SummaryMetaLine(icon: ImageVector, description: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, description, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
private fun SheetDragHandle() {
    Box(
        Modifier
            .padding(top = 10.dp, bottom = 2.dp)
            .size(width = 74.dp, height = 5.dp)
            .clip(RoundedCornerShape(50))
            .background(Color(0xFFB9C4CD))
    )
}

@Composable
internal fun AppointmentDetailsDialog(
    appointment: AppointmentDTO,
    onDismiss: () -> Unit,
    onCheckIn: (() -> Unit)? = null,
    onQueue: (() -> Unit)? = null,
    checkingIn: Boolean = false,
    checkedIn: Boolean = !appointment.checkedInAt.isNullOrBlank(),
) {
    val style = statusStyle(mapAppointmentStatus(appointment.appointmentStatus), appointment.needsPatientScheduleChoice)
    val status = mapAppointmentStatus(appointment.appointmentStatus)
    val isToday = DateUtils.appointmentDateOnly(appointment.appointmentDate) ==
        java.time.LocalDate.now(BookingRules.clinicZone).toString()
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 20.dp,
        ) {
            Column {
                Box(
                    Modifier.fillMaxWidth().background(
                        Brush.linearGradient(
                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.82f))
                        )
                    ).padding(horizontal = 18.dp, vertical = 15.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AppointmentAvatar(appointment, size = 54.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("APPOINTMENT DETAILS", color = Color.White.copy(alpha = 0.72f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Text(appointment.doctor, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text(appointment.dentistSpecialty ?: "Dental care", color = Color.White.copy(alpha = 0.82f), fontSize = 13.sp)
                            }
                            IconButton(onClick = onDismiss, modifier = Modifier.size(38.dp)) {
                                Icon(Lucide.X, "Close appointment details", tint = Color.White)
                            }
                        }
                        Surface(color = Color.White.copy(alpha = 0.14f), shape = RoundedCornerShape(14.dp)) {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(style.icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(9.dp))
                                Column {
                                    Text(style.label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(style.message, color = Color.White.copy(alpha = 0.82f), fontSize = 11.sp, lineHeight = 14.sp, maxLines = 1)
                                }
                            }
                        }
                    }
                }

                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Visit information", color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    AppointmentDetailTile(Lucide.CalendarDays, "Date", DateUtils.formatDisplayDate(appointment.appointmentDate))
                    AppointmentDetailTile(Lucide.Clock, "Time", "${DateUtils.formatDisplayTime(appointment.startTime)} – ${DateUtils.formatDisplayTime(appointment.endTime)}")
                    AppointmentDetailTile(dentalServiceIcon(appointment.serviceName ?: appointment.treatment), "Service", appointment.serviceName ?: appointment.treatment)
                    AppointmentDetailTile(Lucide.MapPin, "Clinic", appointment.clinicName ?: "Rivera Dental Clinic")
                    appointment.price?.let { AppointmentDetailTile(Lucide.Tag, "Price", "₱${String.format(Locale.US, "%,.0f", it)}") }

                    AppointmentDetailTile(Lucide.Tag, "Reference", "#${appointment.appointmentId}")

                    if (isToday && (status == AppointmentStatus.CONFIRMED || status == AppointmentStatus.IN_PROGRESS)) {
                        HorizontalDivider(Modifier.padding(top = 2.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        Text("Today's visit", color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (status == AppointmentStatus.CONFIRMED && onCheckIn != null) {
                                Box(Modifier.weight(1f).height(46.dp)) {
                                androidx.compose.animation.AnimatedVisibility(visible = checkedIn, enter = fadeIn() + scaleIn(initialScale = 0.78f)) {
                                    Surface(
                                        modifier = Modifier.fillMaxSize(),
                                        shape = RoundedCornerShape(15.dp),
                                        color = Color(0xFFE8F7EF),
                                        border = BorderStroke(1.dp, Color(0xFF74C69D)),
                                    ) {
                                        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                            Icon(Lucide.CircleCheck, null, tint = Color(0xFF168354), modifier = Modifier.size(19.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Checked In", color = Color(0xFF168354), fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                                        }
                                    }
                                }
                                if (!checkedIn) {
                                Button(
                                    onClick = onCheckIn,
                                    enabled = !checkingIn,
                                    modifier = Modifier.fillMaxSize(),
                                    shape = RoundedCornerShape(15.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = VibrantTeal),
                                ) {
                                    if (checkingIn) CircularProgressIndicator(Modifier.size(19.dp), color = Color.White, strokeWidth = 2.dp)
                                    else { Icon(Lucide.CircleCheck, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Check In", fontWeight = FontWeight.Bold, maxLines = 1) }
                                }
                                }
                                }
                            }
                            if (onQueue != null) {
                                OutlinedButton(
                                    onClick = onQueue,
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    shape = RoundedCornerShape(15.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                ) {
                                    Icon(Lucide.Clock, null, Modifier.size(18.dp)); Spacer(Modifier.width(5.dp)); Text("View Queue", fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppointmentDetailTile(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), shape = RoundedCornerShape(11.dp)) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(8.dp).size(18.dp))
            }
            Spacer(Modifier.width(11.dp))
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.width(68.dp))
            Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, modifier = Modifier.weight(1f))
        }
    }
}

@Composable private fun EmptyAppointments() { Column(Modifier.fillMaxWidth().padding(vertical = 44.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) { Icon(Lucide.CalendarCheck, "No appointments", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp)); Text("No appointments in this tab", color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text("Book a visit when you are ready.", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
@Composable private fun LoadingState() {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(3) {
            Box(Modifier.fillMaxWidth().height(210.dp).clip(AppointmentCardShape).shimmer())
        }
    }
}
@Composable private fun ErrorState(message: String, retry: () -> Unit) { Column(Modifier.fillMaxWidth().padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(message, color = ErrorRed); TextButton(onClick = retry) { Text("Retry") } } }
