package com.example.clinexusapp.ui.screens.auth

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.clinexusapp.ui.theme.ErrorRed
import com.example.clinexusapp.ui.theme.SlateGray

@Composable
fun PasswordRequirements(password: String, confirmation: String) {
    val rules = listOf(
        "Minimum of 8 characters" to (password.length >= 8),
        "At least 1 uppercase letter" to password.any { it.isUpperCase() },
        "No spaces" to (password.isNotEmpty() && password.none { it.isWhitespace() }),
        "Passwords must match" to (password.isNotEmpty() && password == confirmation)
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        rules.forEach { (label, satisfied) ->
            Text("• $label", color = if (satisfied) MaterialTheme.colorScheme.onSurfaceVariant else ErrorRed, fontSize = 13.sp)
        }
    }
}
