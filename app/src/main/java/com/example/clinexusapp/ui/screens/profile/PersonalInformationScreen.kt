package com.example.clinexusapp.ui.screens.profile

import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.CalendarDays
import com.composables.icons.lucide.CircleAlert
import com.composables.icons.lucide.CircleCheck
import com.composables.icons.lucide.House
import com.composables.icons.lucide.IdCard
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.Phone
import com.composables.icons.lucide.UserRound


import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.clinexusapp.model.UpdateProfileRequest
import com.example.clinexusapp.ui.components.*
import com.example.clinexusapp.ui.screens.auth.AddressDropdown
import com.example.clinexusapp.util.Resource
import com.example.clinexusapp.util.SessionManager
import com.example.clinexusapp.util.createProfileImagePart
import com.example.clinexusapp.util.isValidBirthDate
import com.example.clinexusapp.util.isValidPhilippineMobile
import com.example.clinexusapp.viewmodel.ProfileViewModel
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

private fun normalizeDateOfBirth(value: String?): String = value.orEmpty().substringBefore('T')

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalInformationScreen(onBack: () -> Unit, viewModel: ProfileViewModel) {
    val user by SessionManager.currentUser.collectAsState()
    val updateState by viewModel.updateState.collectAsState()

    val context = LocalContext.current
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var showPhotoEditor by remember { mutableStateOf(false) }
    var photoZoom by remember { mutableFloatStateOf(1f) }
    var photoOffset by remember { mutableStateOf(Offset.Zero) }
    var cropFrameSizePx by remember { mutableFloatStateOf(1f) }
    val photoEditorSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let {
            profileImageUri = it
            photoZoom = 1f
            photoOffset = Offset.Zero
            showPhotoEditor = true
        }
    }

    var firstName by remember { mutableStateOf(user?.firstName ?: "") }
    var middleName by remember { mutableStateOf(user?.middleName ?: "") }
    var lastName by remember { mutableStateOf(user?.lastName ?: "") }
    var phoneNumber by remember { mutableStateOf(user?.phoneNumber ?: "") }
    var dateOfBirth by remember { mutableStateOf(normalizeDateOfBirth(user?.dateOfBirth)) }
    var streetAddress by remember { mutableStateOf(user?.streetAddress ?: "") }
    var province by remember { mutableStateOf(user?.province ?: "") }
    var city by remember { mutableStateOf(user?.city ?: "") }
    var barangay by remember { mutableStateOf(user?.barangay ?: "") }

    val provinces by viewModel.provinces.collectAsState()
    val cities by viewModel.cities.collectAsState()
    val barangays by viewModel.barangays.collectAsState()
    val addressLoading by viewModel.addressLoading.collectAsState()
    val addressError by viewModel.addressError.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var confirmSave by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }

    val hasUnsavedChanges = user?.let {
        firstName != it.firstName.orEmpty() || middleName != it.middleName.orEmpty() ||
            lastName != it.lastName.orEmpty() || phoneNumber != it.phoneNumber.orEmpty() ||
            dateOfBirth != normalizeDateOfBirth(it.dateOfBirth) || streetAddress != it.streetAddress.orEmpty() ||
            province != it.province.orEmpty() || city != it.city.orEmpty() ||
            barangay != it.barangay.orEmpty() || profileImageUri != null
    } ?: false
    val handleBack = { if (hasUnsavedChanges) confirmDiscard = true else onBack() }
    BackHandler(onBack = handleBack)

    val saveProfile = {
        val imagePart = profileImageUri?.let { uri ->
            createProfileImagePart(
                context = context,
                uri = uri,
                zoom = photoZoom,
                panFractionX = photoOffset.x / cropFrameSizePx,
                panFractionY = photoOffset.y / cropFrameSizePx,
            )
        }
        viewModel.updateFullProfile(
            UpdateProfileRequest(
                email = user?.email ?: "",
                firstName = firstName,
                middleName = middleName,
                lastName = lastName,
                phoneNumber = phoneNumber,
                dateOfBirth = normalizeDateOfBirth(dateOfBirth),
                streetAddress = streetAddress,
                province = province,
                city = city,
                barangay = barangay
            ),
            imagePart
        )
    }

    if (confirmSave) {
        AlertDialog(
            onDismissRequest = { confirmSave = false },
            title = { Text("Save profile changes?") },
            text = { Text("Please confirm that your personal information is correct.") },
            dismissButton = { TextButton(onClick = { confirmSave = false }) { Text("Review") } },
            confirmButton = { Button(onClick = { confirmSave = false; saveProfile() }) { Text("Save changes") } },
            shape = RoundedCornerShape(24.dp)
        )
    }
    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard changes?") },
            text = { Text("Your unsaved profile changes will be lost.") },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Keep editing") } },
            confirmButton = {
                Button(
                    onClick = { confirmDiscard = false; onBack() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Discard") }
            },
        )
    }

    if (showPhotoEditor && profileImageUri != null) {
        ModalBottomSheet(
            onDismissRequest = { showPhotoEditor = false },
            sheetState = photoEditorSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        ) {
            Column(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("Edit profile photo", style = MaterialTheme.typography.headlineSmall)
                Text("Drag to reposition and pinch to zoom.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box(
                    modifier = Modifier.size(236.dp).background(Color.Black.copy(alpha = 0.72f), RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        modifier = Modifier.size(204.dp).onSizeChanged { cropFrameSizePx = it.width.toFloat() },
                        shape = CircleShape,
                        border = BorderStroke(3.dp, Color.White),
                    ) {
                        AsyncImage(
                            model = profileImageUri,
                            contentDescription = "Profile photo crop preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                                .pointerInput(profileImageUri, cropFrameSizePx) {
                                    detectTransformGestures { _, pan, zoomChange, _ ->
                                        val newZoom = (photoZoom * zoomChange).coerceIn(1f, 4f)
                                        val maxPan = cropFrameSizePx * (newZoom - 1f) / 2f
                                        photoZoom = newZoom
                                        photoOffset = if (maxPan <= 0f) Offset.Zero else Offset(
                                            (photoOffset.x + pan.x).coerceIn(-maxPan, maxPan),
                                            (photoOffset.y + pan.y).coerceIn(-maxPan, maxPan),
                                        )
                                    }
                                }
                                .graphicsLayer(
                                    scaleX = photoZoom,
                                    scaleY = photoZoom,
                                    translationX = photoOffset.x,
                                    translationY = photoOffset.y,
                                ),
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { photoZoom = 1f; photoOffset = Offset.Zero },
                        modifier = Modifier.weight(1f).height(50.dp),
                    ) { Text("Reset") }
                    Button(
                        onClick = { showPhotoEditor = false },
                        modifier = Modifier.weight(1f).height(50.dp),
                    ) { Text("Apply photo") }
                }
            }
        }
    }

    // Form validity – all fields including email must be filled
    val isFormValid = (firstName.isNotBlank()) &&
            (lastName.isNotBlank()) &&
            isValidPhilippineMobile(phoneNumber) &&
            isValidBirthDate(dateOfBirth) &&
            (streetAddress.isNotBlank()) &&
            (province.isNotBlank()) &&
            (city.isNotBlank()) &&
            (barangay.isNotBlank()) &&
            (user?.email?.isNotBlank() == true)

    LaunchedEffect(user) {
        user?.let {
            if (firstName.isEmpty()) firstName = it.firstName ?: ""
            if (middleName.isEmpty()) middleName = it.middleName ?: ""
            if (lastName.isEmpty()) lastName = it.lastName ?: ""
            if (phoneNumber.isEmpty()) phoneNumber = it.phoneNumber ?: ""
            if (dateOfBirth.isEmpty()) dateOfBirth = normalizeDateOfBirth(it.dateOfBirth)
            if (streetAddress.isEmpty()) streetAddress = it.streetAddress ?: ""
            if (province.isEmpty()) province = it.province ?: ""
            if (city.isEmpty()) city = it.city ?: ""
            if (barangay.isEmpty()) barangay = it.barangay ?: ""
        }
    }

    LaunchedEffect(user?.patientID) {
        user?.let {
            viewModel.loadAddressOptionsForProfile(it.province.orEmpty(), it.city.orEmpty())
        }
    }

    val state = updateState
    LaunchedEffect(state) {
        when (state) {
            is Resource.Success -> {
                snackbarHostState.showSnackbar("Profile updated successfully")
                profileImageUri = null // Clear local selection to show updated server image
                viewModel.resetState()
            }
            is Resource.Error -> {
                snackbarHostState.showSnackbar(state.message ?: "Update failed")
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = { ElegantTopAppBar(title = "Personal Information", onBack = handleBack) },
        snackbarHost = { ClinexusSnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 24.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(112.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .clickable { imagePickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (profileImageUri != null) {
                                AsyncImage(
                                    model = profileImageUri,
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else if (!user?.profilePicture.isNullOrEmpty()) {
                                AsyncImage(
                                    model = user?.profilePicture,
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    Lucide.UserRound,
                                    contentDescription = "Add Photo",
                                    modifier = Modifier.size(60.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Surface(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(36.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            shadowElevation = 5.dp,
                            border = androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.background)
                        ) {
                            Icon(
                                Lucide.Pencil,
                                contentDescription = "Change profile picture",
                                modifier = Modifier.padding(8.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Change Profile Picture",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            item {
                SectionTitle("Personal Details")
                NeumorphicCard {
                    MintTextField(value = firstName, onValueChange = { firstName = it }, label = "First Name", icon = Lucide.UserRound, required = true)
                    Spacer(modifier = Modifier.height(12.dp))
                    MintTextField(value = middleName, onValueChange = { middleName = it }, label = "Middle Name", icon = Lucide.IdCard)
                    Spacer(modifier = Modifier.height(12.dp))
                    MintTextField(value = lastName, onValueChange = { lastName = it }, label = "Last Name", icon = Lucide.UserRound, required = true)
                    Spacer(modifier = Modifier.height(12.dp))
                    MintTextField(value = phoneNumber, onValueChange = { phoneNumber = it }, label = "Phone Number", icon = Lucide.Phone, required = true, errorText = phoneNumber.takeIf { it.isNotBlank() && !isValidPhilippineMobile(it) }?.let { "Use 09XXXXXXXXX or +639XXXXXXXXX" })
                    Spacer(modifier = Modifier.height(12.dp))
                    DatePickerField(value = dateOfBirth, onValueChange = { dateOfBirth = it }, errorText = dateOfBirth.takeIf { it.isNotBlank() && !isValidBirthDate(it) }?.let { "Choose a valid past date" })
                }
            }
            item {
                SectionTitle("Address")
                NeumorphicCard {
                    MintTextField(value = streetAddress, onValueChange = { streetAddress = it }, label = "Street Address", icon = Lucide.House)
                    Spacer(modifier = Modifier.height(12.dp))
                    AddressDropdown(
                        label = "Province",
                        options = provinces.map { it.displayName },
                        selectedOption = province,
                        onOptionSelected = { name ->
                            province = name
                            city = ""
                            barangay = ""
                            val prov = provinces.find { it.displayName == name }
                            prov?.let { viewModel.onProvinceSelected(it.code) }
                        },
                        loading = addressLoading == "Province",
                        errorMessage = addressError?.takeIf { provinces.isEmpty() },
                        onRetry = { viewModel.retryAddressOptions("Province") },
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AddressDropdown(
                        label = "City",
                        options = cities.map { it.displayName },
                        selectedOption = city,
                        onOptionSelected = { name ->
                            city = name
                            barangay = ""
                            val c = cities.find { it.displayName == name }
                            c?.let { viewModel.onCitySelected(it.code) }
                        },
                        enabled = province.isNotBlank(),
                        loading = addressLoading == "City",
                        errorMessage = addressError?.takeIf { cities.isEmpty() },
                        onRetry = { viewModel.retryAddressOptions("City") },
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AddressDropdown(
                        label = "Barangay",
                        options = barangays.map { it.displayName },
                        selectedOption = barangay,
                        onOptionSelected = { barangay = it },
                        enabled = city.isNotBlank(),
                        loading = addressLoading == "Barangay",
                        errorMessage = addressError?.takeIf { barangays.isEmpty() },
                        onRetry = { viewModel.retryAddressOptions("Barangay") },
                    )
                }
            }
            item {
                VibrantButton(
                    text = if (updateState is Resource.Loading) "Updating..." else "Save Changes",
                    onClick = { confirmSave = true },
                    enabled = (updateState !is Resource.Loading) && isFormValid
                )
            }
        }
    }
}

internal fun uriToMultipart(context: android.content.Context, uri: Uri): MultipartBody.Part? {
    return createProfileImagePart(context, uri)
}
