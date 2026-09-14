package com.example.clinexusapp.ui.screens.auth

import com.example.clinexusapp.util.passwordsMeetRules

import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.BadgeCheck
import com.composables.icons.lucide.KeyRound
import com.composables.icons.lucide.LockKeyhole


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.clinexusapp.ui.components.*
import com.example.clinexusapp.util.Resource
import com.example.clinexusapp.util.SessionManager
import com.example.clinexusapp.viewmodel.OTPViewModel
import kotlinx.coroutines.launch

@Composable
fun ChangePasswordScreen(
    viewModel: OTPViewModel,
    onBack: () -> Unit,
    onChangeSuccess: () -> Unit,
) {
    var step by remember { mutableIntStateOf(0) } // 0=Passwords, 1=Verify OTP, 2=Confirm change
    var currentPassword by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var confirmChange by remember { mutableStateOf(false) }
    var currentPasswordError by remember { mutableStateOf<String?>(null) }
    var newPasswordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    val otpState by viewModel.otpState.collectAsState()
    val resetToken by viewModel.resetToken.collectAsState()
    val resendState by viewModel.resendState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val user by SessionManager.currentUser.collectAsState()
    val registeredEmail = user?.email.orEmpty()
    val maskedEmail = remember(registeredEmail) { maskEmailAddress(registeredEmail) }
    var resendSeconds by remember { mutableIntStateOf(60) }

    LaunchedEffect(resendSeconds, step) {
        if (step == 1 && resendSeconds > 0) { kotlinx.coroutines.delay(1_000); resendSeconds-- }
    }
    LaunchedEffect(resendState) {
        when (val resend = resendState) {
            is Resource.Success -> {
                otp = ""
                resendSeconds = 60
                snackbarHostState.showSnackbar("A new OTP was sent to $maskedEmail")
                viewModel.resetResendState()
            }
            is Resource.Error -> {
                snackbarHostState.showSnackbar(resend.message ?: "Unable to resend OTP")
                viewModel.resetResendState()
            }
            else -> Unit
        }
    }

    if (confirmChange) {
        AlertDialog(
            onDismissRequest = { confirmChange = false },
            title = { Text("Change password?", fontWeight = FontWeight.Bold) },
            text = { Text("Your current password will stop working after this change.") },
            dismissButton = { TextButton(onClick = { confirmChange = false }) { Text("Cancel") } },
            confirmButton = {
                Button(onClick = {
                    confirmChange = false
                    viewModel.changePassword(resetToken ?: "", newPassword)
                }) { Text("Change password") }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    val state = otpState
    LaunchedEffect(state) {
        when (val currentState = state) {
            is Resource.Success<*> -> {
                when (step) {
                    0 -> {
                        step = 1
                        viewModel.resetOtpState()  // ✅ now exists
                    }
                    1 -> {
                        step = 2
                        viewModel.resetOtpState()  // ✅ now exists
                    }
                    2 -> {
                        onChangeSuccess()
                        viewModel.resetState()
                    }
                }
            }
            is Resource.Error -> {
                if (step == 0) {
                    currentPasswordError = currentState.message
                        ?.takeUnless { it.equals("Invalid", ignoreCase = true) }
                        ?: "Current password is incorrect"
                } else {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(currentState.message ?: "Operation failed")
                    }
                }
            }
            else -> {} // ignore Loading or other states
        }
    }

    Scaffold(
        topBar = {
            ElegantTopAppBar(title = "Change Password", onBack = onBack)
        },
        snackbarHost = { com.example.clinexusapp.ui.components.ClinexusSnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (step) {
                0 -> {
                    Text(
                        text = "Update Password",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Enter your current password and choose a new one.",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(36.dp))
                    NeumorphicCard {
                        MintTextField(
                            value = currentPassword,
                            onValueChange = { currentPassword = it; currentPasswordError = null },
                            label = "Current Password",
                            icon = Lucide.LockKeyhole,
                            isPassword = true,
                            placeholder = "Enter your current password",
                            errorText = currentPasswordError,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        MintTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it; newPasswordError = null; confirmPasswordError = null },
                            label = "New Password",
                            icon = Lucide.KeyRound,
                            isPassword = true,
                            placeholder = "Create a strong password",
                            errorText = newPasswordError,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        MintTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it; confirmPasswordError = null },
                            label = "Re-enter New Password",
                            icon = Lucide.KeyRound,
                            isPassword = true,
                            placeholder = "Enter the new password again",
                            errorText = confirmPasswordError,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        PasswordRequirements(newPassword, confirmPassword)
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                    VibrantButton(
                        text = if (otpState is Resource.Loading) "Checking..." else "Continue",
                        onClick = {
                            currentPasswordError = if (currentPassword.isBlank()) "Enter your current password" else null
                            newPasswordError = passwordInputError(newPassword)
                            confirmPasswordError = when {
                                confirmPassword.isBlank() -> "Re-enter your new password"
                                confirmPassword != newPassword -> "Passwords do not match"
                                else -> null
                            }
                            if (currentPasswordError == null && newPasswordError == null && confirmPasswordError == null) {
                                viewModel.confirmCurrentPassword(registeredEmail, currentPassword)
                            }
                        },
                        enabled = registeredEmail.isNotBlank() && otpState !is Resource.Loading
                    )
                }
                1 -> {
                    Text(
                        text = "Verify OTP",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "The OTP was sent to $maskedEmail",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(48.dp))
                    OtpCodeInput(
                        value = otp,
                        onValueChange = { otp = it },
                        enabled = otpState !is Resource.Loading,
                    )
                    TextButton(
                        onClick = { viewModel.resendOtp(registeredEmail, "change") },
                        enabled = resendSeconds == 0 && resendState !is Resource.Loading,
                    ) {
                        Text(if (resendSeconds > 0) "Resend OTP in ${resendSeconds}s" else if (resendState is Resource.Loading) "Sending…" else "Resend OTP")
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    VibrantButton(
                        text = if (otpState is Resource.Loading) "Verifying..." else "Verify",
                        onClick = { viewModel.verifyPasswordChangeOTP(otp) },
                        enabled = (otp.length == 6) && (otpState !is Resource.Loading)
                    )
                }
                2 -> {
                    Text(
                        text = "Verification Complete",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your email has been verified. Confirm to update your password.",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    VibrantButton(
                        text = if (otpState is Resource.Loading) "Changing..." else "Confirm Change",
                        onClick = { confirmChange = true },
                        enabled = resetToken != null && otpState !is Resource.Loading
                    )
                }
            }
        }
    }
}

private fun maskEmailAddress(email: String): String {
    val parts = email.trim().split("@", limit = 2)
    if (parts.size != 2) return "your registered email"
    val local = parts[0]
    val visibleCount = when {
        local.length >= 4 -> 4
        local.length >= 2 -> 1
        else -> 0
    }
    val visible = local.take(visibleCount)
    return "$visible***@${parts[1]}"
}

private fun passwordInputError(password: String): String? = when {
    password.isBlank() -> "Enter a new password"
    password.length < 8 -> "Password must contain at least 8 characters"
    password.none { it.isUpperCase() } -> "Password must contain at least 1 uppercase letter"
    password.any { it.isWhitespace() } -> "Password must not contain spaces"
    else -> null
}
