package com.example.clinexusapp.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.clinexusapp.ui.navigation.BottomBarScreen
import com.example.clinexusapp.ui.navigation.Screen
import com.example.clinexusapp.ui.screens.dashboard.DashboardScreen
import com.example.clinexusapp.ui.screens.chat.ChatScreen
import com.example.clinexusapp.ui.screens.profile.ProfileScreen
import com.example.clinexusapp.ui.screens.appointments.AppointmentHistoryScreen
import com.example.clinexusapp.util.SessionManager
import com.example.clinexusapp.viewmodel.*
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel


@Composable
fun MainScreen(rootNavController: NavHostController, @Suppress("UNUSED_PARAMETER") settingsViewModel: SettingsViewModel) {
    val navController = rememberNavController()
    var isBottomBarVisible by remember { mutableStateOf(value = true) }

    Scaffold(
        bottomBar = { 
            if (isBottomBarVisible) {
                TealBottomBar(navController = navController)
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
        ) {
            composable(route = Screen.Dashboard.route) {
                val dashboardViewModel: DashboardViewModel = hiltViewModel()
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    rootNavController = rootNavController,
                    onNotificationClick = {
                        rootNavController.navigate(Screen.Notifications.route)
                    },
                )
            }
            composable(route = Screen.Chat.route) {
                val chatViewModel: ChatViewModel = hiltViewModel()
                ChatScreen(
                    onBack = { navController.popBackStack() }, 
                    viewModel = chatViewModel,
                ) {
                    isBottomBarVisible = it
                }
            }
            composable(route = Screen.AppointmentHistory.route) {
                val historyViewModel: HistoryViewModel = hiltViewModel()
                AppointmentHistoryScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToBooking = {
                        rootNavController.navigate(Screen.AppointmentBooking.route)
                    },
                    viewModel = historyViewModel,
                )
            }
            composable(route = Screen.Profile.route) {
                val profileViewModel: ProfileViewModel = hiltViewModel()
                ProfileScreen(
                    onLogout = {
                        SessionManager.logout()
                        rootNavController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onNavigateToPersonalInformation = {
                        rootNavController.navigate(Screen.PersonalInformation.route)
                    },
                    onNavigateToHistory = {
                        rootNavController.navigate(Screen.AppointmentHistory.route)
                    },
                    // ✅ NEW: Navigate to ChangePassword screen using the root controller
                    onNavigateToChangePassword = {
                        rootNavController.navigate(Screen.ChangePassword.route)
                    },
                    viewModel = profileViewModel,
                )
            }
        }
    }
}

@Composable
fun TealBottomBar(navController: NavHostController) {
    val screens = listOf(
        BottomBarScreen.Dashboard,
        BottomBarScreen.Appointments,
        BottomBarScreen.Chat,
        BottomBarScreen.Profile,
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .shadow(12.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            screens.forEach { screen ->
                val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                TealNavItem(
                    screen = screen,
                    isSelected = isSelected
                ) {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id)
                        launchSingleTop = true
                    }
                }
            }
        }
    }
}

@Composable
fun TealNavItem(
    screen: BottomBarScreen,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val contentColor = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    val highlightColor = primaryColor.copy(alpha = 0.12f)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) highlightColor else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = screen.icon,
                contentDescription = screen.title,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(4.dp)
                        .background(primaryColor, CircleShape)
                )
            }
        }
    }
}