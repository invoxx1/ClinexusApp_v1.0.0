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
import androidx.compose.ui.geometry.Rect
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
import com.example.clinexusapp.ui.components.AppWalkthroughOverlay
import com.example.clinexusapp.ui.components.WalkthroughTarget
import com.example.clinexusapp.ui.components.appWalkthroughSteps
import com.example.clinexusapp.ui.screens.dashboard.DashboardScreen
import com.example.clinexusapp.ui.screens.notifications.NotificationScreen
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
    val dashboardViewModel: DashboardViewModel = hiltViewModel()
    val unreadNotificationsCount by dashboardViewModel.unreadNotificationsCount.collectAsState()
    val sessionInitialized by SessionManager.isInitialized.collectAsState()
    val walkthroughCompleted by SessionManager.walkthroughCompleted.collectAsState()
    val pendingPasswordSave by SessionManager.pendingPasswordSave.collectAsState()
    val walkthroughBounds = remember { mutableStateMapOf<WalkthroughTarget, Rect>() }
    var walkthroughStep by remember { mutableIntStateOf(0) }
    val walkthroughVisible = sessionInitialized && !walkthroughCompleted && pendingPasswordSave == null
    val activeTarget = appWalkthroughSteps.getOrNull(walkthroughStep)?.target
    val activeBounds = activeTarget?.let(walkthroughBounds::get)
    val walkthroughBackStackEntry by navController.currentBackStackEntryAsState()
    val walkthroughDestination = walkthroughBackStackEntry?.destination

    fun captureWalkthroughTarget(target: WalkthroughTarget, bounds: Rect) {
        // Freeze each target after its first stable layout. Recomposition and data refreshes
        // must not move a coach mark that is already visible.
        if (walkthroughBounds[target] == null && bounds.width > 0f && bounds.height > 0f) {
            walkthroughBounds[target] = bounds
        }
    }

    LaunchedEffect(walkthroughVisible, activeTarget) {
        if (!walkthroughVisible || activeTarget == null) return@LaunchedEffect
        val route = when (activeTarget) {
            WalkthroughTarget.APPOINTMENT, WalkthroughTarget.PROMOTIONS, WalkthroughTarget.CLINIC_NEWS -> Screen.Dashboard.route
            WalkthroughTarget.APPOINTMENT_STATUSES, WalkthroughTarget.BOOK_APPOINTMENT -> Screen.AppointmentHistory.route
            WalkthroughTarget.MESSAGES -> Screen.Chat.route
        }
        val alreadyOnTargetScreen = walkthroughDestination?.hierarchy?.any { it.route == route } == true
        if (!alreadyOnTargetScreen) {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id)
                launchSingleTop = true
            }
        }
    }

    fun finishWalkthrough() {
        SessionManager.completeWalkthrough()
        navController.navigate(Screen.Dashboard.route) {
            popUpTo(navController.graph.findStartDestination().id)
            launchSingleTop = true
        }
    }

    fun nextWalkthroughStep() {
        if (walkthroughStep == appWalkthroughSteps.lastIndex) {
            finishWalkthrough()
        } else {
            val nextTarget = appWalkthroughSteps[walkthroughStep + 1].target
            walkthroughBounds.remove(nextTarget)
            walkthroughStep++
        }
    }
    Box(Modifier.fillMaxSize()) {
    Scaffold(
        bottomBar = {
            if (isBottomBarVisible) {
                TealBottomBar(
                    navController = navController,
                    unreadNotificationsCount = unreadNotificationsCount,
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
    ) { innerPadding ->
        val contentBottomPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding())
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(contentBottomPadding).consumeWindowInsets(contentBottomPadding),
        ) {
            composable(route = Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    rootNavController = rootNavController,
                    activeWalkthroughTarget = activeTarget,
                    onWalkthroughTarget = ::captureWalkthroughTarget,
                    onWalkthroughTargetUnavailable = { target ->
                        if (activeTarget == target) nextWalkthroughStep()
                    },
                )
            }
            composable(route = Screen.Chat.route) {
                val chatViewModel: ChatViewModel = hiltViewModel()
                ChatScreen(
                    onBack = { navController.popBackStack() },
                    viewModel = chatViewModel,
                    onWalkthroughTarget = { captureWalkthroughTarget(WalkthroughTarget.MESSAGES, it) },
                ) { isBottomBarVisible = it }
            }
            composable(route = Screen.Notifications.route) {
                val notificationViewModel: NotificationViewModel = hiltViewModel()
                NotificationScreen(
                    onBack = { navController.popBackStack() },
                    viewModel = notificationViewModel,
                    onOpenReference = { appointmentId -> navController.navigate(Screen.AppointmentHistory.createRoute(appointmentId)) { launchSingleTop = true } },
                )
            }
            composable(
                route = Screen.AppointmentHistory.pattern,
                arguments = listOf(androidx.navigation.navArgument("appointmentId") {
                    type = androidx.navigation.NavType.IntType
                    defaultValue = -1
                }),
            ) { entry ->
                val historyViewModel: HistoryViewModel = hiltViewModel()
                AppointmentHistoryScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToBooking = { rootNavController.navigate(Screen.AppointmentBooking.route) },
                    viewModel = historyViewModel,
                    initialAppointmentId = entry.arguments?.getInt("appointmentId")?.takeIf { it > 0 },
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
                        rootNavController.navigate(Screen.Login.route) { launchSingleTop = true }
                    },
                    onLogout = {
                        SessionManager.logout()
                        rootNavController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToPersonalInformation = { rootNavController.navigate(Screen.PersonalInformation.route) },
                    onNavigateToClinicalHistory = { rootNavController.navigate(Screen.ClinicalHistory.route) },
                    onNavigateToHistory = { rootNavController.navigate(Screen.AppointmentHistory.route) },
                    onNavigateToChangePassword = { rootNavController.navigate(Screen.ChangePassword.route) },
                    onNavigateToSessions = { rootNavController.navigate(Screen.Sessions.route) },
                    viewModel = profileViewModel,
                )
            }
        }
    }
        if (walkthroughVisible) {
            if (activeBounds == null) {
                // Keep the underlying screen covered while a new screen or lazy-list
                // target completes its first layout. This prevents a white frame flash.
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color(0xC400102A))
                )
            } else {
                AppWalkthroughOverlay(
                    stepIndex = walkthroughStep,
                    target = activeBounds,
                    onNext = ::nextWalkthroughStep,
                    onSkip = ::finishWalkthrough,
                )
            }
        }
    }
}

@Composable
internal fun RememberPasswordDialog() {
    val pendingPasswordSave by SessionManager.pendingPasswordSave.collectAsState()
    if (pendingPasswordSave != null) {
        AlertDialog(
            onDismissRequest = { SessionManager.dismissPasswordSave() },
            icon = {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Icon(
                        Lucide.LockKeyhole,
                        contentDescription = null,
                        tint = Color(0xFF1F3A6D),
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
}

@Composable
fun TealBottomBar(
    navController: NavHostController,
    unreadNotificationsCount: Int,
) {
    val screens = listOf(
        BottomBarScreen.Dashboard,
        BottomBarScreen.Appointments,
        BottomBarScreen.Notifications,
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
                ambientColor = MaterialTheme.colorScheme.surfaceVariant,
                spotColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup()
                .heightIn(min = 60.dp)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            screens.forEach { screen ->
                val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                TealNavItem(
                    screen = screen,
                    isSelected = isSelected,
                    badgeCount = if (screen == BottomBarScreen.Notifications) unreadNotificationsCount else 0,
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
    badgeCount: Int = 0,
    onClick: () -> Unit
) {
    val contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val highlightColor = MaterialTheme.colorScheme.surfaceVariant
    val label = when (screen) {
        BottomBarScreen.Dashboard -> "Home"
        BottomBarScreen.Appointments -> "Appointments"
        BottomBarScreen.Notifications -> "Notifications"
        BottomBarScreen.Chat -> "Messages"
        BottomBarScreen.Profile -> "Profile"
    }

    BoxWithConstraints(
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) highlightColor else Color.Transparent)
            .selectable(selected = isSelected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 1.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        val labelSize = if (maxWidth < 76.dp) 8.sp else 10.sp
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            BadgedBox(
                badge = {
                    if (badgeCount > 0 && !isSelected) {
                        Badge(containerColor = Color(0xFFE53935)) {
                            Text(if (badgeCount > 99) "99+" else badgeCount.toString())
                        }
                    }
                },
            ) {
                Icon(
                    imageVector = screen.icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp),
                )
            }
            Text(
                text = label,
                color = contentColor,
                fontSize = labelSize,
                lineHeight = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                overflow = androidx.compose.ui.text.style.TextOverflow.Clip,
            )
        }
    }
}
