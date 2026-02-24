package com.example.ovi.presentation.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ovi.R
import com.example.ovi.presentation.viewmodel.DevicesViewModel
import com.example.ovi.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DeviceScreen(
    deviceId: String,
    viewModel: DevicesViewModel,
    onBack: () -> Unit
) {
    val device = viewModel.devices.collectAsState().value.find { it.id == deviceId }
    var isLocked by remember { mutableStateOf(device?.locked ?: true) }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Анимация пульса на иконке замка
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(BgTop, BgBottom)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // TopBar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = TextWhite
                    )
                }
                Text(
                    text = device?.name ?: "Lock",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    modifier = Modifier.weight(1f)
                )
                // Online индикатор
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF81C784))
                    )
                    Text(
                        text = "Online",
                        fontSize = 12.sp,
                        color = TextWhite.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Большая иконка замка с пульсом
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .scale(if (!isLocked) pulseScale else 1f)
                    .clip(CircleShape)
                    .background(
                        if (isLocked) Color.White.copy(alpha = 0.12f)
                        else Color(0xFF81C784).copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.lock),
                    contentDescription = "Lock",
                    modifier = Modifier.size(100.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Статус
            Text(
                text = if (isLocked) "Locked" else "Unlocked",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isLocked) Color(0xFFEF9A9A) else Color(0xFF81C784)
            )

            // Статус сообщение (результат unlock)
            statusMessage?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    fontSize = 12.sp,
                    color = TextWhite.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Карточка с данными
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.15f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Батарея
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Battery",
                            fontSize = 12.sp,
                            color = TextHint
                        )
                        val batteryColor = when {
                            (device?.battery_level ?: 0) > 50 -> Color(0xFF81C784)
                            (device?.battery_level ?: 0) > 20 -> Color(0xFFFFB74D)
                            else -> Color(0xFFEF5350)
                        }
                        Text(
                            text = "${device?.battery_level ?: 0}%",
                            fontSize = 12.sp,
                            color = batteryColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                    ) {
                        val batteryLevel = device?.battery_level ?: 0
                        val batteryColor = when {
                            batteryLevel > 50 -> Color(0xFF81C784)
                            batteryLevel > 20 -> Color(0xFFFFB74D)
                            else -> Color(0xFFEF5350)
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(batteryLevel / 100f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(batteryColor)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = Color.White.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Last seen
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Last opened", fontSize = 12.sp, color = TextHint)
                        Text(
                            text = device?.lastSeen ?: "—",
                            fontSize = 12.sp,
                            color = TextWhite
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Кнопка Unlock/Lock — главная
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        statusMessage = "Requesting token..."
                        delay(1000) // имитация запроса к серверу
                        statusMessage = "Sending via BLE..."
                        delay(800)
                        isLocked = !isLocked
                        statusMessage = if (!isLocked) "Unlocked successfully" else "Locked"
                        isLoading = false
                        delay(2000)
                        statusMessage = null
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLocked)
                        Color(0xFF81C784).copy(alpha = 0.9f)
                    else
                        Color(0xFFEF9A9A).copy(alpha = 0.9f),
                    disabledContainerColor = Color.White.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Icon(
                        imageVector = if (isLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isLocked) "Unlock" else "Lock",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Три кнопки действий
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ActionButton(
                    icon = Icons.Default.Pin,
                    label = "Change PIN",
                    onClick = { /* TODO */ },
                    modifier = Modifier.weight(1f)
                )
                ActionButton(
                    icon = Icons.Default.Schedule,
                    label = "Auto PIN",
                    onClick = { /* TODO */ },
                    modifier = Modifier.weight(1f)
                )
                ActionButton(
                    icon = Icons.Default.History,
                    label = "Event Log",
                    onClick = { /* TODO */ },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.15f),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = TextWhite,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = TextWhite.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}