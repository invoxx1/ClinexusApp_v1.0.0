package com.example.clinexusapp.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.ClipboardList
import com.composables.icons.lucide.CircleCheck
import com.composables.icons.lucide.Lucide
import com.example.clinexusapp.model.firstText
import com.example.clinexusapp.util.Resource
import com.example.clinexusapp.viewmodel.ClinicalHistoryViewModel

@Composable
fun ClinicalHistoryScreen(onBack: () -> Unit, viewModel: ClinicalHistoryViewModel) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        topBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) { Icon(Lucide.ArrowLeft, "Back") }
                Column {
                    Text("Clinical History", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("Your dental records from the clinic", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        when (val result = state) {
            Resource.Idle, Resource.Loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is Resource.Error -> Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(result.message ?: "Unable to load clinical history.", color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(12.dp))
                Button(onClick = viewModel::load) { Text("Retry") }
            }
            is Resource.Success -> {
                val history = result.data.clinicalHistory
                LazyColumn(
                    Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(20.dp, 8.dp, 20.dp, 40.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    item {
                        ClinicalSummaryCard(
                            hasMedicalHistory = history.medicalHistory?.isJsonObject == true,
                            consultations = history.consultations.size,
                            treatmentPlans = history.treatmentPlans.size,
                            prescriptions = history.prescriptions.size,
                        )
                    }
                    item { SectionTitle("Consultations") }
                    if (history.consultations.isEmpty()) {
                        item { EmptyClinicalSection("No consultations recorded yet.") }
                    } else {
                        items(history.consultations) { consultation ->
                            ClinicalRecordCard(
                                title = consultation.firstText("diagnosis", "chief_complaint") ?: "Dental consultation",
                                subtitle = consultation.firstText("treatment_recommendation", "clinical_findings", "consultation_notes") ?: "No additional notes",
                                date = consultation.firstText("created_at", "updated_at"),
                            )
                        }
                    }
                    item { SectionTitle("Treatment and prescriptions") }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                CountCard("Treatment plans", history.treatmentPlans.size, Modifier.weight(1f))
                                CountCard("Treatment sessions", history.treatmentSessions.size, Modifier.weight(1f))
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                CountCard("Service sessions", history.serviceSessions.size, Modifier.weight(1f))
                                CountCard("Prescriptions", history.prescriptions.size, Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClinicalSummaryCard(hasMedicalHistory: Boolean, consultations: Int, treatmentPlans: Int, prescriptions: Int) {
    Surface(shape = RoundedCornerShape(24.dp), shadowElevation = 3.dp) {
        Column(
            Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF17376F), Color(0xFF315A9B)))).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Color.White.copy(alpha = 0.14f), shape = RoundedCornerShape(14.dp)) {
                    Icon(Lucide.ClipboardList, null, tint = Color.White, modifier = Modifier.padding(10.dp).size(26.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Record overview", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Text("Your clinic record at a glance", color = Color.White.copy(alpha = 0.72f), fontSize = 12.sp)
                }
            }
            Surface(color = Color.White.copy(alpha = 0.12f), shape = RoundedCornerShape(14.dp)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (hasMedicalHistory) Lucide.CircleCheck else Lucide.ClipboardList, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(9.dp))
                    Column {
                        Text("Medical history", color = Color.White.copy(alpha = 0.70f), fontSize = 11.sp)
                        Text(if (hasMedicalHistory) "Record available" else "No record on file yet", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryStat("Consultations", consultations, Modifier.weight(1f))
                SummaryStat("Plans", treatmentPlans, Modifier.weight(1f))
                SummaryStat("Prescriptions", prescriptions, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SummaryStat(label: String, count: Int, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count.toString(), color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color.White.copy(alpha = 0.72f), fontSize = 10.sp, maxLines = 1)
    }
}

@Composable private fun SectionTitle(text: String) = Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
    Box(Modifier.width(4.dp).height(20.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50)))
    Spacer(Modifier.width(9.dp))
    Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun ClinicalRecordCard(title: String, subtitle: String, date: String?) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp), tonalElevation = 1.dp) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            date?.let { Text(it.substringBefore('T'), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary) }
        }
    }
}

@Composable private fun CountCard(label: String, count: Int, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.height(92.dp), color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(17.dp), tonalElevation = 1.dp) {
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(count.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, lineHeight = 15.sp)
        }
    }
}

@Composable private fun EmptyClinicalSection(message: String) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(18.dp), tonalElevation = 1.dp) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), shape = RoundedCornerShape(50)) {
                Icon(Lucide.ClipboardList, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(10.dp).size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("No consultation records", fontWeight = FontWeight.SemiBold)
                Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
    }
}
