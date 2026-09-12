package com.example.clinexusapp.data.service

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.example.clinexusapp.util.NotificationHelper

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM", "From: ${remoteMessage.from}")

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "CliNexus update"
        val message = remoteMessage.notification?.body
            ?: remoteMessage.data["message"]
            ?: remoteMessage.data["body"]
            ?: return
        NotificationHelper.showNotification(this, title, message)
    }

    @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Refreshed token: $token")
        // TODO: Send token to server
    }
}
