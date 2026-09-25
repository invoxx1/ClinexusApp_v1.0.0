package com.example.clinexusapp.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.clinexusapp.viewmodel.AppointmentTicket
import com.example.clinexusapp.model.AppointmentDTO
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object AppointmentReminderScheduler {
    fun sync(context: Context, appointments: List<AppointmentDTO>) {
        appointments.forEach { appointment ->
            when (appointment.appointmentStatus.lowercase()) {
                "cancelled", "canceled", "completed", "done", "rejected", "declined", "denied" ->
                    cancel(context, appointment.appointmentId)
                else -> schedule(context, appointment)
            }
        }
    }

    fun schedule(context: Context, ticket: AppointmentTicket) {
        if (!context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                .getBoolean("appointment_reminders", true)
        ) return
        val appointment = runCatching {
            val date = LocalDate.parse(ticket.date.take(10))
            val rawTime = ticket.time.substringBefore('–').trim().substringBefore('-').trim()
            val time = runCatching { LocalTime.parse(rawTime.take(5), DateTimeFormatter.ofPattern("HH:mm")) }
                .getOrElse { LocalTime.parse(rawTime, DateTimeFormatter.ofPattern("h:mm a")) }
            LocalDateTime.of(date, time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }.getOrNull() ?: return

        scheduleOne(context, ticket, appointment - 24 * 60 * 60 * 1000L, 24, "Appointment tomorrow")
        scheduleOne(context, ticket, appointment - 60 * 60 * 1000L, 1, "Appointment in one hour")
    }

    fun schedule(context: Context, appointment: AppointmentDTO) {
        val ticket = AppointmentTicket(
            reference = appointment.appointmentId.toString(), status = appointment.appointmentStatus,
            dentist = appointment.doctor, service = appointment.serviceName ?: appointment.treatment,
            price = "", date = DateUtils.appointmentDateOnly(appointment.appointmentDate),
            time = "${appointment.startTime} – ${appointment.endTime}", clinic = appointment.clinicName.orEmpty(), patient = "",
        )
        cancel(context, appointment.appointmentId)
        schedule(context, ticket)
    }

    fun cancel(context: Context, appointmentId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        listOf(24, 1).forEach { offset ->
            val pending = PendingIntent.getBroadcast(
                context,
                requestCode(appointmentId.toString(), offset),
                Intent(context, AppointmentReminderReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            )
            if (pending != null) {
                alarmManager.cancel(pending)
                pending.cancel()
            }
        }
    }

    private fun scheduleOne(context: Context, ticket: AppointmentTicket, triggerAt: Long, offset: Int, title: String) {
        if (triggerAt <= System.currentTimeMillis()) return
        val requestCode = requestCode(ticket.reference, offset)
        val intent = Intent(context, AppointmentReminderReceiver::class.java)
            .putExtra("title", title)
            .putExtra("message", "Your visit with ${ticket.dentist} is at ${ticket.time}.")
            .putExtra("appointment_id", ticket.reference.toIntOrNull() ?: -1)
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
    }

    private fun requestCode(reference: String, offset: Int) = 31 * reference.hashCode() + offset
}

class AppointmentReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                .getBoolean("appointment_reminders", true)
        ) return
        NotificationHelper.showNotification(
            context,
            intent.getStringExtra("title") ?: "Appointment reminder",
            intent.getStringExtra("message") ?: "You have an upcoming dental appointment.",
            intent.getIntExtra("appointment_id", -1).takeIf { it > 0 },
        )
    }
}
