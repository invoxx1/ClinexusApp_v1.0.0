package com.example.clinexusapp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomBarScreen(
    val route: String,
    val title: String,
    val icon: ImageVector,
) {
    object Dashboard : BottomBarScreen(
        route = Screen.Dashboard.route,
        title = "Home",
        icon = Icons.Default.Home,
    )
    object Appointments : BottomBarScreen(
        route = Screen.AppointmentHistory.route,
        title = "Visits",
        icon = Icons.AutoMirrored.Filled.EventNote,
    )
    object Chat : BottomBarScreen(
        route = Screen.Chat.route,
        title = "Chat",
        icon = Icons.AutoMirrored.Filled.Chat,
    )
    object Profile : BottomBarScreen(
        route = Screen.Profile.route,
        title = "Profile",
        icon = Icons.Default.Person,
    )
}
