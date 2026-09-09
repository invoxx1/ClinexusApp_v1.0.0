package com.example.clinexusapp.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.clinexusapp.ui.components.DashboardSkeleton
import com.example.clinexusapp.ui.components.NeumorphicCard
import com.example.clinexusapp.ui.components.VibrantButton
import com.example.clinexusapp.ui.components.WavyTealHeader
import com.example.clinexusapp.ui.navigation.Screen
import com.example.clinexusapp.ui.theme.DeepTeal
import com.example.clinexusapp.model.PromotionDTO
import com.example.clinexusapp.util.Resource
import com.example.clinexusapp.util.SessionManager
import com.example.clinexusapp.viewmodel.DashboardViewModel
import java.util.Locale

data class DashboardArticle(val title: String, val category: String)

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    rootNavController: NavController,
    onNotificationClick: () -> Unit,
) {
    var showInsightDialog by remember { mutableStateOf<DashboardArticle?>(null) }
    
    val user by SessionManager.currentUser.collectAsState()
    val firstName = user?.firstName ?: "Patient"

    val newsState by viewModel.newsState.collectAsState()
    val insightsState by viewModel.insightsState.collectAsState()
    val promotionsState by viewModel.promotionsState.collectAsState()
    val nextAppt by viewModel.nextAppointment.collectAsState()

    if (showInsightDialog != null) {
        val article = showInsightDialog!!
        AlertDialog(
            onDismissRequest = { showInsightDialog = null },
            confirmButton = {
                TextButton(onClick = { showInsightDialog = null }) { 
                    Text("DONE", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) 
                }
            },
            title = { 
                Text(
                    text = article.title.uppercase(), 
                    color = MaterialTheme.colorScheme.primary, 
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                ) 
            },
            text = { 
                Text(
                    text = "Professional clinical data regarding ${article.category.lowercase()} is being synchronized. Stay tuned for expert health tips!",
                    color = MaterialTheme.colorScheme.onSurface,
                ) 
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        val isLoading = (newsState is Resource.Loading) || (insightsState is Resource.Loading) || (promotionsState is Resource.Loading)
        
        if (isLoading) {
            DashboardSkeleton()
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = padding.calculateBottomPadding()),
            ) {
                item {
                    WavyTealHeader(
                        title = "Hello, $firstName!",
                        subtitle = "Welcome back!",
                        onNotificationClick = onNotificationClick,
                    )
                }

                item {
                    Column(modifier = Modifier.padding(horizontal = 24.dp).padding(top = 10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Upcoming Appointment",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            TextButton(onClick = { rootNavController.navigate(Screen.AppointmentHistory.route) }) {
                                Text("VIEW ALL", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        NeumorphicCard(modifier = Modifier.fillMaxWidth()) {
                            if (nextAppt != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
                                                .background(Color(0xFFE0F7F4), RoundedCornerShape(14.dp)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.EventNote, null, tint = DeepTeal, modifier = Modifier.size(26.dp))
                                        }
                                        Spacer(modifier = Modifier.width(18.dp))
                                        Column {
                                            Text(
                                                text = nextAppt!!.treatment, 
                                                fontWeight = FontWeight.Bold, 
                                                color = MaterialTheme.colorScheme.onSurface, 
                                                fontSize = 17.sp,
                                            )
                                            Text(
                                                text = "${com.example.clinexusapp.util.DateUtils.formatDisplayDate(nextAppt!!.appointmentDate)} • ${com.example.clinexusapp.util.DateUtils.formatDisplayTime(nextAppt!!.startTime)}", 
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), 
                                                fontSize = 13.sp,
                                            )

                                            val status = nextAppt!!.appointmentStatus.lowercase()
                                            val isConfirmed = status.contains("confirmed") || status.contains("scheduled")
                                            
                                            val badgeColor = when {
                                                isConfirmed -> Color(0xFF4CAF50)
                                                else -> MaterialTheme.colorScheme.primary
                                            }

                                            Surface(
                                                color = badgeColor.copy(alpha = 0.1f),
                                                shape = RoundedCornerShape(4.dp),
                                                modifier = Modifier.padding(top = 4.dp),
                                            ) {
                                                Text(
                                                    text = nextAppt!!.displayStatus.uppercase(), 
                                                    fontSize = 9.sp, 
                                                    fontWeight = FontWeight.Black,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    color = badgeColor,
                                                )
                                            }
                                        }
                                    }
                                    Box(
                                        modifier = Modifier.size(64.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator(
                                            progress = { 1f },
                                            modifier = Modifier.fillMaxSize(),
                                            color = DeepTeal,
                                            strokeWidth = 4.dp,
                                            trackColor = DeepTeal.copy(alpha = 0.1f),
                                        )
                                        Icon(Icons.Default.Schedule, null, tint = DeepTeal, modifier = Modifier.size(24.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                                Surface(
                                    onClick = { rootNavController.navigate(Screen.AppointmentHistory.route) },
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.align(Alignment.End),
                                ) {
                                    Text(
                                        text = "View Details", 
                                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                                        fontSize = 13.sp, 
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                    )
                                }
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(
                                        text = "No upcoming appointments",
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        fontSize = 15.sp,
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    VibrantButton(
                                        text = "Book Now",
                                        onClick = { rootNavController.navigate(Screen.AppointmentBooking.route) },
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    val promotions = (promotionsState as? Resource.Success)?.data ?: emptyList()
                    if (promotions.isNotEmpty()) {
                        Column(modifier = Modifier.padding(horizontal = 24.dp).padding(top = 32.dp)) {
                            Text(
                                text = "Promotions",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                promotions.forEach { promotion ->
                                    PromotionCard(promotion)
                                }
                            }
                        }
                    }
                }

                item {
                    Column(modifier = Modifier.padding(horizontal = 24.dp).padding(top = 32.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Health Insights", 
                                fontSize = 19.sp, 
                                fontWeight = FontWeight.Bold, 
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                repeat(3) { index ->
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 3.dp)
                                            .size(if (index == 0) 8.dp else 6.dp)
                                            .background(
                                                if (index == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), 
                                                CircleShape,
                                            ),
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        val insights = (insightsState as? Resource.Success)?.data ?: emptyList()
                        if ((insights.isEmpty()) && (insightsState is Resource.Success)) {
                             Text("No insights available", modifier = Modifier.padding(8.dp))
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                                insights.take(2).forEach { insight ->
                                    InsightItem(
                                        title = insight.title,
                                        subtitle = insight.description,
                                        icon = Icons.Default.Lightbulb,
                                        color = MaterialTheme.colorScheme.primary,
                                        bgColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    ) { showInsightDialog = DashboardArticle(insight.title, insight.category) }
                                }
                                
                                // Fallback if no data yet or to match your previous manual items
                                if (insights.isEmpty()) {
                                    InsightItem(
                                        title = "Tips for a Brighter Smile",
                                        subtitle = "Essential dental care tips you should know",
                                        icon = Icons.Default.AutoFixHigh,
                                        color = Color(0xFFFF8A65),
                                        bgColor = Color(0xFFFFEAE3),
                                    ) { showInsightDialog = DashboardArticle("Tips for a Brighter Smile", "Care") }
                                }
                            }
                        }
                    }
                }

                item {
                    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(), 
                            horizontalArrangement = Arrangement.SpaceBetween, 
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Clinic News", 
                                    fontSize = 19.sp, 
                                    fontWeight = FontWeight.Bold, 
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                                Box(modifier = Modifier.padding(start = 8.dp).size(6.dp).background(MaterialTheme.colorScheme.secondary, CircleShape))
                            }
                            IconButton(onClick = { /* simulated web link */ }) { 
                                Icon(Icons.AutoMirrored.Filled.Launch, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) 
                            }
                        }
                        
                        val newsList = (newsState as? Resource.Success)?.data ?: emptyList()
                        if ((newsList.isEmpty()) && (newsState is Resource.Success)) {
                             Text("No news currently", modifier = Modifier.padding(8.dp))
                        } else {
                            newsList.take(1).forEach { news ->
                                NeumorphicCard(modifier = Modifier.fillMaxWidth()) {
                                    Column {
                                        Text(
                                            text = news.title,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 16.sp,
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = news.description,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            fontSize = 14.sp,
                                            lineHeight = 20.sp,
                                        )
                                    }
                                }
                            }
                            
                            if (newsList.isEmpty()) {
                                NeumorphicCard(modifier = Modifier.fillMaxWidth()) {
                                    Column {
                                        Text(
                                            text = "Our Clinic is Expanding!",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 16.sp,
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Exciting new facilities coming soon to serve you better with the latest dental technology.",
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            fontSize = 14.sp,
                                            lineHeight = 20.sp,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PromotionCard(promotion: PromotionDTO) {
    NeumorphicCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(Color(0xFFFFE8D5), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.LocalOffer, null, tint = Color(0xFFE77A35), modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = promotion.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                promotion.discountValue?.let { value ->
                    Text(
                        text = formatDiscount(promotion.discountType, value),
                        color = Color(0xFFE77A35),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                promotion.description?.takeIf { it.isNotBlank() }?.let { description ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                    )
                }
            }
        }
    }
}

private fun formatDiscount(type: String?, value: Double): String {
    return if (type.equals("percentage", ignoreCase = true) || type.equals("percent", ignoreCase = true)) {
        "${value.toInt()}% OFF"
    } else {
        "PHP ${String.format(Locale.US, "%,.2f", value)} OFF"
    }
}

@Composable
fun InsightItem(title: String, subtitle: String, icon: ImageVector, color: Color, bgColor: Color, onClick: () -> Unit) {
    NeumorphicCard(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(bgColor, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(26.dp))
            }
            Spacer(modifier = Modifier.width(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title, 
                    fontWeight = FontWeight.Bold, 
                    color = MaterialTheme.colorScheme.onSurface, 
                    fontSize = 16.sp,
                )
                Text(
                    text = subtitle, 
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), 
                    fontSize = 13.sp, 
                    maxLines = 1,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight, 
                null, 
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), 
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
