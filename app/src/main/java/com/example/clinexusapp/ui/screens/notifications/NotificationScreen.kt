package com.example.clinexusapp.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.clinexusapp.ui.components.ElegantTopAppBar
import com.example.clinexusapp.ui.components.NeumorphicCard
import com.example.clinexusapp.ui.theme.*
import com.example.clinexusapp.model.NotificationDTO
import com.example.clinexusapp.util.Resource
import com.example.clinexusapp.viewmodel.NotificationViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onBack: () -> Unit,
    viewModel: NotificationViewModel
) {
    val notificationState by viewModel.notifications.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadNotifications()
    }

    Scaffold(
        topBar = {
            ElegantTopAppBar(
                title = "Notifications",
                onBack = onBack
            )
        },
        containerColor = SoftMist
    ) { padding ->
        when (val state = notificationState) {
            Resource.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = VibrantTeal)
            }
            is Resource.Error -> Box(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(state.message ?: "Failed to load notifications", color = Color.Red)
            }
            is Resource.Success -> {
                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    if (state.data.any { it.isRead == 0 }) {
                        TextButton(
                            onClick = viewModel::markAllAsRead,
                            modifier = Modifier.align(Alignment.End).padding(horizontal = 16.dp)
                        ) {
                            Text("Mark all as read")
                        }
                    }
                    if (state.data.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No notifications yet", color = SlateGray)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(vertical = 24.dp)
                        ) {
                            items(state.data, key = { it.notificationId }) { item ->
                                TealNotificationCard(
                                    item = item,
                                    onClick = { viewModel.markAsRead(item) }
                                )
                            }
                        }
                    }
                }
            }
            Resource.Idle -> Unit
        }
    }
}

@Composable
fun TealNotificationCard(item: NotificationDTO, onClick: () -> Unit) {
    NeumorphicCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (item.isRead == 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else GrayMedium),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = null,
                    tint = if (item.isRead == 0) MaterialTheme.colorScheme.primary else SlateGray,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = RoyalNavy
                    )
                    if (item.isRead == 0) {
                        Box(modifier = Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                    }
                }
                Text(
                    text = formatNotificationTime(item.createdAt),
                    fontSize = 12.sp,
                    color = SlateGray.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = cleanNotificationMessage(item.message),
                    fontSize = 14.sp,
                    color = SlateGray,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

private fun formatNotificationTime(value: String?): String {
    if (value.isNullOrBlank()) return ""

    val inputFormats = listOf(
        "EEE MMM dd yyyy HH:mm:ss 'GMT'Z (zzzz)",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
    )

    val date = inputFormats.firstNotNullOfOrNull { pattern ->
        runCatching {
            SimpleDateFormat(pattern, Locale.US).parse(value)
        }.getOrNull()
    } ?: return value.substringBefore(" GMT")

    return SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(date)
}

private fun cleanNotificationMessage(value: String): String {
    val withoutTimezone = value.replace(
        Regex("\\sGMT[+-]\\d{4}(?:\\s*\\(Coordinated Universal Time\\))?"),
        ""
    )

    return Regex("\\b([01]\\d|2[0-3]):([0-5]\\d):([0-5]\\d)\\b")
        .replace(withoutTimezone) { match ->
            runCatching {
                val parsedTime = SimpleDateFormat("HH:mm:ss", Locale.US).parse(match.value)
                SimpleDateFormat("h:mm a", Locale.getDefault()).format(parsedTime!!)
            }.getOrDefault(match.value)
        }
}
