package com.example.clinexusapp.ui.screens.auth

import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.BadgeCheck


import androidx.compose.foundation.layout.*
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
import com.example.clinexusapp.viewmodel.OTPViewModel
import kotlinx.coroutines.delay

@Composable
fun VerifyOTPScreen(
    email: String,
    purpose: String = "verification",
    viewModel: OTPViewModel,
    onOtpVerified: (String?) -> Unit, // passes resetToken or null
    onNavigateBack: () -> Unit
) {
    var otp by remember { mutableStateOf("") }
    val otpState by viewModel.otpState.collectAsState()
    val resetToken by viewModel.resetToken.collectAsState()
    val resendState by viewModel.resendState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var resendSeconds by remember { mutableIntStateOf(60) }

    LaunchedEffect(resendSeconds) {
        if (resendSeconds > 0) { delay(1_000); resendSeconds-- }
    }
    LaunchedEffect(resendState) {
        when (val resend = resendState) {
            is Resource.Success -> {
                otp = ""
                resendSeconds = 60
                snackbarHostState.showSnackbar("A new OTP was sent to your email")
                viewModel.resetResendState()
            }
            is Resource.Error -> {
                snackbarHostState.showSnackbar(resend.message ?: "Unable to resend OTP")
                viewModel.resetResendState()
            }
            else -> Unit
        }
    }

    val state = otpState
    LaunchedEffect(state, resetToken) {
        if (state is Resource.Success) {
            if (purpose == "reset") {
                if (resetToken != null) {
                    onOtpVerified(resetToken)
                    viewModel.resetState()
                } else {
                    // Handled error if token is missing
                    snackbarHostState.showSnackbar("Verification successful, but reset token is missing.")
                }
            } else if (purpose == "verification") {
                onOtpVerified(null)
                viewModel.resetState()
            }
        } else if (state is Resource.Error) {
            snackbarHostState.showSnackbar(state.message ?: "Verification failed")
        }
    }

    Scaffold(
        snackbarHost = { com.example.clinexusapp.ui.components.ClinexusSnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Verify OTP",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Enter the 6‑digit code sent to your email.",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(48.dp))

            OtpCodeInput(value = otp, onValueChange = { otp = it }, enabled = otpState !is Resource.Loading)

            if (purpose == "reset") {
                TextButton(
                    onClick = { viewModel.resendOtp(email, purpose) },
                    enabled = resendSeconds == 0 && resendState !is Resource.Loading,
                ) {
                    Text(if (resendSeconds > 0) "Resend OTP in ${resendSeconds}s" else if (resendState is Resource.Loading) "Sending…" else "Resend OTP")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            VibrantButton(
                text = if (otpState is Resource.Loading) "Verifying..." else "Verify OTP",
                onClick = {
                    if (purpose == "reset") {
                        viewModel.verifyOTP(email, otp)
                    } else {
                        viewModel.verifyEmail(email, otp)
                    }
                },
                enabled = otp.length == 6 && otpState !is Resource.Loading
            )

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = onNavigateBack) {
                Text(
                    text = "Back",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
