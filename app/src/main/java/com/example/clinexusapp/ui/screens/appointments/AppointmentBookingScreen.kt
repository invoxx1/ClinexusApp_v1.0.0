package com.example.clinexusapp.ui.screens.appointments

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.clinexusapp.model.*
import com.example.clinexusapp.ui.theme.*
import com.example.clinexusapp.util.NotificationHelper
import com.example.clinexusapp.util.Resource
import com.example.clinexusapp.viewmodel.*
import java.text.SimpleDateFormat
import java.util.*

private val BookingCardShape = RoundedCornerShape(16.dp)

@Composable
fun AppointmentBookingScreen(
    onBack: () -> Unit,
    onBookSuccess: (AppointmentTicket) -> Unit,
    viewModel: BookingViewModel = viewModel(),
    doctorName: String = ""
) {
    val state by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showCalendar by remember { mutableStateOf(false) }
    val patient = com.example.clinexusapp.util.SessionManager.currentUser.collectAsState().value

    LaunchedEffect(state.submission) {
        if (state.submission is Resource.Success) {
            NotificationHelper.showBookingNotification(context, state.selectedDentist?.dentistName ?: doctorName)
            viewModel.buildTicket()?.let(onBookSuccess)
            viewModel.clearSubmission()
        }
    }

    if (showCalendar) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis >= System.currentTimeMillis() - 86_400_000L
            }
        )
        DatePickerDialog(
            onDismissRequest = { showCalendar = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        viewModel.selectDate(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(millis)))
                    }
                    showCalendar = false
                }) { Text("Select") }
            },
            dismissButton = { TextButton(onClick = { showCalendar = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    Scaffold(
        containerColor = SoftMist,
        topBar = { BookingTopBar(onBack = if (state.step == BookingStep.DENTIST) onBack else { { viewModel.goTo(previousStep(state.step)) } }) },
        bottomBar = {
            BookingBottomAction(
                label = when (state.step) {
                    BookingStep.DENTIST, BookingStep.SERVICE -> "Continue"
                    BookingStep.DATE_TIME -> "Review appointment"
                    BookingStep.REVIEW -> if (state.isSubmitting) "Submitting..." else "Confirm appointment"
                },
                enabled = BookingRules.canContinue(state),
                onClick = {
                    when (state.step) {
                        BookingStep.DENTIST -> viewModel.goTo(BookingStep.SERVICE)
                        BookingStep.SERVICE -> viewModel.goTo(BookingStep.DATE_TIME)
                        BookingStep.DATE_TIME -> viewModel.goTo(BookingStep.REVIEW)
                        BookingStep.REVIEW -> viewModel.submit()
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { BookingProgress(state.step) }
            item { StepHeading(state.step) }
            when (state.step) {
                BookingStep.DENTIST -> item { DentistStep(state, viewModel) }
                BookingStep.SERVICE -> item { ServiceStep(state, viewModel) }
                BookingStep.DATE_TIME -> item { DateTimeStep(state, viewModel, onCalendar = { showCalendar = true }) }
                BookingStep.REVIEW -> item { ReviewStep(state, patient, viewModel) }
            }
        }
    }
}

private fun previousStep(step: BookingStep) = when (step) {
    BookingStep.DENTIST -> BookingStep.DENTIST
    BookingStep.SERVICE -> BookingStep.DENTIST
    BookingStep.DATE_TIME -> BookingStep.SERVICE
    BookingStep.REVIEW -> BookingStep.DATE_TIME
}

@Composable
private fun BookingTopBar(onBack: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
        else Spacer(Modifier.size(48.dp))
        Text("Schedule visit", Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = RoyalNavy)
        Spacer(Modifier.size(48.dp))
    }
}

@Composable
private fun BookingProgress(step: BookingStep) {
    val index = BookingStep.values().indexOf(step)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            BookingStep.values().forEachIndexed { itemIndex, item ->
                Box(Modifier.size(36.dp).clip(CircleShape).background(if (itemIndex <= index) VibrantTeal else Color(0xFFD9E0E5)), contentAlignment = Alignment.Center) {
                    if (itemIndex < index) Icon(Icons.Default.Check, null, tint = White)
                    else Text("${itemIndex + 1}", color = if (itemIndex <= index) White else SlateGray, fontWeight = FontWeight.Bold)
                }
                if (itemIndex < 3) Box(Modifier.weight(1f).height(3.dp).background(if (itemIndex < index) VibrantTeal else Color(0xFFD9E0E5)))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("Dentist", "Service", "Date & time", "Review").forEach { Text(it, fontSize = 11.sp, color = RoyalNavy, maxLines = 1) }
        }
    }
}

@Composable
private fun StepHeading(step: BookingStep) {
    val title = when (step) {
        BookingStep.DENTIST -> "Select a dentist"; BookingStep.SERVICE -> "Select a service"
        BookingStep.DATE_TIME -> "Choose date & time"; BookingStep.REVIEW -> "Review appointment"
    }
    val subtitle = when (step) {
        BookingStep.DENTIST -> "Choose who you would like to see."; BookingStep.SERVICE -> "Choose the treatment you need."
        BookingStep.DATE_TIME -> "Find a time that works for you."; BookingStep.REVIEW -> "Please check the details before confirming."
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("${BookingStep.values().indexOf(step) + 1} of 4", color = SlateGray, fontSize = 14.sp)
        Text(title, color = RoyalNavy, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = SlateGray, fontSize = 16.sp)
    }
}

@Composable
private fun DentistStep(state: BookingUiState, viewModel: BookingViewModel) {
    ResourceContent(state.dentists, onRetry = viewModel::retryDentists) { dentists ->
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            dentists.forEach { dentist ->
                SelectableCard(state.selectedDentist?.dentistId == dentist.dentistId, { viewModel.selectDentist(dentist) }) {
                    Avatar(dentist.profileImage, dentist.dentistName)
                    Column(Modifier.weight(1f)) {
                        Text(dentist.dentistName, color = RoyalNavy, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(DentistScheduleFormatter.format(dentist.daysOfWeek), color = DeepTeal, fontSize = 14.sp, maxLines = 2)
                    }
                    Checkmark(state.selectedDentist?.dentistId == dentist.dentistId)
                }
            }
        }
    }
}

@Composable
private fun ServiceStep(state: BookingUiState, viewModel: BookingViewModel) {
    state.selectedDentist?.let { dentist -> SummaryCard("Dentist", dentist.dentistName, "Edit") { viewModel.goTo(BookingStep.DENTIST) } }
    ResourceContent(state.services, onRetry = viewModel::retryServices) { services ->
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            services.forEach { service ->
                val selected = state.selectedService?.serviceId == service.serviceId
                SelectableCard(selected, { viewModel.selectService(service) }) {
                    Icon(Icons.Default.MedicalServices, null, tint = DeepTeal, modifier = Modifier.size(32.dp))
                    Column(Modifier.weight(1f)) {
                        Text(service.serviceName ?: "Service", color = RoyalNavy, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text("${formatPrice(service.price)}${service.durationMinutes?.let { "  •  $it min" } ?: ""}", color = DeepTeal, fontSize = 14.sp)
                    }
                    Checkmark(selected)
                }
            }
        }
    }
}

@Composable
private fun DateTimeStep(state: BookingUiState, viewModel: BookingViewModel, onCalendar: () -> Unit) {
    SummaryCard("Appointment", "${state.selectedDentist?.dentistName ?: "Dentist"} • ${state.selectedService?.serviceName ?: "Service"}", "Edit") { viewModel.goTo(BookingStep.SERVICE) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(SimpleDateFormat("MMMM yyyy", LocalLocale.current.platformLocale).format(Date()), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = RoyalNavy)
        TextButton(onClick = onCalendar) { Icon(Icons.Default.CalendarMonth, null); Spacer(Modifier.width(4.dp)); Text("View calendar") }
    }
    DateStrip(state, viewModel)
    Text("Available times", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = RoyalNavy)
    when (val times = state.timeslots) {
        Resource.Idle -> EmptyState("Choose a date to see available times.")
        Resource.Loading -> LoadingState("Loading available times...")
        is Resource.Error -> ErrorState(times.message ?: "Unable to load times.") { state.selectedDate?.let(viewModel::selectDate) }
        is Resource.Success -> if (times.data.isEmpty()) EmptyState("No times are available for this date. Choose another date.") else TimeGrid(times.data, state.selectedSlot, viewModel::selectSlot)
    }
}

@Composable
private fun DateStrip(state: BookingUiState, viewModel: BookingViewModel) {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val display = SimpleDateFormat("EEE", Locale.US)
    val day = SimpleDateFormat("d", Locale.US)
    val today = Calendar.getInstance()
    val dates = (0..4).map { offset -> Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offset) } }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
        items(dates) { date ->
            val value = formatter.format(date.time)
            val selected = state.selectedDate == value
            val unavailable = !isDentistWorkingDay(
                state.selectedDentist?.daysOfWeek,
                SimpleDateFormat("EEEE", Locale.US).format(date.time)
            )
            Surface(
                onClick = { if (!unavailable) viewModel.selectDate(value) }, enabled = !unavailable,
                modifier = Modifier.widthIn(min = 72.dp).heightIn(min = 90.dp).padding(horizontal = 2.dp), shape = BookingCardShape,
                color = if (selected) VibrantTeal else White, border = BorderStroke(1.dp, if (selected) VibrantTeal else Color(0xFFE4EAEE))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(if (date.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) "Today" else display.format(date.time), color = if (selected) White else if (unavailable) LightSlate else SlateGray, fontSize = 12.sp)
                    Text(day.format(date.time), color = if (selected) White else if (unavailable) LightSlate else RoyalNavy, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    if (unavailable) Text("Unavailable", color = LightSlate, fontSize = 9.sp)
                    if (selected) Icon(Icons.Default.Check, null, tint = White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun TimeGrid(slots: List<AvailableSlotDTO>, selected: AvailableSlotDTO?, onSelect: (AvailableSlotDTO) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        slots.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { slot ->
                    val isSelected = selected?.startTime == slot.startTime
                    Surface(onClick = { onSelect(slot) }, modifier = Modifier.weight(1f).heightIn(min = 56.dp).padding(vertical = 2.dp), shape = BookingCardShape, color = if (isSelected) VibrantTeal else White) { Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 8.dp)) { Text(slot.label ?: slot.startTime ?: "Time", color = if (isSelected) White else RoyalNavy, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center) } }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ReviewStep(state: BookingUiState, patient: com.example.clinexusapp.model.PatientInfo?, viewModel: BookingViewModel) {
    Surface(color = White, shape = BookingCardShape, border = BorderStroke(1.dp, Color(0xFFE4EAEE))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Appointment details", Modifier.weight(1f), color = RoyalNavy, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = { viewModel.goTo(BookingStep.DATE_TIME) }) { Text("Edit", color = DeepTeal) }
            }
            DetailRow(Icons.Default.Person, "Dentist", state.selectedDentist?.dentistName.orEmpty(), "Dentist")
            DetailRow(Icons.Default.MedicalServices, "Service", state.selectedService?.serviceName.orEmpty(), "Service")
            DetailRow(Icons.Default.LocalOffer, "Price", formatPrice(state.selectedService?.price), "Price")
            DetailRow(Icons.Default.CalendarMonth, "Date", formatAppointmentDate(state.selectedDate), "Date")
            DetailRow(Icons.Default.AccessTime, "Time", formatTimeRange(state.selectedSlot), "Time")
            DetailRow(Icons.Default.LocationOn, "Clinic", "Clinexus Dental Clinic", "Clinic")
        }
    }
    Surface(color = White, shape = BookingCardShape, border = BorderStroke(1.dp, Color(0xFFE4EAEE))) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            PatientAvatar(patient)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Patient", color = SlateGray, fontSize = 13.sp)
                Text(listOfNotNull(patient?.firstName, patient?.lastName).joinToString(" ").ifBlank { patient?.email ?: "Current patient" }, color = RoyalNavy, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Patient", color = SlateGray, fontSize = 13.sp)
            }
        }
    }
    Surface(color = Color(0xFFE4F3FF), shape = BookingCardShape) { Text("Your appointment request will be sent to the clinic for approval.", Modifier.padding(16.dp), color = RoyalNavy) }
    Surface(onClick = { viewModel.setConfirmationChecked(!state.confirmationChecked) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), color = White, shape = BookingCardShape, border = BorderStroke(1.dp, Color(0xFFE4EAEE))) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
            Checkbox(checked = state.confirmationChecked, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = VibrantTeal))
            Text("I confirm that the details are correct.", color = RoyalNavy)
        }
    }
    if (state.submission is Resource.Error) {
        Text(state.submission.message ?: "Submission failed. Please retry.", color = ErrorRed)
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String, description: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, description, tint = SlateGray, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, color = SlateGray, fontSize = 14.sp, modifier = Modifier.width(72.dp))
        Text(value, color = RoyalNavy, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SummaryCard(title: String, value: String, action: String?, onAction: () -> Unit) {
    Surface(color = White, shape = BookingCardShape, border = BorderStroke(1.dp, Color(0xFFE4EAEE))) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(title, color = SlateGray, fontSize = 13.sp); Text(value, color = RoyalNavy, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            if (action != null) TextButton(onClick = onAction) { Text(action, color = DeepTeal) }
        }
    }
}

@Composable
private fun SelectableCard(selected: Boolean, onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 76.dp), shape = BookingCardShape, color = if (selected) MintSparkle else White, border = BorderStroke(2.dp, if (selected) VibrantTeal else Color(0xFFE4EAEE))) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), content = content) }
}

@Composable
private fun Avatar(url: String?, description: String) {
    Box(Modifier.size(52.dp).clip(CircleShape).background(MintSparkle), contentAlignment = Alignment.Center) { if (url.isNullOrBlank()) Icon(Icons.Default.Person, description, tint = DeepTeal) else AsyncImage(model = url, contentDescription = description, modifier = Modifier.fillMaxSize()) }
}

@Composable
private fun PatientAvatar(patient: com.example.clinexusapp.model.PatientInfo?) {
    var imageFailed by remember(patient?.profilePicture) { mutableStateOf(false) }
    val initials = listOfNotNull(patient?.firstName?.firstOrNull(), patient?.lastName?.firstOrNull()).joinToString("")
    val profilePicture = patient?.profilePicture
    Box(Modifier.size(56.dp).clip(CircleShape).background(MintSparkle), contentAlignment = Alignment.Center) {
        if (!profilePicture.isNullOrBlank() && !imageFailed) {
            AsyncImage(model = profilePicture, contentDescription = "Patient profile image", modifier = Modifier.fillMaxSize(), onError = { imageFailed = true })
        } else if (initials.isNotBlank()) {
            Text(initials.uppercase(Locale.US), color = DeepTeal, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        } else {
            Icon(Icons.Default.AccountCircle, "Patient profile", tint = DeepTeal, modifier = Modifier.size(34.dp))
        }
    }
}

@Composable
private fun Checkmark(selected: Boolean) { Box(Modifier.size(28.dp).clip(CircleShape).background(if (selected) VibrantTeal else Color.Transparent), contentAlignment = Alignment.Center) { if (selected) Icon(Icons.Default.Check, null, tint = White) else Icon(Icons.Default.RadioButtonUnchecked, null, tint = LightSlate) } }

@Composable
private fun <T> ResourceContent(resource: Resource<List<T>>, onRetry: () -> Unit, content: @Composable (List<T>) -> Unit) {
    when (resource) { Resource.Idle -> EmptyState("Nothing is available right now."); Resource.Loading -> LoadingState("Loading..."); is Resource.Error -> ErrorState(resource.message ?: "Unable to load.", onRetry); is Resource.Success -> if (resource.data.isEmpty()) EmptyState("Nothing is available right now.") else content(resource.data) }
}

@Composable private fun LoadingState(message: String) { Row(Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(color = VibrantTeal, modifier = Modifier.size(24.dp)); Spacer(Modifier.width(12.dp)); Text(message, color = SlateGray) } }
@Composable private fun EmptyState(message: String) { Surface(color = White, shape = BookingCardShape) { Text(message, Modifier.padding(20.dp), color = SlateGray) } }
@Composable private fun ErrorState(message: String, retry: () -> Unit) { Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) { Text(message, color = ErrorRed); TextButton(onClick = retry) { Text("Retry") } } }

@Composable
private fun BookingBottomAction(label: String, enabled: Boolean, onClick: () -> Unit) { Surface(color = White, shadowElevation = 8.dp) { Box(Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding()) { Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), shape = RoundedCornerShape(28.dp), colors = ButtonDefaults.buttonColors(containerColor = VibrantTeal, disabledContainerColor = Color(0xFFCBD5D8))) { Text(label, fontSize = 17.sp, fontWeight = FontWeight.Bold) } } } }

private fun formatPrice(price: Double?): String = price?.let { "₱${String.format(Locale.US, "%,.0f", it)}" } ?: "Price unavailable"
private fun formatAppointmentDate(value: String?): String = value?.let {
    runCatching {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(it)?.let { date ->
            SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US).format(date)
        } ?: it
    }.getOrDefault(it)
} ?: "Date unavailable"

private fun formatTimeRange(slot: AvailableSlotDTO?): String = listOfNotNull(slot?.startTime, slot?.endTime).joinToString(" – ").ifBlank { slot?.label ?: "Time unavailable" }

private fun isDentistWorkingDay(days: String?, dayName: String): Boolean {
    if (days.isNullOrBlank()) return true
    val normalizedDay = dayName.lowercase(Locale.US)
    return days.split(",", "-", "–")
        .map { it.trim().lowercase(Locale.US) }
        .any { configuredDay ->
            configuredDay == normalizedDay ||
                configuredDay.take(3) == normalizedDay.take(3)
        }
}