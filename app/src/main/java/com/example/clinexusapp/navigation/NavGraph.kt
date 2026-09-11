package com.example.clinexusapp.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.clinexusapp.ui.screens.notifications.NotificationScreen
import com.example.clinexusapp.ui.screens.auth.ChangePasswordScreen
import com.example.clinexusapp.ui.screens.profile.PersonalInformationScreen
import com.example.clinexusapp.ui.screens.settings.SettingsScreen
import com.example.clinexusapp.util.SessionManager
import com.example.clinexusapp.viewmodel.*

@Composable
fun SetupNavGraph(navController: NavHostController, settingsViewModel: SettingsViewModel) {
    var latestAppointmentTicket by remember { mutableStateOf<AppointmentTicket?>(null) }
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        enterTransition = { fadeIn(tween(400)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(400)) },
        exitTransition = { fadeOut(tween(400)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(400)) },
        popEnterTransition = { fadeIn(tween(400)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(400)) },
        popExitTransition = {
            fadeOut(tween(400)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(400))
        },
    ) {
        // Splash, Onboarding, Login, Register
        composable(route = Screen.Splash.route) {
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
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
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

        composable(route = Screen.AppointmentHistory.route) {
            val historyViewModel: HistoryViewModel = hiltViewModel()
            AppointmentHistoryScreen(
                onBack = { navController.popBackStack() },
                onNavigateToBooking = {
                    navController.navigate(Screen.AppointmentBooking.route)
                },
                viewModel = historyViewModel
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
    }
}
