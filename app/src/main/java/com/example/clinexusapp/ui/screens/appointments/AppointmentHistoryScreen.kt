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


import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
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
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

private val AppointmentCardShape = RoundedCornerShape(18.dp)

private data class StatusStyle(val label: String, val icon: ImageVector, val foreground: Color, val background: Color, val message: String)

private fun statusStyle(status: AppointmentStatus, needsPatientChoice: Boolean = false): StatusStyle = when (status) {
    AppointmentStatus.PENDING -> StatusStyle("Pending", Lucide.Clock, Color(0xFFD97706), Color(0xFFFFF7E6), "Waiting for confirmation from the clinic.")
    AppointmentStatus.CONFIRMED -> StatusStyle("Confirmed", Lucide.CircleCheck, Color(0xFF15803D), Color(0xFFECFDF3), "Your appointment is confirmed.")
    AppointmentStatus.COMPLETED -> StatusStyle("Completed", Lucide.CircleCheck, Color(0xFF2563EB), Color(0xFFEFF6FF), "This appointment has been completed.")
    AppointmentStatus.CANCELLED -> StatusStyle("Cancelled", Lucide.CircleX, Color(0xFFDC2626), Color(0xFFFFF2F2), "This appointment was cancelled.")
    AppointmentStatus.RESCHEDULE_REQUESTED -> if (needsPatientChoice) {
        StatusStyle("Action needed", Lucide.CalendarSync, Color(0xFF7C3AED), Color(0xFFF5F3FF), "The clinic requested a new schedule. Choose another available date and time.")
    } else {
        StatusStyle("Reschedule", Lucide.CalendarSync, Color(0xFF7C3AED), Color(0xFFF5F3FF), "Your selected schedule is waiting for clinic approval.")
    }
    AppointmentStatus.CANCELLATION_REQUESTED -> StatusStyle("Cancellation", Lucide.CircleX, Color(0xFFDC2626), Color(0xFFFFF2F2), "Your cancellation request is being processed.")
    AppointmentStatus.UNKNOWN -> StatusStyle("Status unavailable", Lucide.CircleQuestionMark, SlateGray, Color(0xFFF1F5F9), "This appointment has an unrecognized status.")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AppointmentHistoryScreen(onBack: () -> Unit, onNavigateToBooking: () -> Unit, viewModel: HistoryViewModel) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    var selected by remember { mutableStateOf<AppointmentDTO?>(null) }
    var cancelTarget by remember { mutableStateOf<AppointmentDTO?>(null) }
    var rescheduleTarget by remember { mutableStateOf<AppointmentDTO?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
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
    if (selected != null) AppointmentDetailsDialog(selected!!, onDismiss = { selected = null })

    Scaffold(containerColor = SoftMist) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp, 8.dp, 20.dp, 120.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            stickyHeader {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SoftMist)
                        .padding(bottom = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AppointmentHeader(onBack, onNavigateToBooking)
                    AppointmentTabs(state, viewModel)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${tabLabel(state.selectedTab)} Appointments", color = RoyalNavy, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { viewModel.selectTab(state.selectedTab) }) {
                            Text("See All", color = DeepTeal, fontSize = 15.sp)
                        }
                    }
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
                        AppointmentCard(appointment, onClick = { selected = appointment }, onCancel = { cancelTarget = appointment }, onReschedule = { rescheduleTarget = appointment }, onBook = onNavigateToBooking, onAddToCalendar = { addAppointmentToCalendar(context, appointment) })
                    }
                }
            }
        }
    }
}

@Composable
private fun AppointmentErrorDialog(message: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = White,
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
                    "Schedule not submitted",
                    color = RoyalNavy,
                    fontSize = 22.sp,
                    lineHeight = 27.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    message,
                    color = SlateGray,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VibrantTeal)
                ) {
                    Text("Choose Another Time", fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
    onNavigateToBooking: () -> Unit
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
                tint = RoyalNavy,
                modifier = Modifier.size(25.dp)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
        ) {
            Text(
                "My Appointments",
                color = RoyalNavy,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Manage your visits and schedules",
                color = SlateGray,
                fontSize = 13.sp
            )
        }

        Surface(
            onClick = onNavigateToBooking,
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            color = MintSparkle
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Lucide.CalendarDays,
                    contentDescription = "Book appointment",
                    tint = DeepTeal,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}

@Composable
private fun AppointmentTabs(
    state: HistoryUiState,
    viewModel: HistoryViewModel
) {
    val appointments =
        (state.appointments as? Resource.Success)?.data.orEmpty()

    Surface(
        color = Color(0xFFEAF3F4),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            AppointmentTab.values().forEach { tab ->

                val count = appointments.count {
                    mapAppointmentStatus(it.appointmentStatus).toTab() == tab
                }

                val selected = state.selectedTab == tab

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable {
                            viewModel.selectTab(tab)
                        },
                    shape = RoundedCornerShape(19.dp),
                    color = if (selected) VibrantTeal else Color.Transparent
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${tabLabel(tab)} ($count)",
                            color = if (selected) White else SlateGray,
                            fontSize = 12.sp,
                            fontWeight = if (selected)
                                FontWeight.Bold
                            else
                                FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

private fun tabLabel(tab: AppointmentTab) = when (tab) { AppointmentTab.PENDING -> "Pending"; AppointmentTab.CONFIRMED -> "Confirmed"; AppointmentTab.COMPLETED -> "Completed"; AppointmentTab.CANCELLED -> "Cancelled" }

@Composable
private fun AppointmentCard(appointment: AppointmentDTO, onClick: () -> Unit, onCancel: () -> Unit, onReschedule: () -> Unit, onBook: () -> Unit, onAddToCalendar: () -> Unit) {
    val status = mapAppointmentStatus(appointment.appointmentStatus)
    val style = statusStyle(status, appointment.needsPatientScheduleChoice)
    Surface(
        onClick = onClick,
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = "View appointment details for ${appointment.doctor}"
            role = Role.Button
        },
        color = White,
        shape = AppointmentCardShape,
        border = BorderStroke(1.dp, Color(0xFFE3ECEE))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppointmentAvatar(appointment)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(appointment.doctor, color = RoyalNavy, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(appointment.dentistSpecialty ?: "Dental care", color = SlateGray, fontSize = 14.sp)
                }
                StatusBadge(style)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Lucide.CalendarDays, "Appointment date", tint = DeepTeal, modifier = Modifier.size(22.dp))
                Text(DateUtils.formatDisplayDate(appointment.appointmentDate), color = SlateGray, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp))
                Spacer(Modifier.width(14.dp))
                Box(Modifier.width(1.dp).height(22.dp).background(Color(0xFFD5E1E3)))
                Spacer(Modifier.width(14.dp))
                Icon(Lucide.Clock, "Appointment time", tint = DeepTeal, modifier = Modifier.size(22.dp))
                Text(DateUtils.formatDisplayTime(appointment.startTime), color = SlateGray, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp))
            }
            Text("${appointment.serviceName ?: appointment.treatment}  •  ${appointment.clinicName ?: "Clinexus Dental Clinic"}", color = SlateGray, fontSize = 13.sp)
            Surface(color = style.background, shape = RoundedCornerShape(12.dp)) { Text(style.message, color = style.foreground, fontSize = 13.sp, modifier = Modifier.padding(12.dp)) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                when (status) {

                    AppointmentStatus.PENDING -> {
                        OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f).height(38.dp), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, ErrorRed), colors = ButtonDefaults.outlinedButtonColors(containerColor = White, contentColor = ErrorRed)) {
                            Icon(Lucide.CircleX, "Cancel appointment", modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Cancel", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(onClick = onReschedule, modifier = Modifier.weight(1f).height(38.dp), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, DeepTeal), colors = ButtonDefaults.outlinedButtonColors(containerColor = White, contentColor = DeepTeal)) {
                            Icon(Lucide.CalendarSync, "Reschedule appointment", modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Reschedule", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    AppointmentStatus.CONFIRMED -> {
                        OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f).height(38.dp), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, ErrorRed), colors = ButtonDefaults.outlinedButtonColors(containerColor = White, contentColor = ErrorRed)) {
                            Icon(Lucide.CircleX, "Cancel appointment", modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Cancel", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(onClick = onReschedule, modifier = Modifier.weight(1f).height(38.dp), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, DeepTeal), colors = ButtonDefaults.outlinedButtonColors(containerColor = White, contentColor = DeepTeal)) {
                            Icon(Lucide.CalendarSync, "Reschedule appointment", modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Reschedule", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    AppointmentStatus.CANCELLED ->
                        OutlinedButton(onClick = onBook, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, DeepTeal), colors = ButtonDefaults.outlinedButtonColors(containerColor = White, contentColor = DeepTeal)) {
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
                    AppointmentStatus.CANCELLATION_REQUESTED,
                    AppointmentStatus.UNKNOWN -> Unit
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

private fun addAppointmentToCalendar(context: android.content.Context, appointment: AppointmentDTO) {
    val parser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    val date = appointment.appointmentDate.substringBefore('T')
    val start = parser.parse("$date ${appointment.startTime}")?.time ?: return
    val end = parser.parse("$date ${appointment.endTime}")?.time ?: (start + 60 * 60 * 1000)
    val intent = Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end)
        .putExtra(CalendarContract.Events.TITLE, "Dental appointment – ${appointment.doctor}")
        .putExtra(CalendarContract.Events.DESCRIPTION, appointment.serviceName ?: appointment.treatment)
        .putExtra(CalendarContract.Events.EVENT_LOCATION, appointment.clinicName ?: "Clinexus Dental Clinic")
    context.startActivity(intent)
}

@Composable
private fun StatusBadge(style: StatusStyle) { Surface(color = style.background, shape = RoundedCornerShape(20.dp)) { Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) { Icon(style.icon, style.label, tint = style.foreground, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text(style.label.uppercase(), color = style.foreground, fontSize = 11.sp, fontWeight = FontWeight.Bold) } } }

@Composable
private fun AppointmentAvatar(appointment: AppointmentDTO, size: androidx.compose.ui.unit.Dp = 52.dp) {
    var imageFailed by remember(appointment.dentistProfileImage) { mutableStateOf(false) }
    if (appointment.dentistProfileImage.isNullOrBlank() || imageFailed) {
        Box(Modifier.size(size).clip(CircleShape).background(MintSparkle), contentAlignment = Alignment.Center) { Icon(Lucide.UserRound, "Dentist profile", tint = DeepTeal, modifier = Modifier.size(size * 0.55f)) }
    } else AsyncImage(model = appointment.dentistProfileImage, contentDescription = "Dentist profile image", modifier = Modifier.size(size).clip(CircleShape), onError = { imageFailed = true })
}

@Composable
private fun DetailLine(icon: ImageVector, label: String, value: String) { Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) { Icon(icon, label, tint = DeepTeal, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(10.dp)); Text("$label: ", color = SlateGray, fontSize = 13.sp); Text(value, color = RoyalNavy, fontSize = 14.sp, fontWeight = FontWeight.Medium) } }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CancellationSheet(appointment: AppointmentDTO, submitting: Boolean, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var reason by remember { mutableStateOf<String?>(null) }
    var details by remember { mutableStateOf("") }
    val other = reason == "Other"
    val valid = !reason.isNullOrBlank() && (!other || details.isNotBlank())
    ModalBottomSheet(
        onDismissRequest = { if (details.isBlank()) onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = White,
        scrimColor = Color(0x990B1820),
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        dragHandle = { SheetDragHandle() }
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 24.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Cancel visit", color = RoyalNavy, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text("Tell us why you want to cancel this appointment.", color = SlateGray, fontSize = 17.sp, lineHeight = 23.sp)
            AppointmentMiniSummary(appointment)
            listOf("Schedule conflict", "Feeling unwell", "Need a different time", "Other").forEach { option ->
                Surface(onClick = { reason = option }, color = if (reason == option) MintSparkle else White, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, if (reason == option) VibrantTeal else Color(0xFFD8E2E6))) { Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) { RadioButton(reason == option, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = DeepTeal, unselectedColor = SlateGray)); Text(option, color = RoyalNavy, fontSize = 15.sp) } }
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
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(38.dp), enabled = !submitting, shape = RoundedCornerShape(16.dp), border = BorderStroke(2.dp, DeepTeal), contentPadding = PaddingValues(horizontal = 6.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepTeal)) { Text("Keep Appointment", fontSize = 13.sp, maxLines = 1, softWrap = false, fontWeight = FontWeight.Bold) }
                Button(onClick = { onConfirm(if (other) details else reason.orEmpty()) }, modifier = Modifier.weight(1f).height(38.dp), enabled = valid && !submitting, shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(horizontal = 6.dp), colors = ButtonDefaults.buttonColors(containerColor = ErrorRed, disabledContainerColor = Color(0xFFE7B7B7))) { if (submitting) CircularProgressIndicator(Modifier.size(18.dp), color = White) else Text("Confirm Cancellation", fontSize = 13.sp, maxLines = 1, softWrap = false, fontWeight = FontWeight.Bold) }
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
    val dates = remember { (1..14).map { Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, it) } } }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = White,
        scrimColor = Color(0x990B1820),
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        dragHandle = { SheetDragHandle() }
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 24.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(if (appointment.needsPatientScheduleChoice) "Choose New Schedule" else "Request Reschedule", color = RoyalNavy, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(
                if (appointment.needsPatientScheduleChoice) "The clinic asked you to reschedule. Choose an available date and time for approval." else "Choose your preferred new schedule and we will send your request to the clinic.",
                color = SlateGray,
                fontSize = 16.sp,
                lineHeight = 22.sp
            )
            AppointmentMiniSummary(appointment)
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Lucide.CalendarDays, "Preferred date", tint = DeepTeal, modifier = Modifier.size(22.dp)); Spacer(Modifier.width(8.dp)); Text("Preferred Date", color = RoyalNavy, fontSize = 17.sp, fontWeight = FontWeight.Bold) }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(dates) { calendar -> val value = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time); FilterChip(selected = date == value, onClick = { date = value; selectedSlot = null; viewModel.loadRescheduleSlots(appointment, value) }, label = { Text(SimpleDateFormat("MMM d", Locale.US).format(calendar.time)) }) } }
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Lucide.Clock, "Preferred time", tint = DeepTeal, modifier = Modifier.size(22.dp)); Spacer(Modifier.width(8.dp)); Text("Preferred Time", color = RoyalNavy, fontSize = 17.sp, fontWeight = FontWeight.Bold) }
            when (val slots = state.rescheduleSlots) {
                Resource.Loading -> CircularProgressIndicator(color = VibrantTeal)
                is Resource.Error -> Text(slots.message ?: "Unable to load availability.", color = ErrorRed)
                is Resource.Success -> if (slots.data.isEmpty()) Text("No available times for this date.", color = SlateGray) else LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(slots.data) { slot -> FilterChip(selected = selectedSlot?.startTime == slot.startTime, onClick = { selectedSlot = slot }, label = { Text(slot.label ?: slot.startTime ?: "Time") }) } }
                Resource.Idle -> Text("Choose a date first.", color = SlateGray)
            }
            OutlinedTextField(value = note, onValueChange = { if (it.length <= 200) note = it }, label = { Text("Add note (optional)") }, supportingText = { Text("${note.length}/200") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), minLines = 3)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(38.dp), enabled = !state.rescheduleSubmitting, shape = RoundedCornerShape(16.dp), border = BorderStroke(2.dp, Color(0xFF1689E8)), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1689E8))) { Text("Back", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                Button(onClick = { selectedSlot?.let { viewModel.requestReschedule(appointment, date, it, note) } }, modifier = Modifier.weight(1f).height(38.dp), enabled = date.isNotBlank() && selectedSlot != null && !state.rescheduleSubmitting, shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = VibrantTeal)) { if (state.rescheduleSubmitting) CircularProgressIndicator(Modifier.size(18.dp), color = White) else Text(if (appointment.needsPatientScheduleChoice) "Submit Schedule" else "Send Request", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun AppointmentMiniSummary(appointment: AppointmentDTO) {
    Surface(color = Color(0xFFF9FCFC), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color(0xFFE4EEEF))) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.Top) {
            AppointmentAvatar(appointment, size = 64.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(appointment.doctor, color = RoyalNavy, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(appointment.dentistSpecialty ?: "Dental care", color = SlateGray, fontSize = 14.sp)
                SummaryMetaLine(Lucide.CalendarDays, "Appointment date", DateUtils.formatDisplayDate(appointment.appointmentDate))
                SummaryMetaLine(Lucide.Clock, "Appointment time", DateUtils.formatDisplayTime(appointment.startTime))
                SummaryMetaLine(Lucide.MapPin, "Appointment clinic", appointment.clinicName ?: "Clinexus Dental Clinic")
            }
            Box(Modifier.padding(start = 6.dp)) { StatusBadge(statusStyle(mapAppointmentStatus(appointment.appointmentStatus))) }
        }
    }
}

@Composable
private fun SummaryMetaLine(icon: ImageVector, description: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, description, tint = DeepTeal, modifier = Modifier.size(18.dp))
        Text(value, color = SlateGray, fontSize = 12.sp, maxLines = 1)
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
private fun AppointmentDetailsDialog(appointment: AppointmentDTO, onDismiss: () -> Unit) {
    val style = statusStyle(mapAppointmentStatus(appointment.appointmentStatus), appointment.needsPatientScheduleChoice)
    AlertDialog(onDismissRequest = onDismiss, confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }, title = { Text("Appointment details", color = RoyalNavy, fontWeight = FontWeight.Bold) }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { AppointmentAvatar(appointment, size = 56.dp); Spacer(Modifier.width(12.dp)); Column { Text(appointment.doctor, color = RoyalNavy, fontSize = 17.sp, fontWeight = FontWeight.Bold); Text(appointment.dentistSpecialty ?: "Dental care", color = SlateGray, fontSize = 14.sp) } }; StatusBadge(style); DetailLine(dentalServiceIcon(appointment.serviceName ?: appointment.treatment), "Service", appointment.serviceName ?: appointment.treatment); appointment.price?.let { DetailLine(Lucide.Tag, "Price", "₱${String.format(Locale.US, "%,.0f", it)}") }; DetailLine(Lucide.CalendarDays, "Date", DateUtils.formatDisplayDate(appointment.appointmentDate)); DetailLine(Lucide.Clock, "Time", "${DateUtils.formatDisplayTime(appointment.startTime)} – ${DateUtils.formatDisplayTime(appointment.endTime)}"); DetailLine(Lucide.MapPin, "Clinic", appointment.clinicName ?: "Clinexus Dental Clinic"); Text("Booking reference: #${appointment.appointmentId}", color = SlateGray, fontSize = 13.sp); appointment.submittedAt?.let { Text("Submitted: ${DateUtils.formatDisplayDate(it)}", color = SlateGray, fontSize = 13.sp) }; appointment.cancelledBy?.let { Text("Cancelled by: $it", color = ErrorRed, fontSize = 13.sp) }; appointment.cancellationReason?.let { Text("Cancellation reason: $it", color = ErrorRed, fontSize = 13.sp) }; appointment.rescheduleNote?.let { Text("Reschedule note: $it", color = Color(0xFF7C3AED), fontSize = 13.sp) } } })
}

@Composable private fun EmptyAppointments() { Column(Modifier.fillMaxWidth().padding(vertical = 44.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) { Icon(Lucide.CalendarCheck, "No appointments", tint = TealMuted, modifier = Modifier.size(56.dp)); Text("No appointments in this tab", color = RoyalNavy, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text("Book a visit when you are ready.", color = SlateGray) } }
@Composable private fun LoadingState() {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(3) {
            Box(Modifier.fillMaxWidth().height(210.dp).clip(AppointmentCardShape).shimmer())
        }
    }
}
@Composable private fun ErrorState(message: String, retry: () -> Unit) { Column(Modifier.fillMaxWidth().padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(message, color = ErrorRed); TextButton(onClick = retry) { Text("Retry") } } }
