package com.example.clinexusapp.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import com.example.clinexusapp.ui.components.*
import com.example.clinexusapp.ui.theme.*
import com.example.clinexusapp.util.SessionManager
import com.example.clinexusapp.viewmodel.ProfileViewModel
import com.example.clinexusapp.util.Resource
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPersonalInformation: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToChangePassword: () -> Unit,
    viewModel: ProfileViewModel,
) {
    val user by SessionManager.currentUser.collectAsState()
    val updateState by viewModel.updateState.collectAsState()

    // Options Dialog State
    var showOptions by remember { mutableStateOf(false) }
    var showFullImage by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    // Auto-fetch if names are missing
    LaunchedEffect(user) {
        if ((user != null) && (user?.firstName == null)) {
            viewModel.fetchProfile()
        }
    }

    val displayFirstName = if (!user?.firstName.isNullOrBlank()) user!!.firstName!! else "Patient"
    val displayLastName = user?.lastName ?: ""
    val defaultEmail = user?.email ?: "Not available"

    var isEditing by remember { mutableStateOf(value = false) }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    LaunchedEffect(user) {
        if (!isEditing) {
            firstName = user?.firstName ?: ""
            lastName = user?.lastName ?: ""
            email = user?.email ?: ""
        }
    }

    LaunchedEffect(updateState) {
        if (updateState is Resource.Success) {
            isEditing = false
            viewModel.resetState()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            WavyTealHeader(
                title = "Profile",
                onBack = onBack,
                onSettingsClick = onNavigateToSettings,
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .offset(y = (-15).dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    modifier = Modifier
                        .size(110.dp)
                        .clickable { showOptions = true },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    border = androidx.compose.foundation.BorderStroke(4.dp, MaterialTheme.colorScheme.surface)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (!user?.profilePicture.isNullOrEmpty()) {
                            AsyncImage(
                                model = user?.profilePicture,
                                contentDescription = "Profile Picture",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.Person,
                                null,
                                modifier = Modifier.size(70.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (showOptions) {
                    ModalBottomSheet(
                        onDismissRequest = { showOptions = false },
                        sheetState = sheetState,
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                                .padding(bottom = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Profile Options",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            ProfileOptionItem(
                                title = "View Profile Picture",
                                icon = Icons.Default.Visibility,
                                onClick = {
                                    showOptions = false
                                    showFullImage = true
                                }
                            )
                            
                            ProfileOptionItem(
                                title = "Edit Profile",
                                icon = Icons.Default.Edit,
                                onClick = {
                                    showOptions = false
                                    onNavigateToPersonalInformation()
                                }
                            )
                        }
                    }
                }

                if (showFullImage) {
                    Dialog(onDismissRequest = { showFullImage = false }) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { showFullImage = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .aspectRatio(1f),
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                if (!user?.profilePicture.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = user?.profilePicture,
                                        contentDescription = "Full Profile Picture",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Person,
                                        null,
                                        modifier = Modifier.fillMaxSize().padding(48.dp),
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                NeumorphicCard(modifier = Modifier.fillMaxWidth()) {
                    if (isEditing) {
                        MintTextField(value = firstName, onValueChange = { firstName = it }, label = "First Name", icon = Icons.Default.Badge)
                        Spacer(modifier = Modifier.height(16.dp))
                        MintTextField(value = lastName, onValueChange = { lastName = it }, label = "Last Name", icon = Icons.Default.Badge)
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text(text = if (displayFirstName != "Patient") "$displayFirstName $displayLastName" else displayFirstName, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
                            Text(text = defaultEmail, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Verified, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "VERIFIED PATIENT",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ProfileMenuItem(
                        title = "Personal Information",
                        icon = Icons.Default.PersonOutline,
                        iconColor = Color(0xFF00C9B1),
                        iconBg = Color(0xFFE0F7F4),
                        onClick = onNavigateToPersonalInformation
                    )
                    ProfileMenuItem(
                        title = "My Appointments",
                        icon = Icons.AutoMirrored.Filled.EventNote,
                        iconColor = DeepTeal,
                        iconBg = MintSparkle,
                        onClick = onNavigateToHistory
                    )
                    ProfileMenuItem(
                        title = "Medical Records",
                        icon = Icons.Default.MedicalInformation,
                        iconColor = Color(0xFF0288D1),
                        iconBg = Color(0xFFE1F5FE)
                    ) {
                        scope.launch { snackbarHostState.showSnackbar("ACCESSING: Clinical Records") }
                    }
                    ProfileMenuItem(
                        title = "Change Password",
                        icon = Icons.Default.LockOpen,
                        iconColor = Color(0xFF64748B),
                        iconBg = Color(0xFFF1F5F9)
                    ) {
                        onNavigateToChangePassword()
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))
                VibrantButton(
                    text = if (isEditing) (if (updateState is Resource.Loading) "Saving..." else "Save Profile") else "Edit Profile",
                    onClick = {
                        if (isEditing) {
                            viewModel.updateProfile(firstName, lastName, email)
                        } else {
                            isEditing = true
                        }
                    },
                    enabled = updateState !is Resource.Loading
                )

                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onLogout) {
                    Text(text = "Log Out", color = ErrorRed, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun ProfileOptionItem(title: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ProfileMenuItem(title: String, icon: ImageVector, iconColor: Color, iconBg: Color, onClick: () -> Unit) {
    NeumorphicCard(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(iconBg, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(18.dp))
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp
            )
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), modifier = Modifier.size(22.dp))
        }
    }
}
