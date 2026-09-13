package com.example.clinexusapp.ui.screens.profile

import android.os.Build
import coil.compose.SubcomposeAsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.ShieldCheck
import com.composables.icons.lucide.UserRound
import com.example.clinexusapp.ui.components.ElegantTopAppBar
import com.example.clinexusapp.ui.theme.DeepTeal
import com.example.clinexusapp.ui.theme.RoyalNavy
import com.example.clinexusapp.ui.theme.SlateGray
import com.example.clinexusapp.util.SessionManager
import java.text.DateFormat
import java.util.Date

@Composable
fun SessionManagementScreen(onBack: () -> Unit, onSignedOut: () -> Unit) {
    val accounts by SessionManager.savedAccounts.collectAsState()
    val currentUser by SessionManager.currentUser.collectAsState()
    var confirmAll by remember { mutableStateOf(false) }

    if (confirmAll) {
        AlertDialog(
            onDismissRequest = { confirmAll = false },
            title = { Text("Sign out all accounts?") },
            text = { Text("This removes saved credentials and profile shortcuts from this phone.") },
            dismissButton = { TextButton(onClick = { confirmAll = false }) { Text("Cancel") } },
            confirmButton = {
                Button(
                    onClick = { confirmAll = false; SessionManager.signOutAllAccountsOnDevice(); onSignedOut() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Sign out all") }
            },
        )
    }

    Scaffold(
        topBar = { ElegantTopAppBar("Sessions & Saved Accounts", onBack) },
        containerColor = Color(0xFFE8EEF8),
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            contentPadding = PaddingValues(vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text("This device", color = RoyalNavy, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}", color = SlateGray)
            }
            items(accounts, key = { it.patient.patientID }) { account ->
                val patient = account.patient
                val isCurrent = patient.patientID == currentUser?.patientID
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Color(0xFFDDE7E6)),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = Color(0xFFE8EEF8), modifier = Modifier.size(46.dp)) {
                                                                val photo = account.cachedProfilePicture?.takeIf { it.isNotBlank() }
                                    ?: patient.profilePicture?.takeIf { it.isNotBlank() }
                                SubcomposeAsyncImage(
                                    model = photo,
                                    contentDescription = "${patient.firstName.orEmpty()} profile picture",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                    loading = { Box(contentAlignment = Alignment.Center) { Icon(Lucide.UserRound, null, tint = DeepTeal) } },
                                    error = { Box(contentAlignment = Alignment.Center) { Icon(Lucide.UserRound, null, tint = DeepTeal) } }
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(listOfNotNull(patient.firstName, patient.lastName).joinToString(" "), color = RoyalNavy, fontWeight = FontWeight.Bold)
                                Text(patient.email.orEmpty(), color = SlateGray, fontSize = 13.sp)
                            }
                            if (isCurrent) {
                                Icon(Lucide.ShieldCheck, "Current session", tint = DeepTeal)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            if (account.lastSignInAt > 0) "Last sign-in: ${DateFormat.getDateTimeInstance().format(Date(account.lastSignInAt))}" else "Last sign-in time unavailable",
                            color = SlateGray,
                            fontSize = 12.sp,
                        )
                        TextButton(
                            onClick = {
                                SessionManager.removeSavedAccount(patient.patientID)
                                if (isCurrent) { SessionManager.logout(); onSignedOut() }
                            },
                        ) { Text("Remove saved credentials", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = { confirmAll = true },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                ) { Text("Sign out all accounts on this device", color = MaterialTheme.colorScheme.error) }
                Spacer(Modifier.height(8.dp))
                Text("Server-wide sign-out is unavailable until the clinic API supports session revocation.", color = SlateGray, fontSize = 12.sp)
            }
        }
    }
}
