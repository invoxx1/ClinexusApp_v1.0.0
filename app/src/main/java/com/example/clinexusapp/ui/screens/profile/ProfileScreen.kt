package com.example.clinexusapp.ui.screens.profile

import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.ArrowLeftRight
import com.composables.icons.lucide.CalendarDays
import com.composables.icons.lucide.Camera
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Leaf
import com.composables.icons.lucide.LockKeyhole
import com.composables.icons.lucide.LogOut
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.ShieldCheck
import com.composables.icons.lucide.UserRound
import com.composables.icons.lucide.UserRoundPlus
import com.composables.icons.lucide.ImagePlus
import com.composables.icons.lucide.Trash2


import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
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
import com.example.clinexusapp.util.Resource
import java.io.File
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
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var cameraPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var confirmRemovePhoto by remember { mutableStateOf(false) }
    var showAccountSwitcher by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val updateState by viewModel.updateState.collectAsState()
    val galleryPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        selectedPhotoUri = uri
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        if (saved) selectedPhotoUri = cameraPhotoUri
    }

    fun launchCamera() {
        val directory = File(context.cacheDir, "profile_photos").apply { mkdirs() }
        val file = File(directory, "profile_${System.currentTimeMillis()}.jpg")
        cameraPhotoUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        cameraLauncher.launch(cameraPhotoUri!!)
    }

    LaunchedEffect(user) {
        if (user != null && user?.firstName.isNullOrBlank()) viewModel.fetchProfile()
    }
    ProfileSystemBars()

    LaunchedEffect(updateState) {
        when (val result = updateState) {
            is Resource.Success -> {
                selectedPhotoUri = null
                showPhotoOptions = false
                snackbarHostState.showSnackbar("Profile photo updated")
                viewModel.resetState()
            }
            is Resource.Error -> {
                snackbarHostState.showSnackbar(result.message ?: "Unable to update profile photo")
                viewModel.resetState()
            }
            else -> Unit
        }
    }

    if (confirmRemovePhoto) {
        AlertDialog(
            onDismissRequest = { confirmRemovePhoto = false },
            title = { Text("Remove profile photo?", fontWeight = FontWeight.Bold) },
            text = { Text("Your account will use the default profile icon.") },
            dismissButton = { TextButton(onClick = { confirmRemovePhoto = false }) { Text("Cancel") } },
            confirmButton = {
                Button(
                    onClick = { confirmRemovePhoto = false; viewModel.updateProfilePhoto(remove = true) },
                    colors = ButtonDefaults.buttonColors(containerColor = ProfileRed)
                ) { Text("Remove") }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White,
        )
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Log out?", color = ProfileNavy, fontWeight = FontWeight.Bold) },
            text = { Text("You’ll need to sign in again to access your appointments and messages.") },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Stay signed in") } },
            confirmButton = {
                Button(
                    onClick = { confirmLogout = false; onLogout() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) { Text("Log out") }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White,
        )
    }

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
                Text("Take a new photo or choose one from your gallery.", color = ProfileMuted, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Surface(
                    modifier = Modifier.align(Alignment.CenterHorizontally).size(112.dp),
                    shape = CircleShape,
                    color = ProfileMint,
                ) {
                    val preview = selectedPhotoUri ?: user?.profilePicture
                    if (preview != null) AsyncImage(preview, "Profile photo preview", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Lucide.UserRound, null, tint = ProfileTeal, modifier = Modifier.size(52.dp)) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { launchCamera() },
                        modifier = Modifier.weight(1f).heightIn(min = 50.dp),
                        shape = RoundedCornerShape(15.dp),
                    ) { Icon(Lucide.Camera, null); Spacer(Modifier.width(6.dp)); Text("Take photo") }
                    OutlinedButton(
                        onClick = { galleryPicker.launch("image/*") },
                        modifier = Modifier.weight(1f).heightIn(min = 50.dp),
                        shape = RoundedCornerShape(15.dp),
                    ) { Icon(Lucide.ImagePlus, null); Spacer(Modifier.width(6.dp)); Text("Gallery") }
                }
                Button(
                    onClick = { selectedPhotoUri?.let { viewModel.updateProfilePhoto(uriToMultipart(context, it)) } },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = selectedPhotoUri != null && updateState !is Resource.Loading,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ProfileTeal),
                ) {
                    if (updateState is Resource.Loading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    else Icon(Lucide.Pencil, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Save photo", fontWeight = FontWeight.Bold)
                }
                if (!user?.profilePicture.isNullOrBlank()) {
                    TextButton(
                        onClick = { confirmRemovePhoto = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = updateState !is Resource.Loading,
                        colors = ButtonDefaults.textButtonColors(contentColor = ProfileRed),
                    ) { Icon(Lucide.Trash2, null); Spacer(Modifier.width(7.dp)); Text("Remove current photo") }
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
                                        Icon(Lucide.UserRound, null, tint = ProfileTeal, modifier = Modifier.size(28.dp))
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
                                                Icon(Lucide.UserRound, null, tint = ProfileTeal, modifier = Modifier.size(28.dp))
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
                    Icon(Lucide.UserRoundPlus, null, tint = ProfileTeal)
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
                        ProfileMenuEntry("Personal Information", "View and manage your details", Lucide.UserRound, onClick = onNavigateToPersonalInformation),
                        ProfileMenuEntry("My Appointments", "View and manage your appointments", Lucide.CalendarDays, onClick = onNavigateToHistory),
                    ),
                )
            }
            item {
                Spacer(Modifier.height(16.dp))
                ProfileSection(
                    title = "Security and Session",
                    entries = listOf(
                        ProfileMenuEntry("Change Password", "Update your account password", Lucide.LockKeyhole, onClick = onNavigateToChangePassword),
                        ProfileMenuEntry("Switch Account", "Choose or add another account", Lucide.ArrowLeftRight) { showAccountSwitcher = true },
                        ProfileMenuEntry("Log Out", "Sign out from this account", Lucide.LogOut, destructive = true) { confirmLogout = true },
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
                        Icon(Lucide.ShieldCheck, null, tint = Color(0xFF087F7A), modifier = Modifier.size(21.dp))
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
                    Icon(Lucide.UserRound, "Profile photo", tint = ProfileTeal, modifier = Modifier.size(76.dp))
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
                Icon(Lucide.Camera, "Edit profile photo", tint = Color(0xFF087F7A), modifier = Modifier.size(23.dp))
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
            Icon(Lucide.ChevronLeft, "Back", tint = Color.White, modifier = Modifier.size(25.dp))
        }
        Column(Modifier.align(Alignment.TopStart).padding(start = 62.dp, top = 12.dp, end = 118.dp)) {
            Text("Profile", color = Color.White, fontSize = 27.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
            Text("Manage your account and preferences", color = Color.White.copy(alpha = 0.84f), fontSize = 14.sp, lineHeight = 19.sp)
        }
        Column(Modifier.align(Alignment.TopEnd).padding(top = 23.dp, end = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Lucide.Leaf, null, tint = Color.White, modifier = Modifier.size(35.dp))
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
        Icon(Lucide.ChevronRight, null, tint = ProfileMuted, modifier = Modifier.size(26.dp))
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
