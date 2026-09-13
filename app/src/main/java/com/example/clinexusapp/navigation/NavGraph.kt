package com.example.clinexusapp.navigation

import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.ShieldAlert


import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.clinexusapp.ui.navigation.Screen
import com.example.clinexusapp.ui.screens.appointments.AppointmentBookingScreen
import com.example.clinexusapp.ui.screens.appointments.AppointmentHistoryScreen
import com.example.clinexusapp.ui.screens.appointments.AppointmentTicketScreen
import com.example.clinexusapp.ui.screens.auth.*
import com.example.clinexusapp.ui.screens.chat.ChatScreen
import com.example.clinexusapp.ui.screens.main.MainScreen
import com.example.clinexusapp.ui.screens.main.RememberPasswordDialog
import com.example.clinexusapp.ui.screens.notifications.NotificationScreen
import com.example.clinexusapp.ui.screens.auth.ChangePasswordScreen
import com.example.clinexusapp.ui.screens.profile.PersonalInformationScreen
import com.example.clinexusapp.ui.screens.profile.SessionManagementScreen
import com.example.clinexusapp.ui.screens.settings.SettingsScreen
import com.example.clinexusapp.util.SessionManager
import com.example.clinexusapp.util.AppointmentReminderScheduler
import com.example.clinexusapp.util.AppNavigationRequests
import com.example.clinexusapp.ui.theme.DeepTeal
import com.example.clinexusapp.ui.theme.MintSparkle
import com.example.clinexusapp.ui.theme.PureWhite
import com.example.clinexusapp.ui.theme.RoyalNavy
import com.example.clinexusapp.ui.theme.SlateGray
import com.example.clinexusapp.viewmodel.*

@Composable
fun SetupNavGraph(navController: NavHostController, settingsViewModel: SettingsViewModel, startDestination: String = Screen.Splash.route) {
    val context = LocalContext.current
    var latestAppointmentTicket by remember { mutableStateOf<AppointmentTicket?>(null) }
    val sessionExpired by SessionManager.sessionExpired.collectAsState()
    val sessionReady by SessionManager.isInitialized.collectAsState()
    val currentUser by SessionManager.currentUser.collectAsState()
    val requestedAppointmentId by AppNavigationRequests.appointmentId.collectAsState()

    LaunchedEffect(requestedAppointmentId, sessionReady, currentUser?.patientID) {
        val appointmentId = requestedAppointmentId ?: return@LaunchedEffect
        if (sessionReady && currentUser != null) {
            navController.navigate(Screen.AppointmentHistory.createRoute(appointmentId)) { launchSingleTop = true }
            AppNavigationRequests.consumeAppointment()
        }
    }

    LaunchedEffect(sessionExpired) {
        if (sessionExpired) {
            navController.navigate(Screen.Login.route) {
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    if (sessionExpired) {
        SessionExpiredDialog(onContinue = SessionManager::acknowledgeSessionExpiry)
    }

    RememberPasswordDialog()

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { fadeIn(tween(400)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(400)) },
        exitTransition = { fadeOut(tween(400)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(400)) },
        popEnterTransition = { fadeIn(tween(400)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(400)) },
        popExitTransition = {
            fadeOut(tween(400)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(400))
        },
    ) {
        // Splash, Onboarding, Login, Register
        composable(
            route = Screen.Splash.route,
            enterTransition = { EnterTransition.None }
        ) {
            SplashScreen(
                onNavigateToOnboarding = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
            ) {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }
        }
        composable(route = Screen.Onboarding.route) {
            OnboardingScreen {
                navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                }
            }
        }
        composable(route = Screen.Login.route) {
            val loginViewModel: LoginViewModel = hiltViewModel()
            LoginScreen(
                viewModel = loginViewModel,
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
            ) {
                navController.navigate(Screen.ForgotPassword.route)
            }
        }
        composable(route = Screen.Register.route) {
            val registerViewModel: RegisterViewModel = hiltViewModel()
            RegisterScreen(
                viewModel = registerViewModel,
                onRegisterSuccess = { email ->
                    navController.navigate(Screen.OTP.createRoute(email, "verification"))
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        // ---- OTP screen for email verification (registration) and password reset ----
        composable(
            route = Screen.OTP.route,
            arguments = listOf(
                navArgument("email") { type = NavType.StringType },
                navArgument("purpose") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            val purpose = backStackEntry.arguments?.getString("purpose") ?: "verification"
            val otpViewModel: OTPViewModel = hiltViewModel()
            VerifyOTPScreen(
                email = email,
                purpose = purpose,
                viewModel = otpViewModel,
                onOtpVerified = { resetToken ->
                    if (purpose == "reset") {
                        navController.navigate(Screen.ResetPassword.createRoute(resetToken ?: ""))
                    } else {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(navController.graph.id) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // ---- Forgot Password (Step 1) ----
        composable(route = Screen.ForgotPassword.route) {
            val otpViewModel: OTPViewModel = hiltViewModel()
            ForgotPasswordScreen(
                viewModel = otpViewModel,
                onNavigateToOtp = { email ->
                    navController.navigate(Screen.OTP.createRoute(email, "reset"))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // ---- Reset Password (Step 3) ----
        composable(
            route = Screen.ResetPassword.route,
            arguments = listOf(navArgument("resetToken") { type = NavType.StringType })
        ) { backStackEntry ->
            val resetToken = backStackEntry.arguments?.getString("resetToken") ?: ""
            val otpViewModel: OTPViewModel = hiltViewModel()
            ResetPasswordScreen(
                resetToken = resetToken,
                viewModel = otpViewModel,
                onResetSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.ForgotPassword.route) { inclusive = true }
                    }
                }
            )
        }

        // ---- CHANGE PASSWORD (for logged‑in users) ----
        composable(route = Screen.ChangePassword.route) {
            val otpViewModel: OTPViewModel = hiltViewModel()
            ChangePasswordScreen(
                viewModel = otpViewModel,
                onBack = { navController.popBackStack() },
                onChangeSuccess = {
                    navController.popBackStack()
                    // Optionally show a success message via snackbar or toast
                }
            )
        }

        // ---- Main app screens ----
        composable(route = Screen.Home.route) {
            MainScreen(
                rootNavController = navController,
                settingsViewModel = settingsViewModel
            )
        }

        composable(
            route = Screen.AppointmentBooking.route,
            arguments = listOf(
                navArgument("doctorName") {
                    type = NavType.StringType
                    defaultValue = "Dr. Olivia Bennett"
                },
            ),
        ) { backStackEntry ->
            val doctorName = backStackEntry.arguments?.getString("doctorName") ?: "Dr. Olivia Bennett"
            val bookingViewModel: BookingViewModel = hiltViewModel()
            AppointmentBookingScreen(
                doctorName = doctorName,
                onBack = { navController.popBackStack() },
                onBookSuccess = { ticket ->
                    AppointmentReminderScheduler.schedule(context, ticket)
                    latestAppointmentTicket = ticket
                    navController.navigate(Screen.AppointmentTicket.route)
                },
                viewModel = bookingViewModel
            )
        }

        composable(route = Screen.AppointmentTicket.route) {
            val ticket = latestAppointmentTicket
            if (ticket != null) {
                AppointmentTicketScreen(
                    ticket = ticket,
                    onViewAppointments = {
                        navController.navigate(Screen.AppointmentHistory.route)
                        latestAppointmentTicket = null
                    },
                    onBackHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.AppointmentBooking.route) { inclusive = true }
                        }
                        latestAppointmentTicket = null
                    }
                )
            }
        }

        composable(
            route = Screen.AppointmentHistory.pattern,
            arguments = listOf(navArgument("appointmentId") { type = NavType.IntType; defaultValue = -1 }),
        ) { entry ->
            val historyViewModel: HistoryViewModel = hiltViewModel()
            AppointmentHistoryScreen(
                onBack = { navController.popBackStack() },
                onNavigateToBooking = {
                    navController.navigate(Screen.AppointmentBooking.route)
                },
                viewModel = historyViewModel,
                initialAppointmentId = entry.arguments?.getInt("appointmentId")?.takeIf { it > 0 },
            )
        }

        composable(route = Screen.Chat.route) {
            val chatViewModel: ChatViewModel = hiltViewModel()
            ChatScreen(
                onBack = { navController.popBackStack() },
                viewModel = chatViewModel
            )
        }

        composable(route = Screen.Notifications.route) {
            val notificationViewModel: NotificationViewModel = hiltViewModel()
            NotificationScreen(
                onBack = { navController.popBackStack() },
                viewModel = notificationViewModel
            )
        }

        composable(route = Screen.PersonalInformation.route) {
            val profileViewModel: ProfileViewModel = hiltViewModel()
            PersonalInformationScreen(
                onBack = { navController.popBackStack() },
                viewModel = profileViewModel
            )
        }

        composable(route = Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onLogout = {
                    SessionManager.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                settingsViewModel = settingsViewModel
            )
        }

        composable(route = Screen.Sessions.route) {
            SessionManagementScreen(
                onBack = { navController.popBackStack() },
                onSignedOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
            )
        }
    }
}

@Composable
private fun SessionExpiredDialog(onContinue: () -> Unit) {
    Dialog(onDismissRequest = {}) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = PureWhite,
            shadowElevation = 12.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(shape = CircleShape, color = MintSparkle) {
                    Icon(
                        imageVector = Lucide.ShieldAlert,
                        contentDescription = null,
                        tint = DeepTeal,
                        modifier = Modifier.padding(11.dp).size(27.dp),
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Session expired",
                    color = RoyalNavy,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    text = "Your session ended. Please sign in again.",
                    color = SlateGray,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepTeal),
                ) {
                    Text("Sign in again", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}
