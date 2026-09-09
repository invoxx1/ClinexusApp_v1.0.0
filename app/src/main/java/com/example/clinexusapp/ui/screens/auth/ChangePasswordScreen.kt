package com.example.clinexusapp.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import kotlinx.coroutines.launch

@Composable
fun ChangePasswordScreen(
    viewModel: OTPViewModel,
    onBack: () -> Unit,
    onChangeSuccess: () -> Unit,
) {
    var step by remember { mutableIntStateOf(0) } // 0=Request OTP, 1=Verify, 2=New Password
    var otp by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val otpState by viewModel.otpState.collectAsState()
    val resetToken by viewModel.resetToken.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

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
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(currentState.message ?: "Operation failed")
                }
            }
            else -> {} // ignore Loading or other states
        }
    }

    Scaffold(
        topBar = {
            ElegantTopAppBar(title = "Change Password", onBack = onBack)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                        text = "Request OTP",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "We'll send a verification code to your registered email.",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(48.dp))
                    VibrantButton(
                        text = if (otpState is Resource.Loading) "Sending..." else "Send Code",
                        onClick = { viewModel.requestPasswordChange() },
                        enabled = otpState !is Resource.Loading
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
                        text = "Enter the 6‑digit code sent to your email.",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(48.dp))
                    NeumorphicCard {
                        MintTextField(
                            value = otp,
                            onValueChange = { otp = it },
                            label = "OTP Code",
                            icon = Icons.Default.Verified
                        )
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
                        text = "New Password",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Set a strong password.",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(48.dp))
                    NeumorphicCard {
                        MintTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = "New Password",
                            icon = Icons.Default.Lock,
                            isPassword = true
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        MintTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = "Confirm Password",
                            icon = Icons.Default.LockReset,
                            isPassword = true
                        )
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    VibrantButton(
                        text = if (otpState is Resource.Loading) "Changing..." else "Change Password",
                        onClick = {
                            if (newPassword == confirmPassword) {
                                viewModel.changePassword(resetToken ?: "", newPassword)
                            } else {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Passwords do not match")
                                }
                            }
                        },
                        enabled = (newPassword.isNotEmpty()) &&
                                (newPassword == confirmPassword) &&
                                (otpState !is Resource.Loading)
                    )
                }
            }
        }
    }
}