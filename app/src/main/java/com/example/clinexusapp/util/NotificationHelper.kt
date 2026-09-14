package com.example.clinexusapp.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.media.AudioAttributes
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.clinexusapp.MainActivity

object NotificationHelper {
    private const val CHANNEL_ID = "clinexus_alerts_v2"
    private const val CHANNEL_NAME = "CliNexus Alerts"

    fun createNotificationChannel(context: Context) {
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
            description = "Appointment updates, requests, and dental reminders"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 220, 120, 220)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            setSound(
                Settings.System.DEFAULT_NOTIFICATION_URI,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
            )
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
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
        } catch (_: SecurityException) {
            // Android 13+ users may decline notification permission.
        }
    }
}
