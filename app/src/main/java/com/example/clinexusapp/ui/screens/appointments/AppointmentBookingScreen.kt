package com.example.clinexusapp.ui.screens.appointments

import com.composables.icons.lucide.CircleAlert

import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.CalendarDays
import com.composables.icons.lucide.CalendarX
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Circle
import com.composables.icons.lucide.CircleUserRound
import com.composables.icons.lucide.Clock
import com.composables.icons.lucide.MapPin
import com.composables.icons.lucide.Stethoscope
import com.composables.icons.lucide.Tag
import com.composables.icons.lucide.UserRound


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.clinexusapp.ui.components.dentalServiceIcon
import com.example.clinexusapp.ui.theme.*
import com.example.clinexusapp.util.NotificationHelper
import com.example.clinexusapp.util.DateUtils
import com.example.clinexusapp.util.Resource
import com.example.clinexusapp.viewmodel.*
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay

private const val BookingWindowNote = "Choose a date from tomorrow up to 30 days ahead. Same-day booking is unavailable."

private val BookingCardShape = RoundedCornerShape(16.dp)
private enum class TimePeriod(val label: String) { MORNING("Morning"), AFTERNOON("Afternoon") }

@Composable
fun AppointmentBookingScreen(
    onBack: () -> Unit,
    onBookSuccess: (AppointmentTicket) -> Unit,
    viewModel: BookingViewModel = viewModel(),
    doctorName: String = ""
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.step, state.selectedDate, state.selectedDentist?.dentistId) {
        if (state.step == BookingStep.DATE_TIME && state.selectedDate != null) {
            while (true) {
                viewModel.refreshSelectedDate()
                delay(3_000)
            }
        }
    }
    val context = androidx.compose.ui.platform.LocalContext.current
    var showCalendar by remember { mutableStateOf(false) }
    val patient = com.example.clinexusapp.util.SessionManager.currentUser.collectAsState().value

    LaunchedEffect(state.submission) {
        if (state.submission is Resource.Success) {
            NotificationHelper.showBookingNotification(context, state.selectedDentist?.dentistName ?: doctorName)
            val ticket = viewModel.buildTicket()
            if (ticket != null) {
                viewModel.clearSubmission()
                onBookSuccess(ticket)
            }
        }
    }

    if (state.step == BookingStep.DATE_TIME && state.submission is Resource.Error) {
        AlertDialog(
            onDismissRequest = { viewModel.clearSubmission() },
            icon = { Icon(Lucide.CalendarX, null, tint = ErrorRed) },
            title = { Text("Time slot unavailable") },
            text = {
                Text("That appointment time was just booked by someone else. Please choose another available time.")
            },
            confirmButton = {
                Button(onClick = { viewModel.clearSubmission() }) {
                    Text("Choose another time")
                }
            },
        )
    }

    if (showCalendar) {
        val serverWorkingDays = (state.schedule as? Resource.Success)?.data?.workingDays.orEmpty()
        val directoryWorkingDays = state.selectedDentist?.daysOfWeek.orEmpty().split(",").map(String::trim).filter(String::isNotBlank)
        val workingDays = if (serverWorkingDays.isNotEmpty()) serverWorkingDays else directoryWorkingDays
        // If neither source has a schedule, do not incorrectly mark every date red.
        val scheduleReady = state.schedule !is Resource.Loading || directoryWorkingDays.isNotEmpty()
        // DatePicker stores SelectableDates in its state. Keying the entire dialog
        // recreates that state when the schedule request completes.
        key(workingDays, scheduleReady, java.time.LocalDate.now(BookingRules.clinicZone)) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = null,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val selectedDate = java.time.Instant.ofEpochMilli(utcTimeMillis).atZone(java.time.ZoneOffset.UTC).toLocalDate()
                    val dayName = selectedDate.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale.US)
                    return scheduleReady && BookingRules.isWithinBookingWindow(selectedDate.toString()) &&
                        isDentistWorkingDay(workingDays, dayName)
                }
            }
        )
        val selectedDate = datePickerState.selectedDateMillis?.let {
            java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneOffset.UTC).toLocalDate()
        }
        val validSelection = selectedDate?.let {
            scheduleReady && BookingRules.isWithinBookingWindow(it.toString()) &&
                isDentistWorkingDay(workingDays, it.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale.US))
        } == true
        val calendarColors = DatePickerDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.primary,
            headlineContentColor = MaterialTheme.colorScheme.onSurface,
            weekdayContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            navigationContentColor = MaterialTheme.colorScheme.onSurface,
            dayContentColor = MaterialTheme.colorScheme.onSurface,
            disabledDayContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
            selectedDayContainerColor = MaterialTheme.colorScheme.primary,
            selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
            todayContentColor = MaterialTheme.colorScheme.primary,
            todayDateBorderColor = MaterialTheme.colorScheme.outlineVariant,
        )
        DatePickerDialog(
            onDismissRequest = { showCalendar = false },
            shape = RoundedCornerShape(28.dp),
            colors = calendarColors,
            tonalElevation = 0.dp,
            confirmButton = {
                Button(
                    enabled = validSelection,
                    onClick = {
                        if (validSelection) {
                            viewModel.selectDate(selectedDate.toString())
                            showCalendar = false
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                ) { Text("Confirm date", fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showCalendar = false }) { Text("Cancel") }
            },
        ) {
            Column(Modifier.fillMaxWidth()) {
                DatePicker(
                    state = datePickerState,
                    colors = calendarColors,
                    showModeToggle = false,
                    title = {
                        Row(
                            Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Lucide.CalendarDays, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Choose your visit date", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    },
                    headline = {
                        Text(
                            selectedDate?.format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US))
                                ?: "Find your day",
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                )
                BookingDateNote(Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            }
        }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) IconButton(onClick = onBack) { Icon(Lucide.ArrowLeft, "Back") }
        else Spacer(Modifier.size(48.dp))
        Text("Schedule visit", Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
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
                    if (itemIndex < index) Icon(Lucide.Check, null, tint = White)
                    else Text("${itemIndex + 1}", color = if (itemIndex <= index) White else if (isSystemInDarkTheme()) Color(0xFF1F3A6D) else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                }
                if (itemIndex < 3) Box(Modifier.weight(1f).height(3.dp).background(if (itemIndex < index) VibrantTeal else Color(0xFFD9E0E5)))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("Dentist", "Service", "Date & time", "Review").forEach { Text(it, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1) }
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
        Text("${BookingStep.values().indexOf(step) + 1} of 4", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
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
                        Text(dentist.dentistName, color = MaterialTheme.colorScheme.onSurface, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(DentistScheduleFormatter.format(dentist.daysOfWeek), color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, maxLines = 2)
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
                val selected = state.selectedServices.any { it.serviceId == service.serviceId }
                SelectableCard(selected, { viewModel.toggleService(service) }) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) DeepTeal.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Icon(
                            imageVector = dentalServiceIcon(service.serviceName),
                            contentDescription = service.serviceName,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(10.dp).size(23.dp),
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(service.serviceName ?: "Service", color = MaterialTheme.colorScheme.onSurface, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text("${formatPrice(service.price)}${service.durationMinutes?.let { "  •  $it min" } ?: ""}", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                        service.serviceCategoryName?.takeIf { it.isNotBlank() }?.let {
                            Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 1)
                        }
                    }
                    Checkmark(selected)
                }
            }
            if (state.selectedServices.isNotEmpty()) {
                Text(
                    "${state.selectedServices.size} service${if (state.selectedServices.size == 1) "" else "s"} selected • ${state.totalEstimatedDurationMinutes} min total",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun DateTimeStep(state: BookingUiState, viewModel: BookingViewModel, onCalendar: () -> Unit) {
    SummaryCard(
        "Appointment",
        "${state.selectedDentist?.dentistName ?: "Dentist"} • ${state.selectedServices.size} service${if (state.selectedServices.size == 1) "" else "s"} • ${state.totalEstimatedDurationMinutes} min",
        "Edit"
    ) { viewModel.goTo(BookingStep.SERVICE) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(SimpleDateFormat("MMMM yyyy", LocalLocale.current.platformLocale).format(Date()), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        TextButton(onClick = onCalendar) { Icon(Lucide.CalendarDays, null); Spacer(Modifier.width(4.dp)); Text("View calendar") }
    }
    BookingDateNote()
    DateStrip(state, viewModel)
    when (val times = state.timeslots) {
        Resource.Idle -> EmptyState("Choose a date to see available times.")
        Resource.Loading -> LoadingState("Loading available times...")
        is Resource.Error -> ErrorState(times.message ?: "Unable to load times.") { state.selectedDate?.let(viewModel::selectDate) }
        is Resource.Success -> if (times.data.isEmpty()) EmptyState("No times are available for this date. Choose another date.") else TimeGrid(times.data, state.selectedSlot, viewModel::selectSlot)
    }
}

@Composable
private fun BookingDateNote(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Icon(Lucide.CircleAlert, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(BookingWindowNote, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun DateStrip(state: BookingUiState, viewModel: BookingViewModel) {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone(BookingRules.clinicZone) }
    val display = SimpleDateFormat("EEE", Locale.US).apply { timeZone = formatter.timeZone }
    val day = SimpleDateFormat("d", Locale.US).apply { timeZone = formatter.timeZone }
    val today = Calendar.getInstance(java.util.TimeZone.getTimeZone(BookingRules.clinicZone))
    // Show a complete seven-day booking window. Dates beyond this week remain
    // available through the full calendar dialog.
    val dates = (1..7).map { offset -> Calendar.getInstance(java.util.TimeZone.getTimeZone(BookingRules.clinicZone)).apply { add(Calendar.DAY_OF_YEAR, offset) } }
    val dateValues = dates.map { formatter.format(it.time) }
    LaunchedEffect(state.selectedDentist?.dentistId, state.totalEstimatedDurationMinutes) {
        viewModel.loadCalendarAvailability(dateValues)
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
        items(dates) { date ->
            val value = formatter.format(date.time)
            val selected = state.selectedDate == value
            val serverWorkingDays = (state.schedule as? Resource.Success)?.data?.workingDays.orEmpty()
            val directoryWorkingDays = state.selectedDentist?.daysOfWeek.orEmpty().split(",").map(String::trim).filter(String::isNotBlank)
            val workingDays = if (serverWorkingDays.isNotEmpty()) serverWorkingDays else directoryWorkingDays
            val availability = state.calendarAvailability[value]
            val checking = availability == null || availability is Resource.Loading
            val failed = availability is Resource.Error
            val serverUnavailable = (availability as? Resource.Success)?.data?.isEmpty() == true
            val unavailable = !BookingRules.isWithinBookingWindow(value) || serverUnavailable || (workingDays.isNotEmpty() && !isDentistWorkingDay(workingDays, SimpleDateFormat("EEEE", Locale.US).apply { timeZone = formatter.timeZone }.format(date.time)))
            Surface(
                onClick = {
                    when {
                        failed -> viewModel.retryCalendarAvailability(value)
                        !unavailable -> viewModel.selectDate(value)
                    }
                },
                enabled = !unavailable && !checking,
                modifier = Modifier.widthIn(min = 72.dp).heightIn(min = 90.dp).padding(horizontal = 2.dp), shape = BookingCardShape,
                color = if (selected) VibrantTeal else MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, if (selected) VibrantTeal else MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(if (date.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) "Today" else display.format(date.time), color = if (selected) White else if (unavailable) ErrorRed else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    Text(day.format(date.time), color = if (selected) White else if (unavailable) ErrorRed else MaterialTheme.colorScheme.onSurface, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    when {
                        unavailable -> Text("Unavailable", color = ErrorRed, fontSize = 9.sp)
                        checking -> Text("Checking…", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                        failed -> Text("Retry", color = ErrorRed, fontSize = 9.sp)
                    }
                    if (selected) Icon(Lucide.Check, null, tint = White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun TimeGrid(slots: List<AvailableSlotDTO>, selected: AvailableSlotDTO?, onSelect: (AvailableSlotDTO) -> Unit) {
    val slotKey = slots.joinToString { it.startTime.orEmpty() }
    val morningSlots = slots.filter { (it.startTime?.take(2)?.toIntOrNull() ?: 0) < 12 }
    val afternoonSlots = slots.filter { (it.startTime?.take(2)?.toIntOrNull() ?: 0) >= 12 }
    val selectedPeriod = if ((selected?.startTime?.take(2)?.toIntOrNull() ?: 0) >= 12) TimePeriod.AFTERNOON else TimePeriod.MORNING
    var period by remember(slotKey) { mutableStateOf(if (selected != null) selectedPeriod else if (morningSlots.isNotEmpty()) TimePeriod.MORNING else TimePeriod.AFTERNOON) }
    var showAll by remember(period, slotKey) { mutableStateOf(false) }
    val periodSlots = if (period == TimePeriod.MORNING) morningSlots else afternoonSlots
    val visibleSlots = if (showAll) periodSlots else periodSlots.take(6)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Available times", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(18.dp)) {
            Row(Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TimePeriod.values().forEach { option ->
                    val availableCount = if (option == TimePeriod.MORNING) morningSlots.size else afternoonSlots.size
                    val active = period == option
                    Surface(
                        onClick = { period = option },
                        enabled = availableCount > 0,
                        modifier = Modifier.weight(1f).height(42.dp),
                        color = if (active) MaterialTheme.colorScheme.surface else Color.Transparent,
                        shape = RoundedCornerShape(14.dp),
                        shadowElevation = if (active) 2.dp else 0.dp,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("${option.label} ($availableCount)", color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
        if (periodSlots.isEmpty()) {
            EmptyState("No ${period.label.lowercase()} times are available.")
        } else {
            visibleSlots.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { slot ->
                        val isSelected = selected?.startTime?.take(5) == slot.startTime?.take(5)
                        Surface(
                            onClick = { onSelect(slot) },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) VibrantTeal else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, if (isSelected) VibrantTeal else MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(horizontal = 3.dp)) {
                                Text(slot.startTime?.let(DateUtils::formatDisplayTime) ?: "Time", color = if (isSelected) White else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            if (periodSlots.size > 6) {
                TextButton(onClick = { showAll = !showAll }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(if (showAll) "Show fewer" else "Show all ${periodSlots.size} times", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ReviewStep(state: BookingUiState, patient: com.example.clinexusapp.model.PatientInfo?, viewModel: BookingViewModel) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = BookingCardShape, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Appointment details", Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = { viewModel.goTo(BookingStep.DATE_TIME) }) { Text("Edit", color = MaterialTheme.colorScheme.primary) }
            }
            DetailRow(Lucide.UserRound, "Dentist", state.selectedDentist?.dentistName.orEmpty(), "Dentist")
            DetailRow(
                dentalServiceIcon(state.selectedServices.firstOrNull()?.serviceName),
                "Services",
                state.selectedServices.joinToString(", ") { it.serviceName ?: "Service" },
                "Services"
            )
            DetailRow(Lucide.Clock, "Duration", "${state.totalEstimatedDurationMinutes} minutes", "Duration")
            DetailRow(Lucide.Tag, "Price", formatPrice(state.selectedServices.sumOf { it.price ?: 0.0 }), "Price")
            DetailRow(Lucide.CalendarDays, "Date", formatAppointmentDate(state.selectedDate), "Date")
            DetailRow(Lucide.Clock, "Time", formatTimeRange(state.selectedSlot), "Time")
            DetailRow(Lucide.MapPin, "Clinic", "Rivera Dental Clinic", "Clinic")
        }
    }
    Surface(color = MaterialTheme.colorScheme.surface, shape = BookingCardShape, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            PatientAvatar(patient)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Patient", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Text(listOfNotNull(patient?.firstName, patient?.lastName).joinToString(" ").ifBlank { patient?.email ?: "Current patient" }, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Patient", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        }
    }
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = BookingCardShape) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Lucide.CircleAlert, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Text("Your appointment request will be sent to the clinic for approval.", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
        }
    }
    Surface(onClick = { viewModel.setConfirmationChecked(!state.confirmationChecked) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), color = MaterialTheme.colorScheme.surface, shape = BookingCardShape, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
            Checkbox(checked = state.confirmationChecked, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = VibrantTeal))
            Spacer(Modifier.width(12.dp))
            Text("I confirm that the details are correct.", color = MaterialTheme.colorScheme.onSurface)
        }
    }
    if (state.submission is Resource.Error) {
        Text(state.submission.message ?: "Submission failed. Please retry.", color = ErrorRed)
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String, description: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, description, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, modifier = Modifier.width(72.dp))
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SummaryCard(title: String, value: String, action: String?, onAction: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = BookingCardShape, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp); Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            if (action != null) TextButton(onClick = onAction) { Text(action, color = MaterialTheme.colorScheme.primary) }
        }
    }
}

@Composable
private fun SelectableCard(selected: Boolean, onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 76.dp), shape = BookingCardShape, color = if (selected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface, border = BorderStroke(2.dp, if (selected) VibrantTeal else MaterialTheme.colorScheme.outlineVariant)) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), content = content) }
}

@Composable
private fun Avatar(url: String?, description: String) {
    Box(Modifier.size(52.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) { if (url.isNullOrBlank()) Icon(Lucide.UserRound, description, tint = MaterialTheme.colorScheme.primary) else AsyncImage(model = url, contentDescription = description, modifier = Modifier.fillMaxSize()) }
}

@Composable
private fun PatientAvatar(patient: com.example.clinexusapp.model.PatientInfo?) {
    var imageFailed by remember(patient?.profilePicture) { mutableStateOf(false) }
    val initials = listOfNotNull(patient?.firstName?.firstOrNull(), patient?.lastName?.firstOrNull()).joinToString("")
    val profilePicture = patient?.profilePicture
    Box(Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        if (!profilePicture.isNullOrBlank() && !imageFailed) {
            AsyncImage(model = profilePicture, contentDescription = "Patient profile image", modifier = Modifier.fillMaxSize(), onError = { imageFailed = true })
        } else if (initials.isNotBlank()) {
            Text(initials.uppercase(Locale.US), color = MaterialTheme.colorScheme.primary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        } else {
            Icon(Lucide.CircleUserRound, "Patient profile", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
        }
    }
}

@Composable
private fun Checkmark(selected: Boolean) { Box(Modifier.size(28.dp).clip(CircleShape).background(if (selected) VibrantTeal else Color.Transparent), contentAlignment = Alignment.Center) { if (selected) Icon(Lucide.Check, null, tint = White) else Icon(Lucide.Circle, null, tint = LightSlate) } }

@Composable
private fun <T> ResourceContent(resource: Resource<List<T>>, onRetry: () -> Unit, content: @Composable (List<T>) -> Unit) {
    when (resource) { Resource.Idle -> EmptyState("Nothing is available right now."); Resource.Loading -> LoadingState("Loading..."); is Resource.Error -> ErrorState(resource.message ?: "Unable to load.", onRetry); is Resource.Success -> if (resource.data.isEmpty()) EmptyState("Nothing is available right now.") else content(resource.data) }
}

@Composable private fun LoadingState(message: String) { Row(Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)); Spacer(Modifier.width(12.dp)); Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
@Composable private fun EmptyState(message: String) { Surface(color = MaterialTheme.colorScheme.surface, shape = BookingCardShape) { Text(message, Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) } }
@Composable private fun ErrorState(message: String, retry: () -> Unit) { Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) { Text(message, color = ErrorRed); TextButton(onClick = retry) { Text("Retry") } } }

@Composable
private fun BookingBottomAction(label: String, enabled: Boolean, onClick: () -> Unit) { Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) { Box(Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding()) { Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), shape = RoundedCornerShape(28.dp), colors = ButtonDefaults.buttonColors(contentColor = Color.White, containerColor = VibrantTeal, disabledContainerColor = Color(0xFFCBD5D8), disabledContentColor = if (isSystemInDarkTheme()) Color(0xFF1F3A6D) else Color.White)) { Text(label, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1) } } } }

private fun formatPrice(price: Double?): String = price?.let { "₱${String.format(Locale.US, "%,.0f", it)}" } ?: "Price unavailable"
private fun formatAppointmentDate(value: String?): String = value?.let {
    runCatching {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(it)?.let { date ->
            SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US).format(date)
        } ?: it
    }.getOrDefault(it)
} ?: "Date unavailable"

private fun formatTimeRange(slot: AvailableSlotDTO?): String =
    listOfNotNull(slot?.startTime, slot?.endTime)
        .map(DateUtils::formatDisplayTime)
        .joinToString(" – ")
        .ifBlank { "Time unavailable" }

private fun isDentistWorkingDay(days: List<String>, dayName: String): Boolean {
    if (days.isEmpty()) return true
    val week = listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")
    val dayIndex = week.indexOf(dayName.lowercase(Locale.US))
    return days.any { configured ->
        val value = configured.trim().lowercase(Locale.US)
        val range = value.split(Regex("\\s*[-–]\\s*"))
        if (range.size == 2) {
            val start = week.indexOfFirst { it.startsWith(range[0].take(3)) }
            val end = week.indexOfFirst { it.startsWith(range[1].take(3)) }
            start >= 0 && end >= 0 && dayIndex >= start && dayIndex <= end
        } else value == dayName.lowercase(Locale.US) || value.take(3) == dayName.take(3).lowercase(Locale.US)
    }
}
