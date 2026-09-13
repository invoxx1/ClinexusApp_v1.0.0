package com.example.clinexusapp.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.clinexusapp.MainActivity

object NotificationHelper {
    private const val CHANNEL_ID = "clinexus_notifications"
    private const val CHANNEL_NAME = "CliNexus Reminders"

    fun createNotificationChannel(context: Context) {
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
            description = "Dental appointment and health reminders"
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    fun showBookingNotification(context: Context, doctorName: String) {
        showNotification(context, "Appointment requested", "Your visit with $doctorName was sent to the clinic.")
    }

    fun showNotification(context: Context, title: String, message: String, appointmentId: Int? = null) {
        createNotificationChannel(context)
        val openApp = PendingIntent.getActivity(
            context,
            appointmentId ?: 0,
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                if (appointmentId != null) {
                    action = AppNavigationRequests.ACTION_OPEN_APPOINTMENT
                    putExtra(AppNavigationRequests.EXTRA_APPOINTMENT_ID, appointmentId)
                }
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(openApp)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
        } catch (_: SecurityException) {
            // Android 13+ users may decline notification permission.
        }
    }
}
