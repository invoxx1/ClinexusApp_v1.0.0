package com.example.clinexusapp.ui.screens.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.clinexusapp.model.UpdateProfileRequest
import com.example.clinexusapp.ui.components.*
import com.example.clinexusapp.ui.screens.auth.AddressDropdown
import com.example.clinexusapp.util.Resource
import com.example.clinexusapp.util.SessionManager
import com.example.clinexusapp.viewmodel.ProfileViewModel
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

@Composable
fun PersonalInformationScreen(onBack: () -> Unit, viewModel: ProfileViewModel) {
    val user by SessionManager.currentUser.collectAsState()
    val updateState by viewModel.updateState.collectAsState()

    val context = LocalContext.current
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        profileImageUri = uri
    }

    var firstName by remember { mutableStateOf(user?.firstName ?: "") }
    var middleName by remember { mutableStateOf(user?.middleName ?: "") }
    var lastName by remember { mutableStateOf(user?.lastName ?: "") }
    var phoneNumber by remember { mutableStateOf(user?.phoneNumber ?: "") }
    var dateOfBirth by remember { mutableStateOf(user?.dateOfBirth ?: "") }
    var streetAddress by remember { mutableStateOf(user?.streetAddress ?: "") }
    var province by remember { mutableStateOf(user?.province ?: "") }
    var city by remember { mutableStateOf(user?.city ?: "") }
    var barangay by remember { mutableStateOf(user?.barangay ?: "") }

    val provinces by viewModel.provinces.collectAsState()
    val cities by viewModel.cities.collectAsState()
    val barangays by viewModel.barangays.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Form validity – all fields including email must be filled
    val isFormValid = (firstName.isNotBlank()) &&
            (lastName.isNotBlank()) &&
            (phoneNumber.isNotBlank()) &&
            (dateOfBirth.isNotBlank()) &&
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
            if (dateOfBirth.isEmpty()) dateOfBirth = it.dateOfBirth ?: ""
            if (streetAddress.isEmpty()) streetAddress = it.streetAddress ?: ""
            if (province.isEmpty()) province = it.province ?: ""
            if (city.isEmpty()) city = it.city ?: ""
            if (barangay.isEmpty()) barangay = it.barangay ?: ""
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
        topBar = { ElegantTopAppBar(title = "Personal Information", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                                Icons.Default.Person,
                                contentDescription = "Add Photo",
                                modifier = Modifier.size(60.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(32.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            shadowElevation = 4.dp
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.padding(6.dp),
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
                    MintTextField(value = firstName, onValueChange = { firstName = it }, label = "First Name", icon = Icons.Default.Person)
                    Spacer(modifier = Modifier.height(12.dp))
                    MintTextField(value = middleName, onValueChange = { middleName = it }, label = "Middle Name", icon = Icons.Default.Badge)
                    Spacer(modifier = Modifier.height(12.dp))
                    MintTextField(value = lastName, onValueChange = { lastName = it }, label = "Last Name", icon = Icons.Default.Person)
                    Spacer(modifier = Modifier.height(12.dp))
                    MintTextField(value = phoneNumber, onValueChange = { phoneNumber = it }, label = "Phone Number", icon = Icons.Default.Phone)
                    Spacer(modifier = Modifier.height(12.dp))
                    MintTextField(value = dateOfBirth, onValueChange = { dateOfBirth = it }, label = "Date of Birth (YYYY-MM-DD)", icon = Icons.Default.CalendarToday)
                }
            }
            item {
                SectionTitle("Address")
                NeumorphicCard {
                    MintTextField(value = streetAddress, onValueChange = { streetAddress = it }, label = "Street Address", icon = Icons.Default.Home)
                    Spacer(modifier = Modifier.height(12.dp))
                    AddressDropdown(
                        label = "Province",
                        options = provinces.map { it.displayName },
                        selectedOption = province,
                        onOptionSelected = { name ->
                            province = name
                            val prov = provinces.find { it.displayName == name }
                            prov?.let { viewModel.onProvinceSelected(it.code) }
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AddressDropdown(
                        label = "City",
                        options = cities.map { it.displayName },
                        selectedOption = city,
                        onOptionSelected = { name ->
                            city = name
                            val c = cities.find { it.displayName == name }
                            c?.let { viewModel.onCitySelected(it.code) }
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AddressDropdown(
                        label = "Barangay",
                        options = barangays.map { it.displayName },
                        selectedOption = barangay,
                        onOptionSelected = { barangay = it }
                    )
                }
            }
            item {
                VibrantButton(
                    text = if (updateState is Resource.Loading) "Updating..." else "Save Changes",
                    onClick = {
                        val imagePart = profileImageUri?.let { uri ->
                            uriToMultipart(context, uri)
                        }
                        viewModel.updateFullProfile(
                            UpdateProfileRequest(
                                email = user?.email ?: "",
                                firstName = firstName,
                                middleName = middleName,
                                lastName = lastName,
                                phoneNumber = phoneNumber,
                                dateOfBirth = dateOfBirth,
                                streetAddress = streetAddress,
                                province = province,
                                city = city,
                                barangay = barangay
                            ),
                            imagePart
                        )
                    },
                    enabled = (updateState !is Resource.Loading) && isFormValid
                )
            }
        }
    }
}

private fun uriToMultipart(context: android.content.Context, uri: Uri): MultipartBody.Part? {
    return try {
        val contentResolver = context.contentResolver
        val file = File(context.cacheDir, "temp_profile_image_${System.currentTimeMillis()}.jpg")
        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        val requestFile = file.asRequestBody(contentResolver.getType(uri)?.toMediaTypeOrNull())
        MultipartBody.Part.createFormData("file", file.name, requestFile)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}