package com.example.clinexusapp.ui.screens.auth

import com.example.clinexusapp.util.passwordsMeetRules

import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.CalendarDays
import com.composables.icons.lucide.Camera
import com.composables.icons.lucide.House
import com.composables.icons.lucide.IdCard
import com.composables.icons.lucide.KeyRound
import com.composables.icons.lucide.LockKeyhole
import com.composables.icons.lucide.Mail
import com.composables.icons.lucide.Phone
import com.composables.icons.lucide.UserRound


import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.clinexusapp.model.*
import com.example.clinexusapp.ui.components.*
import com.example.clinexusapp.ui.theme.*
import com.example.clinexusapp.util.Resource
import com.example.clinexusapp.util.isValidBirthDate
import com.example.clinexusapp.util.isValidPhilippineMobile
import com.example.clinexusapp.viewmodel.RegisterViewModel
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel,
    onRegisterSuccess: (String) -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    var firstName by remember { mutableStateOf("") }
    var middleName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // Address State
    var streetAddress by remember { mutableStateOf("") }
    var selectedRegion by remember { mutableStateOf<Region?>(null) }
    var selectedProvince by remember { mutableStateOf<Province?>(null) }
    var selectedCity by remember { mutableStateOf<City?>(null) }
    var selectedBarangay by remember { mutableStateOf<Barangay?>(null) }

    val regions by viewModel.regions.collectAsState()
    val provinces by viewModel.provinces.collectAsState()
    val cities by viewModel.cities.collectAsState()
    val barangays by viewModel.barangays.collectAsState()
    val addressLoading by viewModel.addressLoading.collectAsState()
    val addressError by viewModel.addressError.collectAsState()

    val registerState by viewModel.registerState.collectAsState()
    val validationError by viewModel.validationError.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val context = LocalContext.current
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        profileImageUri = uri
    }

    // 🟢 Compute form validity – all text fields and dropdowns must be filled
    val isFormValid = (firstName.isNotBlank()) &&
            (lastName.isNotBlank()) &&
            (email.isNotBlank()) &&
            isValidPhilippineMobile(phoneNumber) &&
            isValidBirthDate(dateOfBirth) &&
            passwordsMeetRules(password, confirmPassword) &&
            (confirmPassword.isNotBlank()) &&
            (streetAddress.isNotBlank()) &&
            (selectedProvince != null) &&
            (selectedCity != null) &&
            (selectedBarangay != null)

    val state = registerState
    LaunchedEffect(state, validationError) {
        when (state) {
            is Resource.Success -> {
                onRegisterSuccess(email)
                viewModel.resetState()
            }
            is Resource.Error -> {
                snackbarHostState.showSnackbar(state.message ?: "Registration failed")
            }
            else -> {}
        }

        validationError?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        snackbarHost = { ClinexusSnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(top = 40.dp, bottom = 40.dp),
        ) {
            item {
                Text(
                    text = "Registration",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Join our network of elite care",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            item {
                SectionTitle("Profile Picture (Optional)")
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center,
                ) {
                    if (profileImageUri != null) {
                        AsyncImage(
                            model = profileImageUri,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Icon(
                            Lucide.Camera,
                            contentDescription = "Add Photo",
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            item {
                SectionTitle("Identity Details")
                NeumorphicCard {
                    MintTextField(value = firstName, onValueChange = { firstName = it }, label = "First Name", icon = Lucide.UserRound, placeholder = "Enter your first name")
                    Spacer(modifier = Modifier.height(16.dp))
                    MintTextField(value = middleName, onValueChange = { middleName = it }, label = "Middle Name", icon = Lucide.IdCard, placeholder = "Optional")
                    Spacer(modifier = Modifier.height(16.dp))
                    MintTextField(value = lastName, onValueChange = { lastName = it }, label = "Last Name", icon = Lucide.UserRound, placeholder = "Enter your last name")

                    Spacer(modifier = Modifier.height(20.dp))
                    SectionTitle("Address Information")
                    MintTextField(value = streetAddress, onValueChange = { streetAddress = it }, label = "Street Address", icon = Lucide.House, placeholder = "House number and street")

                    Spacer(modifier = Modifier.height(16.dp))
                    AddressDropdown(
                        label = "Region",
                        options = regions.map { it.regionName },
                        selectedOption = selectedRegion?.regionName ?: "",
                        onOptionSelected = { name ->
                            val region = regions.find { it.regionName == name }
                            selectedRegion = region
                            selectedProvince = null
                            selectedCity = null
                            selectedBarangay = null
                            region?.let { viewModel.onRegionSelected(it.code) }
                        },
                        loading = addressLoading == "Region",
                        errorMessage = addressError?.takeIf { regions.isEmpty() },
                        onRetry = { viewModel.retryAddress("Region") },
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    AddressDropdown(
                        label = "Province",
                        options = provinces.map { it.displayName },
                        selectedOption = selectedProvince?.displayName ?: "",
                        onOptionSelected = { name ->
                            val province = provinces.find { it.displayName == name }
                            selectedProvince = province
                            selectedCity = null
                            selectedBarangay = null
                            province?.let { viewModel.onProvinceSelected(it.code) }
                        },
                        enabled = selectedRegion != null,
                        loading = addressLoading == "Province",
                        errorMessage = addressError?.takeIf { provinces.isEmpty() },
                        onRetry = { viewModel.retryAddress("Province") },
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    AddressDropdown(
                        label = "City / Municipality",
                        options = cities.map { it.displayName },
                        selectedOption = selectedCity?.displayName ?: "",
                        onOptionSelected = { name ->
                            val city = cities.find { it.displayName == name }
                            selectedCity = city
                            selectedBarangay = null
                            city?.let { viewModel.onCitySelected(it.code) }
                        },
                        enabled = selectedProvince != null,
                        loading = addressLoading == "City / Municipality",
                        errorMessage = addressError?.takeIf { cities.isEmpty() },
                        onRetry = { viewModel.retryAddress("City / Municipality") },
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    AddressDropdown(
                        label = "Barangay",
                        options = barangays.map { it.displayName },
                        selectedOption = selectedBarangay?.displayName ?: "",
                        onOptionSelected = { name ->
                            selectedBarangay = barangays.find { it.displayName == name }
                        },
                        enabled = selectedCity != null,
                        loading = addressLoading == "Barangay",
                        errorMessage = addressError?.takeIf { barangays.isEmpty() },
                        onRetry = { viewModel.retryAddress("Barangay") },
                    )
                }
            }

            item {
                SectionTitle("Network Details")
                NeumorphicCard {
                    MintTextField(value = email, onValueChange = { email = it }, label = "Email", icon = Lucide.Mail, placeholder = "name@example.com")
                    Spacer(modifier = Modifier.height(16.dp))
                    MintTextField(value = phoneNumber, onValueChange = { phoneNumber = it }, label = "Mobile Number", icon = Lucide.Phone, required = true, placeholder = "09XXXXXXXXX", errorText = phoneNumber.takeIf { it.isNotBlank() && !isValidPhilippineMobile(it) }?.let { "Use 09XXXXXXXXX or +639XXXXXXXXX" })
                    Spacer(modifier = Modifier.height(16.dp))
                    DatePickerField(value = dateOfBirth, onValueChange = { dateOfBirth = it }, label = "Birthday", errorText = dateOfBirth.takeIf { it.isNotBlank() && !isValidBirthDate(it) }?.let { "Choose a valid past date" })
                }
            }

            item {
                SectionTitle("Security")
                NeumorphicCard {
                    MintTextField(value = password, onValueChange = { password = it }, label = "Password", icon = Lucide.LockKeyhole, isPassword = true, placeholder = "Create a strong password")
                    Spacer(modifier = Modifier.height(16.dp))
                    MintTextField(value = confirmPassword, onValueChange = { confirmPassword = it }, label = "Confirm Password", icon = Lucide.KeyRound, isPassword = true, placeholder = "Enter the password again")
                    Spacer(modifier = Modifier.height(12.dp))
                    PasswordRequirements(password, confirmPassword)
                }
            }

            item {
                VibrantButton(
                    text = if (registerState is Resource.Loading) "Processing..." else "Create Account",
                    onClick = {
                        val imagePart = profileImageUri?.let { uri ->
                            uriToMultipart(context, uri)
                        }
                        viewModel.register(
                            email, password, confirmPassword, firstName, middleName, lastName, phoneNumber, dateOfBirth,
                            streetAddress,
                            selectedProvince?.displayName ?: "",
                            selectedCity?.displayName ?: "",
                            selectedBarangay?.displayName ?: "",
                            imagePart,
                        )
                    },
                    // 🟢 Button is enabled only if form is valid and not already loading
                    enabled = (registerState !is Resource.Loading) && isFormValid,
                )

                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    Text("Have an account? ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = onNavigateToLogin) {
                        Text("Sign In", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

private fun uriToMultipart(context: android.content.Context, uri: Uri): MultipartBody.Part? =
    com.example.clinexusapp.util.createProfileImagePart(context, uri)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressDropdown(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    enabled: Boolean = true,
    loading: Boolean = false,
    errorMessage: String? = null,
    onRetry: (() -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(value = false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 6.dp, bottom = 8.dp),
        )
        ExposedDropdownMenuBox(
            expanded = expanded && enabled,
            onExpandedChange = { if (enabled) expanded = !expanded },
            modifier = Modifier.fillMaxWidth().semantics {
                contentDescription = "$label selector"
                stateDescription = when {
                    loading -> "Loading"
                    errorMessage != null -> "Could not load options"
                    selectedOption.isBlank() -> "No option selected"
                    else -> selectedOption
                }
            },
        ) {
            OutlinedTextField(
                value = selectedOption,
                onValueChange = {},
                readOnly = true,
                placeholder = { Text("Select $label", fontSize = 14.sp) },
                trailingIcon = {
                    if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier.menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                enabled = enabled,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    disabledContainerColor = Color.LightGray.copy(alpha = 0.1f),
                    disabledBorderColor = Color.Transparent,
                    disabledTextColor = Color.Gray,
                ),
            )
            ExposedDropdownMenu(
                expanded = expanded && enabled,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface),
            ) {
                if (options.isEmpty() && loading) {
                    DropdownMenuItem(text = { Text("Loading options…") }, onClick = {}, enabled = false)
                }
                options.forEach { selectionOption ->
                    DropdownMenuItem(
                        text = { Text(selectionOption) },
                        onClick = {
                            onOptionSelected(selectionOption)
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }
        if (errorMessage != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Couldn’t load $label",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f),
                )
                if (onRetry != null) {
                    TextButton(onClick = onRetry, contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Text("Retry", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
