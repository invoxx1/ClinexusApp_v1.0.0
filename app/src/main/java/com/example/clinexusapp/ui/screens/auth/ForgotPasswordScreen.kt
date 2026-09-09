
package com.example.clinexusapp.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
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

@Composable
fun ForgotPasswordScreen(
    viewModel: OTPViewModel,
    onNavigateToOtp: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    val isEmailValid = remember(email) {
        android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    }

    val otpState by viewModel.otpState.collectAsState()

    val snackbarHostState = remember {
        SnackbarHostState()
    }

    LaunchedEffect(otpState) {

        when (val state = otpState) {

            is Resource.Success -> {
                // Remove blocking showSnackbar call
                onNavigateToOtp(email.trim())
                viewModel.resetState()
            }

            is Resource.Error -> {

                snackbarHostState.showSnackbar(
                    state.message ?: "Failed to send OTP"
                )
            }

            else -> Unit
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },
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
                text = "Forgot Password",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "Enter your email address and we'll send you a verification code.",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(
                    alpha = 0.6f
                ),
                textAlign = TextAlign.Center
            )

            Spacer(
                modifier = Modifier.height(48.dp)
            )

            NeumorphicCard {

                MintTextField(
                    value = email,

                    onValueChange = {
                        email = it
                    },

                    label = "Email Address",

                    icon = Icons.Default.Email
                )
            }

            Spacer(
                modifier = Modifier.height(32.dp)
            )

            VibrantButton(
                text = if (otpState is Resource.Loading) {
                    "Sending..."
                } else {
                    "Send Code"
                },

                onClick = {
                    viewModel.forgotPassword(
                        email.trim()
                    )
                },

                enabled = isEmailValid &&
                        otpState !is Resource.Loading
            )

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            TextButton(
                onClick = onNavigateBack
            ) {
                Text(
                    text = "Back to Login",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
