package com.example.clinexusapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.CircleAlert
import com.composables.icons.lucide.CircleCheck
import com.composables.icons.lucide.Lucide

@Composable
fun ClinexusSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
    ) { data ->
        val message = data.visuals.message
        val isSuccess = listOf("success", "updated", "saved", "sent", "complete", "verified")
            .any { message.contains(it, ignoreCase = true) }
        val accent = if (isSuccess) Color(0xFF087F73) else Color(0xFFB4233D)
        val background = if (isSuccess) Color(0xFFE8F7F3) else Color(0xFFFFEEF1)

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = background,
            contentColor = accent,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, accent.copy(alpha = 0.18f)),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (isSuccess) Lucide.CircleCheck else Lucide.CircleAlert,
                    contentDescription = if (isSuccess) "Success" else "Notice",
                    modifier = Modifier.size(22.dp),
                    tint = accent,
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = message,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF173B3A),
                )
            }
        }
    }
}
