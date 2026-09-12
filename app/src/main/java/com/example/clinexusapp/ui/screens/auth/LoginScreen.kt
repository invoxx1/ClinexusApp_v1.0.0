package com.example.clinexusapp.ui.screens.auth

import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.CircleAlert
import com.composables.icons.lucide.LockKeyhole
import com.composables.icons.lucide.Mail
import com.composables.icons.lucide.Settings
import com.composables.icons.lucide.UserRound
import com.composables.icons.lucide.X


import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.example.clinexusapp.ui.components.MintTextField
import com.example.clinexusapp.ui.components.VibrantButton
import com.example.clinexusapp.ui.theme.*
import com.example.clinexusapp.util.Resource
import com.example.clinexusapp.util.SessionManager
import com.example.clinexusapp.viewmodel.LoginViewModel

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedPatientID by remember { mutableStateOf(SessionManager.consumeRequestedAccountLogin()) }
    var autoLoginPatientID by remember { mutableStateOf<Int?>(null) }
    var showManualLogin by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }
    var credentialError by remember { mutableStateOf<String?>(null) }
    var showSavedAccountSettings by remember { mutableStateOf(false) }

    val loginState by viewModel.loginState.collectAsState()
    val savedAccounts by SessionManager.savedAccounts.collectAsState()
    val selectedAccount = savedAccounts.firstOrNull { it.patient.patientID == selectedPatientID }
    val autoLoginAccount = savedAccounts.firstOrNull { it.patient.patientID == autoLoginPatientID }
    val showSavedAccounts = savedAccounts.isNotEmpty() && selectedAccount == null && !showManualLogin
    val showAutoLoginAnimation = autoLoginAccount != null && loginState is Resource.Loading

    val returnToSavedAccounts = {
        selectedPatientID = null
        showManualLogin = false
        email = ""
        password = ""
        credentialError = null
    }

    BackHandler(enabled = savedAccounts.isNotEmpty() && !showSavedAccounts) { returnToSavedAccounts() }

    LaunchedEffect(selectedAccount?.patient?.patientID) {
        selectedAccount?.let { account ->
            email = account.patient.email.orEmpty()
        }
    }

    loginError?.let { message ->
        Dialog(
            onDismissRequest = { loginError = null },
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                color = PureWhite,
                shadowElevation = 12.dp,
            ) {
                Column(Modifier.padding(22.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = Color(0xFFFFECEF)) {
                            Icon(
                                Lucide.CircleAlert,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(10.dp).size(24.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Couldn't sign in",
                            modifier = Modifier.weight(1f),
                            color = RoyalNavy,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        IconButton(onClick = { loginError = null }) {
                            Icon(Lucide.X, "Close", tint = SlateGray)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(message, color = SlateGray, fontSize = 15.sp, lineHeight = 21.sp)
                    Spacer(Modifier.height(22.dp))
                    Button(
                        onClick = { loginError = null },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepTeal),
                    ) {
                        Text("Try again", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showSavedAccountSettings) {
        Dialog(onDismissRequest = { showSavedAccountSettings = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                color = PureWhite,
                shadowElevation = 12.dp,
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Remove saved accounts",
                            modifier = Modifier.weight(1f),
                            color = RoyalNavy,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        IconButton(onClick = { showSavedAccountSettings = false }) {
                            Icon(Lucide.X, "Close", tint = SlateGray)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    savedAccounts.forEach { account ->
                        val patient = account.patient
                        val photo = account.cachedProfilePicture ?: patient.profilePicture
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AccountAvatar(photo, patient.firstName.orEmpty(), 48.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    listOf(patient.firstName, patient.lastName).filterNotNull().joinToString(" ").ifBlank { "Patient" },
                                    color = RoyalNavy,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(patient.email.orEmpty(), color = SlateGray, fontSize = 12.sp, maxLines = 1)
                            }
                            FilledTonalButton(
                                onClick = {
                                    val removingLastAccount = savedAccounts.size == 1
                                    SessionManager.removeSavedAccount(patient.patientID)
                                    if (removingLastAccount) {
                                        showSavedAccountSettings = false
                                        showManualLogin = true
                                    }
                                },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFFFFECEF),
                                    contentColor = MaterialTheme.colorScheme.error,
                                ),
                            ) {
                                Text("Remove", fontWeight = FontWeight.Bold)
                            }
                        }
                        if (account != savedAccounts.last()) HorizontalDivider(color = Color(0xFFE8ECEF))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "This removes login information only from this phone. The patient account will not be deleted.",
                        color = SlateGray,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                    )
                }
            }
        }
    }

    LaunchedEffect(loginState) {
        when (val state = loginState) {
            is Resource.Success -> {
                onLoginSuccess()
                viewModel.resetState()
            }
            is Resource.Error -> {
                if (isCredentialError(state.message)) {
                    credentialError = if (selectedAccount != null || autoLoginAccount != null) {
                        "The password you entered is incorrect."
                    } else {
                        "The email address or password you entered is incorrect."
                    }
                } else loginError = friendlyLoginMessage(state.message)
                autoLoginPatientID?.let { selectedPatientID = it }
                autoLoginPatientID = null
                viewModel.resetState()
            }
            else -> Unit
        }
    }

    val submitLogin = {
        val submittedEmail = selectedAccount?.patient?.email.orEmpty().ifBlank { email }
        when {
            submittedEmail.isBlank() -> loginError = "Enter the email address for your patient account."
            password.isBlank() -> credentialError = "Enter your password to continue."
            else -> {
                val matchingAccount = selectedAccount ?: savedAccounts.firstOrNull {
                    it.patient.email.equals(submittedEmail.trim(), ignoreCase = true)
                }
                viewModel.login(
                    email = submittedEmail,
                    password = password,
                    rememberAccount = true,
                    offerToSavePassword = matchingAccount?.password.isNullOrBlank(),
                )
            }
        }
    }

    Scaffold(containerColor = SoftMist) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).imePadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (showAutoLoginAnimation) {
                Spacer(Modifier.height(150.dp))
            } else {
                if (!showSavedAccounts && savedAccounts.isNotEmpty()) {
                    IconButton(onClick = returnToSavedAccounts, modifier = Modifier.align(Alignment.Start)) {
                        Icon(Lucide.ArrowLeft, "Back to saved accounts", tint = RoyalNavy)
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        if (showSavedAccounts) {
                            IconButton(onClick = { showSavedAccountSettings = true }) {
                                Icon(Lucide.Settings, "Manage saved accounts", tint = RoyalNavy)
                            }
                        } else {
                            Spacer(Modifier.height(48.dp))
                        }
                    }
                }

                LoginBrand()
                Spacer(Modifier.height(if (showSavedAccounts) 64.dp else 44.dp))
            }

            when {
                showAutoLoginAnimation -> {
                    val account = autoLoginAccount
                    val patient = account.patient
                    val photo = account.cachedProfilePicture ?: patient.profilePicture
                    Box(modifier = Modifier.size(116.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            modifier = Modifier.fillMaxSize(),
                            color = DeepTeal,
                            trackColor = MintSparkle,
                            strokeWidth = 4.dp,
                        )
                        AccountAvatar(photo, patient.firstName.orEmpty(), 96.dp)
                    }
                    Spacer(Modifier.height(22.dp))
                    Text(
                        listOf(patient.firstName, patient.lastName).filterNotNull().joinToString(" ").ifBlank { "Patient" },
                        color = RoyalNavy,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Signing in…", color = SlateGray, fontSize = 14.sp)
                }

                showSavedAccounts -> {
                    savedAccounts.forEach { account ->
                        val patient = account.patient
                        val photo = account.cachedProfilePicture ?: patient.profilePicture
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable(enabled = loginState !is Resource.Loading) {
                                credentialError = null
                                email = patient.email.orEmpty()
                                val savedPassword = account.password
                                if (savedPassword.isNullOrBlank()) {
                                    selectedPatientID = patient.patientID
                                    password = ""
                                } else {
                                    autoLoginPatientID = patient.patientID
                                    password = savedPassword
                                    viewModel.login(patient.email.orEmpty(), savedPassword, rememberAccount = true)
                                }
                            },
                            shape = RoundedCornerShape(20.dp), color = PureWhite,
                            border = BorderStroke(1.dp, Color(0xFFDDE7E6)), shadowElevation = 1.dp,
                        ) {
                            Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                                AccountAvatar(photo, patient.firstName.orEmpty(), 58.dp)
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    val name = listOf(patient.firstName, patient.lastName).filterNotNull().joinToString(" ").ifBlank { "Patient" }
                                    Text(name, color = RoyalNavy, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                    Text(patient.email.orEmpty(), color = SlateGray, fontSize = 13.sp, maxLines = 1)
                                }
                                Icon(Lucide.ChevronRight, null, tint = LightSlate)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    FilledTonalButton(
                        onClick = { showManualLogin = true }, modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFE8EFEE)),
                    ) { Text("Use another account", color = RoyalNavy, fontWeight = FontWeight.Bold) }
                }

                selectedAccount != null -> {
                    val patient = selectedAccount.patient
                    AccountAvatar(selectedAccount.cachedProfilePicture ?: patient.profilePicture, patient.firstName.orEmpty(), 86.dp)
                    Spacer(Modifier.height(14.dp))
                    Text(
                        listOf(patient.firstName, patient.lastName).filterNotNull().joinToString(" ").ifBlank { "Patient" },
                        color = RoyalNavy, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                    )
                    Text(patient.email.orEmpty(), color = SlateGray, fontSize = 14.sp)
                    Spacer(Modifier.height(30.dp))
                    MintTextField(
                        value = password, onValueChange = { password = it; credentialError = null },
                        label = "Password", icon = Lucide.LockKeyhole, isPassword = true, errorText = credentialError,
                    )
                }

                else -> {
                    MintTextField(
                        value = email, onValueChange = { email = it; credentialError = null },
                        label = "Email address", icon = Lucide.Mail,
                    )
                    Spacer(Modifier.height(18.dp))
                    MintTextField(
                        value = password, onValueChange = { password = it; credentialError = null },
                        label = "Password", icon = Lucide.LockKeyhole, isPassword = true, errorText = credentialError,
                    )
                }
            }

            if (!showSavedAccounts && !showAutoLoginAnimation) {
                Spacer(Modifier.height(24.dp))
                VibrantButton(
                    text = if (loginState is Resource.Loading) "Signing in…" else "Sign in",
                    onClick = submitLogin, enabled = loginState !is Resource.Loading,
                )
                TextButton(onClick = onNavigateToForgotPassword) {
                    Text("Forgot password?", color = DeepTeal, fontWeight = FontWeight.Bold)
                }
            }

            if (!showAutoLoginAnimation) {
                Spacer(Modifier.height(54.dp))
                OutlinedButton(
                    onClick = onNavigateToRegister, modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, DeepTeal),
                ) { Text("Create new account", color = DeepTeal, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun LoginBrand() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = CircleShape, color = MintSparkle, modifier = Modifier.size(72.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Text("C", color = DeepTeal, fontSize = 42.sp, fontWeight = FontWeight.Black)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("CliNexus", color = DeepTeal, fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp)
    }
}

@Composable
private fun AccountAvatar(photo: String?, name: String, size: Dp) {
    Surface(modifier = Modifier.size(size), shape = CircleShape, color = MintSparkle) {
        if (photo.isNullOrBlank()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Lucide.UserRound, null, tint = DeepTeal, modifier = Modifier.size(size * 0.56f))
            }
        } else {
            SubcomposeAsyncImage(
                model = photo, contentDescription = "$name profile photo", contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(size * 0.38f), color = DeepTeal, strokeWidth = 2.dp)
                    }
                },
                error = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Lucide.UserRound, null, tint = DeepTeal, modifier = Modifier.size(size * 0.56f))
                    }
                },
                success = { SubcomposeAsyncImageContent() },
            )
        }
    }
}

private fun friendlyLoginMessage(message: String?): String {
    val original = message?.trim().orEmpty()
    val normalized = original.lowercase()
    return when {
        original.isBlank() || normalized == "invalid" || "invalid credential" in normalized ||
            "invalid email" in normalized || "incorrect password" in normalized || "unauthorized" in normalized ->
            "The email address or password is incorrect. Please check your details and try again."
        "connect" in normalized || "internet" in normalized || "network" in normalized ->
            "We couldn't connect to CliNexus. Check your internet connection and try again."
        "server" in normalized -> "CliNexus is temporarily unavailable. Please try again in a moment."
        else -> original
    }
}

private fun isCredentialError(message: String?): Boolean {
    val normalized = message?.trim()?.lowercase().orEmpty()
    return normalized.isBlank() || normalized == "invalid" || "invalid credential" in normalized ||
        "invalid email" in normalized || "incorrect password" in normalized || "unauthorized" in normalized
}
