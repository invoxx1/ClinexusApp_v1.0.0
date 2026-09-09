package com.example.clinexusapp.ui.screens.appointments

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.clinexusapp.ui.theme.*
import com.example.clinexusapp.viewmodel.AppointmentTicket
import com.example.clinexusapp.viewmodel.BookingRules

@Composable
fun AppointmentTicketScreen(
    ticket: AppointmentTicket,
    onViewAppointments: () -> Unit,
    onBackHome: () -> Unit,
) {
    val status = BookingRules.statusLabel(ticket.status)
    val pending = status == "Awaiting clinic approval"
    val heading = if (pending) "Booking complete" else "Appointment confirmed"
    val message = if (pending) {
        "Your appointment request was submitted successfully. The clinic will notify you after approval."
    } else {
        "Your appointment has been scheduled successfully."
    }

    Scaffold(containerColor = SoftMist) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(Icons.Default.CheckCircle, "Booking success", tint = VibrantTeal, modifier = Modifier.size(64.dp))
            Text(heading, color = RoyalNavy, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Surface(color = if (pending) Color(0xFFFFF3D6) else MintSparkle, shape = RoundedCornerShape(20.dp)) {
                Text(status, color = RoyalNavy, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
            Text(message, color = SlateGray, fontSize = 15.sp)
            TicketCard(ticket, status)
            Button(onClick = onViewAppointments, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = VibrantTeal), shape = RoundedCornerShape(26.dp)) { Text("View my appointments") }
            TextButton(onClick = onBackHome) { Text("Back to home", color = DeepTeal) }
        }
    }
}

@Composable
private fun TicketCard(ticket: AppointmentTicket, status: String) {
    Surface(color = White, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, Color(0xFFE4EAEE)), shadowElevation = 3.dp) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("Appointment ticket", color = RoyalNavy, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            TicketRow("Status", status)
            TicketRow("Dentist", ticket.dentist)
            TicketRow("Service", ticket.service)
            TicketRow("Price", ticket.price)
            TicketRow("Date", ticket.date)
            TicketRow("Time", ticket.time)
            TicketRow("Clinic", ticket.clinic)
            TicketRow("Patient", ticket.patient)
            HorizontalDivider(color = Color(0xFFDCE5E6), modifier = Modifier.padding(vertical = 4.dp))
            TicketRow("Booking reference", ticket.reference)
        }
    }
}

@Composable
private fun TicketRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Text(label, color = SlateGray, fontSize = 13.sp, modifier = Modifier.weight(0.42f))
        Text(value.ifBlank { "Not provided" }, color = RoyalNavy, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(0.58f))
    }
}