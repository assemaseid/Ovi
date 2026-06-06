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
import androidx.navigation.NavController
import com.example.ovi.R
import com.example.ovi.presentation.navigation.Screen
import com.example.ovi.presentation.viewmodel.DevicesViewModel
import com.example.ovi.presentation.viewmodel.LockOperationState
import com.example.ovi.ui.theme.*


@Composable
fun DeviceScreen(
    deviceId: String,
    viewModel: DevicesViewModel,
    onBack: () -> Unit,
    navController: NavController
) {
    val device = viewModel.devices.collectAsState().value.find { it.id == deviceId }
    val operationState by viewModel.operationState.collectAsState()
    val isOwner = device?.ownerUuid == viewModel.currentUserId
    
    val isLocked = device?.isLocked ?: true
    val isLoading = operationState !is LockOperationState.Idle
    val statusMessage: String? = when (val s = operationState) {
        is LockOperationState.Loading -> "Processing..."
        is LockOperationState.Success -> s.message
        is LockOperationState.Error -> s.message
        else -> null
    }
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = White
                    )
                }
                Text(
                    text = device?.name ?: "Lock",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = White,
                    modifier = Modifier.weight(1f)
                )

            }

            Spacer(modifier = Modifier.height(28.dp))

            Box(
                modifier = Modifier
                    .size(150.dp)
                    .scale(if (!isLocked) pulseScale else 1f)
                    .clip(CircleShape)
                    .background(
                        if (isLocked) White.copy(alpha = 0.12f)
                        else Green.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.lock),
                    contentDescription = "Lock",
                    modifier = Modifier.size(120.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))


            Text(
                text = if (isLocked) "Locked" else "Unlocked",
                fontSize = 23.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isLocked) Green else UnlockBtn
            )

            statusMessage?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    fontSize = 12.sp,
                    color = White.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = White.copy(alpha = 0.15f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Battery",
                            fontSize = 16.sp,
                            color = TextHint
                        )
                        val batteryColor = when {
                            (device?.batteryLevel ?: 0) > 85 -> Green
                            (device?.batteryLevel ?: 0) > 50 -> Yellow
                            (device?.batteryLevel ?: 0) > 20 -> BatteryLow
                            else -> RedCritical

                        }
                        Text(
                            text = "${device?.batteryLevel ?: 0}%",
                            fontSize = 16.sp,
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
                        val batteryLevel = device?.batteryLevel ?: 0
                        val batteryColor = when {
                            batteryLevel > 85 -> Green
                            batteryLevel > 50 -> Yellow
                            batteryLevel > 20 -> BatteryLow
                            else -> RedCritical
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Last activity", fontSize = 16.sp, color = TextHint)
                        Text(
                            text = device?.lastSynced?.let { if (it == 0L) "—" else java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(it)) } ?: "—",
                            fontSize = 16.sp,
                            color = White
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.toggleLock(deviceId) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLocked)
                        Green.copy(alpha = 0.9f)
                    else
                        UnlockBtn.copy(alpha = 0.9f),
                    disabledContainerColor = White.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = White
                    )
                } else {
                    Icon(
                        imageVector = if (isLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                        contentDescription = null,
                        tint = White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isLocked) "Unlock" else "Lock",
                        color = White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ActionButton(
                    icon = Icons.Default.Schedule,
                    label = "Auto PIN",
                    onClick = { navController.navigate(
                        "auto_pin/${deviceId}/${device?.name ?: "Lock"}?isOwner=$isOwner"
                    ) { launchSingleTop = true } },
                    modifier = Modifier.weight(1f)
                )
                if (isOwner) {
                    ActionButton(
                        icon = Icons.Default.Fingerprint,
                        label = "Fingerprints",
                        onClick = { navController.navigate(
                            "fingerprints/${deviceId}/${device?.name ?: "Lock"}"
                        ) { launchSingleTop = true } },
                        modifier = Modifier.weight(1f)
                    )
                }
                ActionButton(
                    icon = Icons.Default.History,
                    label = "Event Log",
                    onClick = {
                        navController.navigate(
                            Screen.EventLog.createRoute(deviceId, device?.name ?: "Lock")
                        ) { launchSingleTop = true }
                    },
                    modifier = Modifier.weight(1f)
                )
                if (isOwner) {
                    ActionButton(
                        icon = Icons.Default.Group,
                        label = "Guests",
                        onClick = {
                            navController.navigate(
                                "guests/${deviceId}/${device?.name ?: "Lock"}"
                            ) { launchSingleTop = true }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Spacer(modifier = Modifier.height(16.dp))
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
        color = White.copy(alpha = 0.15f),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = White,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = label,
                fontSize = 13.sp,
                color = White.copy(alpha = 0.9f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}