package com.example.clinexusapp.ui.screens.settings

import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Bell
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.CircleQuestionMark
import com.composables.icons.lucide.Moon
import com.composables.icons.lucide.ShieldCheck


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.clinexusapp.ui.components.*
import com.example.clinexusapp.ui.theme.*
import com.example.clinexusapp.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onBack: () -> Unit, onLogout: () -> Unit, settingsViewModel: SettingsViewModel) {
    val darkMode by settingsViewModel.isDarkMode.collectAsState()
    var notificationsEnabled by remember { mutableStateOf(value = true) }
    var confirmLogout by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Log out?", fontWeight = FontWeight.Bold) },
            text = { Text("You’ll need to sign in again to access your account.") },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Cancel") } },
            confirmButton = {
                Button(
                    onClick = { confirmLogout = false; onLogout() },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) { Text("Log out") }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding(),)
        ) {
            WavyTealHeader(
                title = "Settings",
                onBack = onBack
            )
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .offset(y = (-30).dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(bottom = 40.dp)
            ) {
                item {
                    NeumorphicCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            SettingsToggleItem(
                                title = "Notifications",
                                icon = Lucide.Bell,
                                iconColor = Color(0xFF0288D1),
                                iconBg = if (isSystemInDarkTheme()) Color(0xFF0288D1).copy(alpha = 0.1f) else Color(0xFFE1F5FE),
                                isChecked = notificationsEnabled,
                            ) {
                                notificationsEnabled = it
                            }
                            SettingsToggleItem(
                                title = "Dark Mode",
                                icon = Lucide.Moon,
                                iconColor = Color(0xFF2C3E50),
                                iconBg = if (isSystemInDarkTheme()) Color(0xFF2C3E50).copy(alpha = 0.1f) else Color(0xFFF1F5F9),
                                isChecked = darkMode,
                                onCheckedChange = { settingsViewModel.toggleDarkMode(it) }
                            )
                        }
                    }
                }

                item {
                    NeumorphicCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            SettingsLinkItem(
                                title = "Privacy & Security", 
                                icon = Lucide.ShieldCheck,
                                iconColor = Color(0xFF00A896),
                                iconBg = if (isSystemInDarkTheme()) Color(0xFF00A896).copy(alpha = 0.1f) else Color(0xFFE0F7F4)
                            ) {
                                scope.launch { snackbarHostState.showSnackbar("Opening: Privacy Settings") }
                            }
                            SettingsLinkItem(
                                title = "Help & Support", 
                                icon = Lucide.CircleQuestionMark,
                                iconColor = Color(0xFF0288D1),
                                iconBg = if (isSystemInDarkTheme()) Color(0xFF0288D1).copy(alpha = 0.1f) else Color(0xFFE1F5FE)
                            ) {
                                scope.launch { snackbarHostState.showSnackbar("Redirecting to: Help Center") }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    VibrantButton(
                        text = "Log Out",
                        onClick = { confirmLogout = true }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsToggleItem(title: String, icon: ImageVector, iconColor: Color, iconBg: Color, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(iconBg, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.width(18.dp))
        Text(text = title, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp)
        Switch(
            checked = isChecked, 
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = White,
                checkedTrackColor = DeepTeal,
                uncheckedThumbColor = White,
                uncheckedTrackColor = Color.LightGray.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
fun SettingsLinkItem(title: String, icon: ImageVector, iconColor: Color, iconBg: Color, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(iconBg, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.width(18.dp))
        Text(text = title, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp)
        Icon(Lucide.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), modifier = Modifier.size(22.dp))
    }
}
