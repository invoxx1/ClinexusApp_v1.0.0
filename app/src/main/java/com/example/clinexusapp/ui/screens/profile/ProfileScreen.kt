package com.example.clinexusapp.ui.screens.profile

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.example.clinexusapp.util.SessionManager
import com.example.clinexusapp.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

private val ProfileBackground = Color(0xFFF1FAF9)
private val ProfileTeal = Color(0xFF009E97)
private val ProfileNavy = Color(0xFF07143C)
private val ProfileMuted = Color(0xFF737C9A)
private val ProfileMint = Color(0xFFE8F7F5)
private val ProfileDivider = Color(0xFFE4E8EF)
private val ProfileRed = Color(0xFFB90829)
private val PanelShape = RoundedCornerShape(20.dp)

private data class ProfileMenuEntry(
    val title: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val destructive: Boolean = false,
    val onClick: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onSwitchAccount: () -> Unit,
    onAddAccount: () -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onNavigateToSettings: () -> Unit,
    onNavigateToPersonalInformation: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToChangePassword: () -> Unit,
    viewModel: ProfileViewModel,
) {
    val user by SessionManager.currentUser.collectAsState()
    val savedAccounts by SessionManager.savedAccounts.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showPhotoOptions by remember { mutableStateOf(false) }
    var showAccountSwitcher by remember { mutableStateOf(false) }

    LaunchedEffect(user) {
        if (user != null && user?.firstName.isNullOrBlank()) viewModel.fetchProfile()
    }
    ProfileSystemBars()

    if (showPhotoOptions) {
        ModalBottomSheet(
            onDismissRequest = { showPhotoOptions = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Profile photo", color = ProfileNavy, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text("Manage your photo from Personal Information.", color = ProfileMuted, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = { showPhotoOptions = false; onNavigateToPersonalInformation() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ProfileTeal),
                ) {
                    Icon(Icons.Outlined.Edit, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Edit profile photo", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showAccountSwitcher) {
        ModalBottomSheet(
            onDismissRequest = { showAccountSwitcher = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Switch account", color = ProfileNavy, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text("Choose a patient account saved on this phone.", color = ProfileMuted, fontSize = 14.sp)
                savedAccounts.filterNot { it.patient.patientID == user?.patientID }.forEach { account ->
                    val patient = account.patient
                    val accountPhoto = account.cachedProfilePicture ?: patient.profilePicture
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable {
                            showAccountSwitcher = false
                            if (account.password.isNullOrBlank()) {
                                SessionManager.requestAccountLogin(patient.patientID)
                                onAddAccount()
                            } else if (SessionManager.switchAccount(patient.patientID)) {
                                onSwitchAccount()
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF7F9FC),
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(46.dp),
                                shape = CircleShape,
                                color = Color.White,
                            ) {
                                if (accountPhoto.isNullOrBlank()) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Person, null, tint = ProfileTeal, modifier = Modifier.size(28.dp))
                                    }
                                } else {
                                    SubcomposeAsyncImage(
                                        model = accountPhoto,
                                        contentDescription = "${patient.firstName.orEmpty()} profile photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                        loading = {
                                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(22.dp),
                                                    color = ProfileTeal,
                                                    strokeWidth = 2.dp,
                                                )
                                            }
                                        },
                                        error = {
                                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Person, null, tint = ProfileTeal, modifier = Modifier.size(28.dp))
                                            }
                                        },
                                        success = { SubcomposeAsyncImageContent() },
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                val accountName = listOf(patient.firstName, patient.lastName).filterNotNull().joinToString(" ").ifBlank { "Patient" }
                                Text(accountName, color = ProfileNavy, fontWeight = FontWeight.Bold)
                                Text(patient.email.orEmpty(), color = ProfileMuted, fontSize = 13.sp)
                            }
                        }
                    }
                }
                OutlinedButton(
                    onClick = { showAccountSwitcher = false; onAddAccount() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, ProfileTeal),
                ) {
                    Icon(Icons.Outlined.PersonAdd, null, tint = ProfileTeal)
                    Spacer(Modifier.width(8.dp))
                    Text("Add account", color = ProfileTeal, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = ProfileBackground,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding()),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                ProfileHero(
                    firstName = user?.firstName.orEmpty(),
                    lastName = user?.lastName.orEmpty(),
                    email = user?.email.orEmpty(),
                    photoUrl = user?.profilePicture,
                    onBack = onBack,
                    onPhotoClick = { showPhotoOptions = true },
                )
            }
            item {
                ProfileSection(
                    title = "Account",
                    entries = listOf(
                        ProfileMenuEntry("Personal Information", "View and manage your details", Icons.Outlined.Person, onClick = onNavigateToPersonalInformation),
                        ProfileMenuEntry("My Appointments", "View and manage your appointments", Icons.Outlined.CalendarMonth, onClick = onNavigateToHistory),
                    ),
                )
            }
            item {
                Spacer(Modifier.height(16.dp))
                ProfileSection(
                    title = "Security and Session",
                    entries = listOf(
                        ProfileMenuEntry("Change Password", "Update your account password", Icons.Outlined.Lock, onClick = onNavigateToChangePassword),
                        ProfileMenuEntry("Switch Account", "Choose or add another account", Icons.Outlined.SwapHoriz) { showAccountSwitcher = true },
                        ProfileMenuEntry("Log Out", "Sign out from this account", Icons.AutoMirrored.Outlined.Logout, destructive = true, onClick = onLogout),
                    ),
                )
            }
        }
    }
}

@Composable
private fun ProfileHero(
    firstName: String,
    lastName: String,
    email: String,
    photoUrl: String?,
    onBack: () -> Unit,
    onPhotoClick: () -> Unit,
) {
    val name = listOf(firstName, lastName).map { it.trim() }.filter { it.isNotBlank() }.joinToString(" ").ifBlank { "Patient" }
    Box(Modifier.fillMaxWidth().height(404.dp)) {
        ProfileHeader(onBack)
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 20.dp).fillMaxWidth().height(235.dp)
                .shadow(10.dp, PanelShape, ambientColor = Color(0xFFB7D8D5).copy(alpha = 0.35f), spotColor = Color.Transparent),
            shape = PanelShape,
            color = Color.White,
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(start = 22.dp, end = 22.dp, top = 91.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(name, color = ProfileNavy, fontSize = 23.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(5.dp))
                Text(email.ifBlank { "Email not available" }, color = ProfileMuted, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(13.dp))
                Surface(shape = RoundedCornerShape(50), color = Color(0xFFE3F6F3)) {
                    Row(Modifier.padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VerifiedUser, null, tint = Color(0xFF087F7A), modifier = Modifier.size(21.dp))
                        Spacer(Modifier.width(9.dp))
                        Text("Verified Patient", color = Color(0xFF087F7A), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Surface(
            modifier = Modifier.align(Alignment.TopCenter).offset(y = 132.dp).size(126.dp),
            shape = CircleShape,
            color = Color.White,
            border = BorderStroke(5.dp, Color.White),
            shadowElevation = 5.dp,
        ) {
            if (photoUrl.isNullOrBlank()) {
                Box(Modifier.fillMaxSize().background(ProfileMint), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, "Profile photo", tint = ProfileTeal, modifier = Modifier.size(76.dp))
                }
            } else {
                AsyncImage(model = photoUrl, contentDescription = "Profile photo", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
        Surface(
            modifier = Modifier.align(Alignment.TopCenter).offset(x = 48.dp, y = 218.dp).size(48.dp)
                .clickable(role = Role.Button, onClick = onPhotoClick),
            shape = CircleShape,
            color = Color(0xFFDBF4F1),
            border = BorderStroke(2.dp, Color.White),
            shadowElevation = 3.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.CameraAlt, "Edit profile photo", tint = Color(0xFF087F7A), modifier = Modifier.size(23.dp))
            }
        }
    }
}

@Composable
private fun ProfileHeader(onBack: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().height(245.dp).drawBehind {
            drawRect(Brush.linearGradient(listOf(Color(0xFF009D9D), Color(0xFF1CB9B5), Color(0xFF69DAD3)), Offset.Zero, Offset(size.width, size.height)))
            val backWave = Path().apply {
                moveTo(0f, size.height * 0.66f)
                cubicTo(size.width * 0.18f, size.height * 0.65f, size.width * 0.25f, size.height * 0.98f, size.width * 0.58f, size.height * 0.97f)
                cubicTo(size.width * 0.78f, size.height * 0.96f, size.width * 0.9f, size.height * 0.75f, size.width, size.height * 0.72f)
                lineTo(size.width, size.height); lineTo(0f, size.height); close()
            }
            drawPath(backWave, Color(0xFF6ADBD4).copy(alpha = 0.72f))
            val frontWave = Path().apply {
                moveTo(0f, size.height * 0.83f)
                cubicTo(size.width * 0.22f, size.height * 1.06f, size.width * 0.46f, size.height * 1.02f, size.width * 0.65f, size.height * 0.98f)
                cubicTo(size.width * 0.82f, size.height * 0.94f, size.width * 0.91f, size.height * 0.83f, size.width, size.height * 0.78f)
                lineTo(size.width, size.height); lineTo(0f, size.height); close()
            }
            drawPath(frontWave, Color(0xFFB7F0EC).copy(alpha = 0.84f))
        }.statusBarsPadding(),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(start = 8.dp, top = 7.dp).size(48.dp)) {
            Icon(Icons.Outlined.ArrowBackIosNew, "Back", tint = Color.White, modifier = Modifier.size(25.dp))
        }
        Column(Modifier.align(Alignment.TopStart).padding(start = 62.dp, top = 12.dp, end = 118.dp)) {
            Text("Profile", color = Color.White, fontSize = 27.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
            Text("Manage your account and preferences", color = Color.White.copy(alpha = 0.84f), fontSize = 14.sp, lineHeight = 19.sp)
        }
        Column(Modifier.align(Alignment.TopEnd).padding(top = 23.dp, end = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.Eco, null, tint = Color.White, modifier = Modifier.size(35.dp))
            Text("Better Care\nBrighter Days", color = Color.White, fontSize = 10.sp, lineHeight = 13.sp, fontFamily = FontFamily.SansSerif)
        }
    }
}

@Composable
private fun ProfileSection(title: String, entries: List<ProfileMenuEntry>) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Text(title, color = ProfileMuted, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 2.dp, bottom = 9.dp).semantics { heading() })
        Surface(
            modifier = Modifier.fillMaxWidth().shadow(7.dp, PanelShape, ambientColor = Color(0xFFB7D8D5).copy(alpha = 0.28f), spotColor = Color.Transparent),
            shape = PanelShape,
            color = Color.White,
        ) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                entries.forEachIndexed { index, entry ->
                    ProfileMenuRow(entry)
                    if (index < entries.lastIndex) HorizontalDivider(color = ProfileDivider, thickness = 1.dp)
                }
            }
        }
    }
}

@Composable
private fun ProfileMenuRow(entry: ProfileMenuEntry) {
    val accent = if (entry.destructive) ProfileRed else Color(0xFF087F7A)
    val background = if (entry.destructive) Color(0xFFFFECEE) else ProfileMint
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 82.dp).clickable(role = Role.Button, onClick = entry.onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(46.dp).background(background, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
            Icon(entry.icon, null, tint = accent, modifier = Modifier.size(25.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(entry.title, color = if (entry.destructive) ProfileRed else ProfileNavy, fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(3.dp))
            Text(entry.subtitle, color = ProfileMuted, fontSize = 13.sp, lineHeight = 17.sp)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = ProfileMuted, modifier = Modifier.size(26.dp))
    }
}

@Composable
private fun ProfileSystemBars() {
    val view = LocalView.current
    DisposableEffect(view) {
        val activity = view.context.findActivity()
        val controller = if (!view.isInEditMode && activity != null) WindowCompat.getInsetsController(activity.window, view) else null
        val previous = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = false
        onDispose { previous?.let { controller.isAppearanceLightStatusBars = it } }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
