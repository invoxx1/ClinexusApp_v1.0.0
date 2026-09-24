package com.example.clinexusapp.ui.navigation

sealed class Screen(val route: String) {
    object ChangePassword : Screen("change_password")
    // Auth
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object Register : Screen("register")
    object OTP : Screen("otp/{email}/{purpose}") {
        fun createRoute(email: String, purpose: String) = "otp/$email/$purpose"
    }
    object ForgotPassword : Screen("forgot_password")
    object ResetPassword : Screen("reset_password/{resetToken}") {
        fun createRoute(resetToken: String) = "reset_password/$resetToken"
    }

    // Main
    object Home : Screen("home")
    object Dashboard : Screen("dashboard")
    object AppointmentBooking : Screen("appointment_booking?doctorName={doctorName}") {
        fun createRoute(doctorName: String) = "appointment_booking?doctorName=$doctorName"
    }
    object AppointmentHistory : Screen("appointment_history") {
        const val pattern = "appointment_history?appointmentId={appointmentId}"
        fun createRoute(appointmentId: Int) = "appointment_history?appointmentId=$appointmentId"
    }
    object AppointmentTicket : Screen("appointment_ticket")
    object Chat : Screen("chat")
    object Notifications : Screen("notifications")
    object Settings : Screen("settings")
    object Profile : Screen("profile")
    object PersonalInformation : Screen("personal_information")
    object ClinicalHistory : Screen("clinical_history")
    object Sessions : Screen("sessions")
}
