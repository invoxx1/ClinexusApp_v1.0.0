package com.example.clinexusapp.ui.navigation

import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.CalendarDays
import com.composables.icons.lucide.Bell
import com.composables.icons.lucide.House
import com.composables.icons.lucide.MessageCircle
import com.composables.icons.lucide.UserRound


import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomBarScreen(
    val route: String,
    val title: String,
    val icon: ImageVector,
) {
    object Dashboard : BottomBarScreen(
        route = Screen.Dashboard.route,
        title = "Home",
        icon = Lucide.House,
    )
    object Appointments : BottomBarScreen(
        route = Screen.AppointmentHistory.route,
        title = "Visits",
        icon = Lucide.CalendarDays,
    )
    object Notifications : BottomBarScreen(
        route = Screen.Notifications.route,
        title = "Notifications",
        icon = Lucide.Bell,
    )
    object Chat : BottomBarScreen(
        route = Screen.Chat.route,
        title = "Chat",
        icon = Lucide.MessageCircle,
    )
    object Profile : BottomBarScreen(
        route = Screen.Profile.route,
        title = "Profile",
        icon = Lucide.UserRound,
    )
}
