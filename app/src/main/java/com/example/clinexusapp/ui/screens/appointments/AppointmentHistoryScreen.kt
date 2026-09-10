package com.example.clinexusapp.ui.screens.appointments

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.clinexusapp.model.AppointmentDTO
import com.example.clinexusapp.model.AvailableSlotDTO
import com.example.clinexusapp.ui.theme.*
import com.example.clinexusapp.util.DateUtils
import com.example.clinexusapp.util.Resource
import com.example.clinexusapp.viewmodel.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val AppointmentCardShape = RoundedCornerShape(18.dp)

private data class StatusStyle(val label: String, val icon: ImageVector, val foreground: Color, val background: Color, val message: String)

private fun statusStyle(status: AppointmentStatus): StatusStyle = when (status) {
    AppointmentStatus.PENDING -> StatusStyle("Pending", Icons.Default.Schedule, Color(0xFFD97706), Color(0xFFFFF7E6), "Waiting for confirmation from the clinic.")
    AppointmentStatus.CONFIRMED -> StatusStyle("Confirmed", Icons.Default.CheckCircle, Color(0xFF15803D), Color(0xFFECFDF3), "Your appointment is confirmed.")
    AppointmentStatus.COMPLETED -> StatusStyle("Completed", Icons.Default.TaskAlt, Color(0xFF2563EB), Color(0xFFEFF6FF), "This appointment has been completed.")
    AppointmentStatus.CANCELLED -> StatusStyle("Cancelled", Icons.Default.Cancel, Color(0xFFDC2626), Color(0xFFFFF2F2), "This appointment was cancelled.")
    AppointmentStatus.RESCHEDULE_REQUESTED -> StatusStyle("Reschedule requested", Icons.Default.EventRepeat, Color(0xFF7C3AED), Color(0xFFF5F3FF), "Your reschedule request is waiting for clinic approval.")
    AppointmentStatus.UNKNOWN -> StatusStyle("Status unavailable", Icons.AutoMirrored.Filled.HelpOutline, SlateGray, Color(0xFFF1F5F9), "This appointment has an unrecognized status.")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentHistoryScreen(onBack: () -> Unit, onNavigateToBooking: () -> Unit, viewModel: HistoryViewModel) {
    val state by viewModel.uiState.collectAsState()
    var selected by remember { mutableStateOf<AppointmentDTO?>(null) }
    var cancelTarget by remember { mutableStateOf<AppointmentDTO?>(null) }
    var rescheduleTarget by remember { mutableStateOf<AppointmentDTO?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.operation) {
        when (val operation = state.operation) {
            is Resource.Success -> {
                snackbar.showSnackbar(operation.data)
                if (cancelTarget != null || rescheduleTarget != null) {
                    cancelTarget = null
                    rescheduleTarget = null
                }
                viewModel.clearOperation()
            }
            is Resource.Error -> snackbar.showSnackbar(operation.message ?: "Request failed. Please try again.")
            else -> Unit
        }
    }

    if (cancelTarget != null) {
        CancellationSheet(cancelTarget!!, state.cancellationSubmitting, onDismiss = { cancelTarget = null }, onConfirm = { reason -> viewModel.cancelAppointment(cancelTarget!!.appointmentId, reason) })
    }
    if (rescheduleTarget != null) {
        RescheduleSheet(rescheduleTarget!!, state, viewModel, onDismiss = { rescheduleTarget = null })
    }
    if (selected != null) AppointmentDetailsDialog(selected!!, onDismiss = { selected = null })

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, containerColor = SoftMist) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp, 8.dp, 20.dp, 120.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                AppointmentHeader(onBack, onNavigateToBooking)
            }
            item { AppointmentTabs(state, viewModel) }
            item {
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${tabLabel(state.selectedTab)} Appointments", color = RoyalNavy, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { viewModel.selectTab(state.selectedTab) }) { Text("See All", color = DeepTeal, fontSize = 15.sp) }
                }
            }
            when (val appointments = state.appointments) {
                Resource.Idle, Resource.Loading -> item { LoadingState() }
                is Resource.Error -> item { ErrorState(appointments.message ?: "Unable to load appointments.", viewModel::fetchHistory) }
                is Resource.Success -> {
                    val visible = viewModel.filteredAppointments(state.selectedTab, appointments.data)
                    if (visible.isEmpty()) item { EmptyAppointments(onNavigateToBooking) }
                    else items(visible, key = { it.appointmentId }) { appointment ->
                        AppointmentCard(appointment, onClick = { selected = appointment }, onCancel = { cancelTarget = appointment }, onReschedule = { rescheduleTarget = appointment }, onBook = onNavigateToBooking)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppointmentHeader(onBack: () -> Unit, onNavigateToBooking: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = RoyalNavy, modifier = Modifier.size(28.dp)) }
        Column(Modifier.weight(1f).padding(start = 4.dp)) {
            Text("My Appointments", color = RoyalNavy, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Manage your visits and schedules", color = SlateGray, fontSize = 15.sp)
        }
        Surface(onClick = onNavigateToBooking, modifier = Modifier.size(56.dp), shape = CircleShape, color = MintSparkle) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.EventAvailable, "Book appointment", tint = DeepTeal, modifier = Modifier.size(30.dp)) }
        }
    }
}

@Composable
private fun AppointmentTabs(state: HistoryUiState, viewModel: HistoryViewModel) {
    val appointments = (state.appointments as? Resource.Success)?.data.orEmpty()
    Surface(color = Color(0xFFEAF3F4), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(4.dp).background(Color.Transparent).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            AppointmentTab.values().forEach { tab ->
                val count = appointments.count { mapAppointmentStatus(it.appointmentStatus).toTab() == tab }
                Surface(onClick = { viewModel.selectTab(tab) }, modifier = Modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(19.dp), color = if (state.selectedTab == tab) VibrantTeal else Color.Transparent) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) { Text("${tabLabel(tab)} ($count)", color = if (state.selectedTab == tab) White else SlateGray, fontSize = 12.sp, fontWeight = if (state.selectedTab == tab) FontWeight.Bold else FontWeight.Normal, maxLines = 1) }
                }
            }
        }
    }
}

private fun tabLabel(tab: AppointmentTab) = when (tab) { AppointmentTab.PENDING -> "Pending"; AppointmentTab.CONFIRMED -> "Confirmed"; AppointmentTab.COMPLETED -> "Completed"; AppointmentTab.CANCELLED -> "Cancelled" }

@Composable
private fun AppointmentCard(appointment: AppointmentDTO, onClick: () -> Unit, onCancel: () -> Unit, onReschedule: () -> Unit, onBook: () -> Unit) {
    val status = mapAppointmentStatus(appointment.appointmentStatus)
    val style = statusStyle(status)
    Surface(
        color = White,
        shape = AppointmentCardShape,
        border = BorderStroke(1.dp, Color(0xFFE3ECEE))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Clickable header area for details
            Column(
                modifier = Modifier.clickable { onClick() }.semantics(mergeDescendants = true) {
                    contentDescription = "View appointment details for ${appointment.doctor}"
                    role = Role.Button
                },
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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
                    Icon(Icons.Default.CalendarMonth, "Appointment date", tint = DeepTeal, modifier = Modifier.size(22.dp))
                    Text(DateUtils.formatDisplayDate(appointment.appointmentDate), color = SlateGray, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp))
                    Spacer(Modifier.width(14.dp))
                    Box(Modifier.width(1.dp).height(22.dp).background(Color(0xFFD5E1E3)))
                    Spacer(Modifier.width(14.dp))
                    Icon(Icons.Default.AccessTime, "Appointment time", tint = DeepTeal, modifier = Modifier.size(22.dp))
                    Text(DateUtils.formatDisplayTime(appointment.startTime), color = SlateGray, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp))
                }
                Text("${appointment.serviceName ?: appointment.treatment}  •  ${appointment.clinicName ?: "Clinexus Dental Clinic"}", color = SlateGray, fontSize = 13.sp)
                Surface(color = style.background, shape = RoundedCornerShape(12.dp)) { Text(style.message, color = style.foreground, fontSize = 13.sp, modifier = Modifier.padding(12.dp)) }
            }
            
            val cardButtonsLargeFont = androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.2f
            if (cardButtonsLargeFont) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    when (status) {
                        AppointmentStatus.PENDING -> {
                            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)) { Icon(Icons.Default.Cancel, "Cancel appointment", modifier = Modifier.size(20.dp)); Spacer(Modifier.width(4.dp)); Text("Cancel") }
                            OutlinedButton(onClick = onReschedule, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepTeal)) { Icon(Icons.Default.EventRepeat, "Request reschedule", modifier = Modifier.size(20.dp)); Spacer(Modifier.width(4.dp)); Text("Reschedule") }
                        }
                        AppointmentStatus.CONFIRMED -> {
                            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)) { Icon(Icons.Default.Cancel, "Cancel appointment"); Spacer(Modifier.width(4.dp)); Text("Cancel") }
                            OutlinedButton(onClick = onReschedule, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepTeal)) { Icon(Icons.Default.EventRepeat, "Request reschedule"); Spacer(Modifier.width(4.dp)); Text("Reschedule") }
                        }
                        AppointmentStatus.CANCELLED -> OutlinedButton(onClick = onBook, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepTeal)) { Text("Book again") }
                        AppointmentStatus.COMPLETED, AppointmentStatus.RESCHEDULE_REQUESTED, AppointmentStatus.UNKNOWN -> Unit
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    when (status) {
                        AppointmentStatus.PENDING -> {
                            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)) { Icon(Icons.Default.Cancel, "Cancel appointment", modifier = Modifier.size(20.dp)); Spacer(Modifier.width(4.dp)); Text("Cancel") }
                            OutlinedButton(onClick = onReschedule, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepTeal)) { Icon(Icons.Default.EventRepeat, "Request reschedule", modifier = Modifier.size(20.dp)); Spacer(Modifier.width(4.dp)); Text("Reschedule") }
                        }
                        AppointmentStatus.CONFIRMED -> {
                            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)) { Icon(Icons.Default.Cancel, "Cancel appointment"); Spacer(Modifier.width(4.dp)); Text("Cancel") }
                            OutlinedButton(onClick = onReschedule, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepTeal)) { Icon(Icons.Default.EventRepeat, "Request reschedule"); Spacer(Modifier.width(4.dp)); Text("Reschedule") }
                        }
                        AppointmentStatus.CANCELLED -> OutlinedButton(onClick = onBook, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepTeal)) { Text("Book again") }
                        AppointmentStatus.COMPLETED, AppointmentStatus.RESCHEDULE_REQUESTED, AppointmentStatus.UNKNOWN -> Unit
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(style: StatusStyle) { Surface(color = style.background, shape = RoundedCornerShape(20.dp)) { Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) { Icon(style.icon, style.label, tint = style.foreground, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text(style.label.uppercase(), color = style.foreground, fontSize = 11.sp, fontWeight = FontWeight.Bold) } } }

@Composable
private fun AppointmentAvatar(appointment: AppointmentDTO, size: androidx.compose.ui.unit.Dp = 52.dp) {
    var imageFailed by remember(appointment.dentistProfileImage) { mutableStateOf(false) }
    if (appointment.dentistProfileImage.isNullOrBlank() || imageFailed) {
        Box(Modifier.size(size).clip(CircleShape).background(MintSparkle), contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, "Dentist profile", tint = DeepTeal, modifier = Modifier.size(size * 0.55f)) }
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
        Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding()) {
            Column(Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                            Icon(Icons.Default.PriorityHigh, "Reschedule information", tint = White, modifier = Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("You can also reschedule instead", color = Color(0xFF15558C), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("If you still need a visit, you can choose a new date and time.", color = Color(0xFF3976A8), fontSize = 13.sp)
                        }
                    }
                }
            }
            Box(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
                val isLargeFont = androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.2f
                if (isLargeFont) {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), enabled = !submitting, shape = RoundedCornerShape(16.dp), border = BorderStroke(2.dp, DeepTeal), colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepTeal)) { Text("Keep Appointment", fontSize = 13.sp, maxLines = 1, softWrap = false, fontWeight = FontWeight.Bold) }
                        Button(onClick = { onConfirm(if (other) details else reason.orEmpty()) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), enabled = valid && !submitting, shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = ErrorRed, disabledContainerColor = Color(0xFFE7B7B7))) { if (submitting) CircularProgressIndicator(Modifier.size(18.dp), color = White) else Text("Confirm Cancellation", fontSize = 13.sp, maxLines = 1, softWrap = false, fontWeight = FontWeight.Bold) }
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).heightIn(min = 56.dp), enabled = !submitting, shape = RoundedCornerShape(16.dp), border = BorderStroke(2.dp, DeepTeal), contentPadding = PaddingValues(horizontal = 6.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepTeal)) { Text("Keep Appointment", fontSize = 13.sp, maxLines = 1, softWrap = false, fontWeight = FontWeight.Bold) }
                        Button(onClick = { onConfirm(if (other) details else reason.orEmpty()) }, modifier = Modifier.weight(1f).heightIn(min = 56.dp), enabled = valid && !submitting, shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(horizontal = 6.dp), colors = ButtonDefaults.buttonColors(containerColor = ErrorRed, disabledContainerColor = Color(0xFFE7B7B7))) { if (submitting) CircularProgressIndicator(Modifier.size(18.dp), color = White) else Text("Confirm Cancellation", fontSize = 13.sp, maxLines = 1, softWrap = false, fontWeight = FontWeight.Bold) }
                    }
                }
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
        Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding()) {
            Column(Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Request Reschedule", color = RoyalNavy, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("Choose your preferred new schedule and we will send your request to the clinic.", color = SlateGray, fontSize = 16.sp, lineHeight = 22.sp)
                AppointmentMiniSummary(appointment)
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.CalendarMonth, "Preferred date", tint = DeepTeal, modifier = Modifier.size(22.dp)); Spacer(Modifier.width(8.dp)); Text("Preferred Date", color = RoyalNavy, fontSize = 17.sp, fontWeight = FontWeight.Bold) }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(dates) { calendar -> val value = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time); FilterChip(selected = date == value, onClick = { date = value; selectedSlot = null; viewModel.loadRescheduleSlots(appointment, value) }, label = { Text(SimpleDateFormat("MMM d", Locale.US).format(calendar.time)) }) } }
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.AccessTime, "Preferred time", tint = DeepTeal, modifier = Modifier.size(22.dp)); Spacer(Modifier.width(8.dp)); Text("Preferred Time", color = RoyalNavy, fontSize = 17.sp, fontWeight = FontWeight.Bold) }
                when (val slots = state.rescheduleSlots) {
                    Resource.Loading -> CircularProgressIndicator(color = VibrantTeal)
                    is Resource.Error -> Text(slots.message ?: "Unable to load availability.", color = ErrorRed)
                    is Resource.Success -> if (slots.data.isEmpty()) Text("No available times for this date.", color = SlateGray) else LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(slots.data) { slot -> FilterChip(selected = selectedSlot?.startTime == slot.startTime, onClick = { selectedSlot = slot }, label = { Text(slot.label ?: slot.startTime ?: "Time") }) } }
                    Resource.Idle -> Text("Choose a date first.", color = SlateGray)
                }
                OutlinedTextField(value = note, onValueChange = { if (it.length <= 200) note = it }, label = { Text("Add note (optional)") }, supportingText = { Text("${note.length}/200") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), minLines = 3)
            }
            Box(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
                val isLargeFont = androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.2f
                if (isLargeFont) {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), enabled = !state.rescheduleSubmitting, shape = RoundedCornerShape(16.dp), border = BorderStroke(2.dp, Color(0xFF1689E8)), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1689E8))) { Text("Back", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                        Button(onClick = { selectedSlot?.let { viewModel.requestReschedule(appointment, date, it, note) } }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), enabled = date.isNotBlank() && selectedSlot != null && !state.rescheduleSubmitting, shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = VibrantTeal)) { if (state.rescheduleSubmitting) CircularProgressIndicator(Modifier.size(18.dp), color = White) else Text("Send Request", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).heightIn(min = 56.dp), enabled = !state.rescheduleSubmitting, shape = RoundedCornerShape(16.dp), border = BorderStroke(2.dp, Color(0xFF1689E8)), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1689E8))) { Text("Back", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                        Button(onClick = { selectedSlot?.let { viewModel.requestReschedule(appointment, date, it, note) } }, modifier = Modifier.weight(1f).heightIn(min = 56.dp), enabled = date.isNotBlank() && selectedSlot != null && !state.rescheduleSubmitting, shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = VibrantTeal)) { if (state.rescheduleSubmitting) CircularProgressIndicator(Modifier.size(18.dp), color = White) else Text("Send Request", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                    }
                }
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
                SummaryMetaLine(Icons.Default.CalendarMonth, "Appointment date", DateUtils.formatDisplayDate(appointment.appointmentDate))
                SummaryMetaLine(Icons.Default.AccessTime, "Appointment time", DateUtils.formatDisplayTime(appointment.startTime))
                SummaryMetaLine(Icons.Default.LocationOn, "Appointment clinic", appointment.clinicName ?: "Clinexus Dental Clinic")
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
    val style = statusStyle(mapAppointmentStatus(appointment.appointmentStatus))
    AlertDialog(onDismissRequest = onDismiss, confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }, title = { Text("Appointment details", color = RoyalNavy, fontWeight = FontWeight.Bold) }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { StatusBadge(style); DetailLine(Icons.Default.Person, "Dentist", appointment.doctor); DetailLine(Icons.Default.MedicalServices, "Service", appointment.serviceName ?: appointment.treatment); appointment.price?.let { DetailLine(Icons.Default.LocalOffer, "Price", "₱${String.format(Locale.US, "%,.0f", it)}") }; DetailLine(Icons.Default.CalendarMonth, "Date", DateUtils.formatDisplayDate(appointment.appointmentDate)); DetailLine(Icons.Default.AccessTime, "Time", "${DateUtils.formatDisplayTime(appointment.startTime)} – ${DateUtils.formatDisplayTime(appointment.endTime)}"); DetailLine(Icons.Default.LocationOn, "Clinic", appointment.clinicName ?: "Clinexus Dental Clinic"); Text("Booking reference: #${appointment.appointmentId}", color = SlateGray, fontSize = 13.sp); appointment.submittedAt?.let { Text("Submitted: ${DateUtils.formatDisplayDate(it)}", color = SlateGray, fontSize = 13.sp) }; appointment.cancelledBy?.let { Text("Cancelled by: $it", color = ErrorRed, fontSize = 13.sp) }; appointment.cancellationReason?.let { Text("Cancellation reason: $it", color = ErrorRed, fontSize = 13.sp) }; appointment.rescheduleNote?.let { Text("Reschedule note: $it", color = Color(0xFF7C3AED), fontSize = 13.sp) } } })
}

@Composable private fun EmptyAppointments(onBook: () -> Unit) { Column(Modifier.fillMaxWidth().padding(vertical = 44.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) { Icon(Icons.Default.EventAvailable, "No appointments", tint = TealMuted, modifier = Modifier.size(56.dp)); Text("No appointments in this tab", color = RoyalNavy, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text("Book a visit when you are ready.", color = SlateGray); Button(onClick = onBook, colors = ButtonDefaults.buttonColors(containerColor = VibrantTeal)) { Text("Book appointment") } } }
@Composable private fun LoadingState() { Box(Modifier.fillMaxWidth().padding(44.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = VibrantTeal) } }
@Composable private fun ErrorState(message: String, retry: () -> Unit) { Column(Modifier.fillMaxWidth().padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(message, color = ErrorRed); TextButton(onClick = retry) { Text("Retry") } } }