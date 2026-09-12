package com.example.clinexusapp.ui.screens.main

import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.LockKeyhole


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val pendingPasswordSave by SessionManager.pendingPasswordSave.collectAsState()

    if (pendingPasswordSave != null) {
        AlertDialog(
            onDismissRequest = { SessionManager.dismissPasswordSave() },
            icon = {
                Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFE0F7F4)) {
                    Icon(
                        Lucide.LockKeyhole,
                        contentDescription = null,
                        tint = Color(0xFF00A896),
                        modifier = Modifier.padding(12.dp).size(28.dp),
                    )
                }
            },
            title = { Text("Remember password?") },
            text = {
                Text("Next time you sign in on this phone, just tap your profile picture instead of typing your password.")
            },
            confirmButton = {
                Button(onClick = { SessionManager.confirmPasswordSave() }) {
                    Text("Remember")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { SessionManager.dismissPasswordSave() }) {
                    Text("Not now")
                }
            },
        )
    }

    Scaffold(
        bottomBar = { 
            if (isBottomBarVisible) {
                TealBottomBar(navController = navController)
            }
        },
        containerColor = Color(0xFFF0FAFA),
    ) { innerPadding ->
        val contentBottomPadding = PaddingValues(
            bottom = innerPadding.calculateBottomPadding()
        )
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier
                .padding(contentBottomPadding)
                .consumeWindowInsets(contentBottomPadding),
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
                    onSwitchAccount = {
                        rootNavController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    },
                    onAddAccount = {
                        rootNavController.navigate(Screen.Login.route) {
                            launchSingleTop = true
                        }
                    },
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
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = Color(0xFFB5DAD6),
                spotColor = Color(0xFFB5DAD6),
            ),
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup()
                .heightIn(min = 60.dp)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            screens.forEach { screen ->
                val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                TealNavItem(
                    screen = screen,
                    isSelected = isSelected,
                    modifier = Modifier.weight(1f),
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
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val contentColor = if (isSelected) Color(0xFF00A69C) else Color(0xFF969CAC)
    val highlightColor = Color(0xFFD5F4F0)
    val label = when (screen) {
        BottomBarScreen.Dashboard -> "Home"
        BottomBarScreen.Appointments -> "Appointments"
        BottomBarScreen.Chat -> "Messages"
        BottomBarScreen.Profile -> "Profile"
    }

    BoxWithConstraints(
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) highlightColor else Color.Transparent)
            .selectable(selected = isSelected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        val labelSize = if (maxWidth < 76.dp) 10.sp else 11.sp
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Icon(
                imageVector = screen.icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = label,
                color = contentColor,
                fontSize = labelSize,
                lineHeight = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}
