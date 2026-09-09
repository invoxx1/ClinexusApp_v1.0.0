package com.example.clinexusapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.clinexusapp.ui.theme.*

@Composable
fun Modifier.premiumClickable(onClick: () -> Unit): Modifier {
    val haptic = LocalHapticFeedback.current
    return this.clickable {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onClick()
    }
}

@Composable
fun NeumorphicCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    Surface(
        modifier = modifier
            .shadow(
                elevation = if (isDark) 2.dp else 6.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.05f),
                spotColor = if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = if (isDark) androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)) else null
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            content()
        }
    }
}

@Composable
fun VibrantButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(value = false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .fillMaxWidth()
            .height(64.dp)
            .shadow(if (enabled) 10.dp else 0.dp, RoundedCornerShape(22.dp), spotColor = DeepTeal.copy(alpha = 0.3f))
            .background(
                brush = if (enabled) ActionButtonGradient else Brush.linearGradient(listOf(Color.LightGray, Color.Gray)),
                shape = RoundedCornerShape(22.dp)
            )
            .pointerInput(enabled) {
                if (enabled) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            tryAwaitRelease()
                            isPressed = false
                        },
                        onTap = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onClick()
                        },
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = if (enabled) White else Color.DarkGray,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun MintTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    isPassword: Boolean = false
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = onSurfaceColor.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            leadingIcon = { Icon(icon, contentDescription = null, tint = primaryColor, modifier = Modifier.size(20.dp)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = primaryColor.copy(alpha = 0.05f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = primaryColor.copy(alpha = 0.3f)
            ),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 15.sp)
        )
    }
}

@Composable
fun WavyTealHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null,
    onNotificationClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(175.dp) // Subtle curve height
            .drawBehind {
                // Layer 1: Base Gradient
                val path1 = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width, size.height * 0.85f)
                    quadraticTo(size.width * 0.5f, size.height * 0.95f, 0f, size.height * 0.85f)
                    close()
                }
                drawPath(path1, brush = WavyTealGradient)

                // Layer 2: Middle Luminous Wave
                val path2 = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width, size.height * 0.7f)
                    quadraticTo(size.width * 0.5f, size.height * 0.82f, 0f, size.height * 0.65f)
                    close()
                }
                drawPath(path2, color = Color(0xFFE0F7F4).copy(alpha = 0.15f))

                // Layer 3: Top Soft Wave
                val path3 = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width, size.height * 0.6f)
                    quadraticTo(size.width * 0.5f, size.height * 0.72f, 0f, size.height * 0.55f)
                    close()
                }
                drawPath(path3, color = Color(0xFF00D2FF).copy(alpha = 0.12f))
            }
            .padding(horizontal = 24.dp)
    ) {
        // Top Icon Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = White, modifier = Modifier.size(24.dp))
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                onNotificationClick?.let {
                    IconButton(onClick = it) {
                        Icon(Icons.Default.Notifications, null, tint = White, modifier = Modifier.size(22.dp))
                    }
                }

            }
        }

        // Centered Content
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 15.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = White,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = White.copy(alpha = 0.85f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// Compatibility mappings for old names
@Composable
fun ElegantCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) = NeumorphicCard(modifier, content)

@Composable
fun ElegantButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @Suppress("UNUSED_PARAMETER") containerColor: Color = DeepTeal,
    @Suppress("UNUSED_PARAMETER") contentColor: Color = White,
) = VibrantButton(text, onClick, modifier, enabled)

@Composable
fun ElegantHeader(
    title: String,
    subtitle: String? = null,
    @Suppress("UNUSED_PARAMETER") onProfileClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
) = WavyTealHeader(title, subtitle, onNotificationClick = onNotificationClick)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElegantTopAppBar(title: String, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) {
    CenterAlignedTopAppBar(
        title = { 
            Text(
                text = title, 
                fontWeight = FontWeight.Bold, 
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onBackground 
            ) 
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
    )
}

@Composable
fun PremiumGlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) = NeumorphicCard(modifier, content)

@Composable
fun PremiumButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @Suppress("UNUSED_PARAMETER") containerColor: Color = DeepTeal,
) = VibrantButton(text, onClick, modifier, enabled)

@Composable
fun PremiumHeader(
    title: String,
    subtitle: String? = null,
    @Suppress("UNUSED_PARAMETER") onProfileClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
) = WavyTealHeader(title, subtitle, onNotificationClick = onNotificationClick)

@Composable
fun PremiumTopAppBar(title: String, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) = ElegantTopAppBar(title, onBack, actions)

@Composable
fun ElegantDoctorSummary(name: String) {
    NeumorphicCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = name.take(2).uppercase(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            }
            Spacer(modifier = Modifier.width(18.dp))
            Column {
                Text(text = name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(text = "Certified Dental Specialist", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp, start = 4.dp)
    )
}

@Composable
fun ModernTextField(value: String, onValueChange: (String) -> Unit, label: String, icon: ImageVector, isPassword: Boolean = false) = MintTextField(value, onValueChange, label, icon, isPassword)

@Composable
fun ElegantTextField(value: String, onValueChange: (String) -> Unit, label: String, icon: ImageVector, isPassword: Boolean = false) = MintTextField(value, onValueChange, label, icon, isPassword)
