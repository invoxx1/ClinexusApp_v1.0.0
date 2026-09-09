package com.example.clinexusapp.ui.screens.doctors

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.clinexusapp.ui.components.ElegantCard
import com.example.clinexusapp.ui.components.ElegantTextField
import com.example.clinexusapp.ui.components.premiumClickable
import com.example.clinexusapp.ui.theme.*

@Composable
fun DoctorListScreen(onDoctorClick: (String) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    
    val allDoctors = listOf(
        Doctor("Dr. Olivia Bennett", "Dentist", 4.9),
        Doctor("Dr. Liam Carter", "Dentist", 4.8)
    )

    val filteredDoctors = if (searchQuery.isEmpty()) {
        allDoctors
    } else {
        allDoctors.filter { 
            it.name.contains(searchQuery, ignoreCase = true) || 
            it.specialty.contains(searchQuery, ignoreCase = true) 
        }
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { -20 }
        ) {
            Column {
                Text(
                    text = "Specialists",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = (-1).sp
                )
                Text(
                    text = "2 certified professionals online",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(28.dp))

        ElegantTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = "Find Specialist",
            icon = Icons.Default.Search
        )

        Spacer(modifier = Modifier.height(32.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            items(filteredDoctors) { doctor ->
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { 50 }
                ) {
                    ElegantDoctorListItem(doctor, onClick = { onDoctorClick(doctor.name) })
                }
            }
        }
    }
}

data class Doctor(val name: String, val specialty: String, val rating: Double)

@Composable
fun ElegantDoctorListItem(doctor: Doctor, onClick: () -> Unit) {
    ElegantCard(
        modifier = Modifier
            .fillMaxWidth()
            .premiumClickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = doctor.name.take(1),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = doctor.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = doctor.specialty,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = doctor.rating.toString(), 
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(text = "Book", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}
