package com.example.ovi.presentation.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ovi.presentation.viewmodel.DevicesViewModel
import com.example.ovi.ui.theme.Green40

@Composable
fun DeviceScreen(
    deviceId: String,
    viewModel: DevicesViewModel
) {
    val devices by viewModel.devices.collectAsState()
    val lock = devices.find { it.id == deviceId } ?: return

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = if (lock.isLocked) Color.Red.copy(alpha = 0.15f) else Green40.copy(alpha = 0.2f),
                border = BorderStroke(
                    width = 2.dp,
                    color = if (lock.isLocked) Color.Red.copy(alpha = 0.5f) else Green40.copy(alpha = 0.5f)
                ),
                modifier = Modifier.size(180.dp)
            ) {
                Box(contentAlignment = Alignment.Center){
                    Icon(
                        imageVector = if (lock.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = if (lock.isLocked) Color.Red else Green40
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            Text(
                text = lock.name,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )

            Surface(
                color = if (lock.isConnected) Green40 else Color.Gray,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    text = if (lock.isConnected) "Connected" else "Offline",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium
                )
            }

            Spacer(Modifier.height(40.dp))

            Text(
                text = "Battery: ${lock.batteryLevel}%",
                style = MaterialTheme.typography.bodyLarge,
                color = if (lock.batteryLevel < 20) Color.Red else Color.Unspecified
            )

            Spacer(Modifier.height(56.dp))

            Button(
                onClick = { viewModel.toggleLock(lock.id) },
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (lock.isLocked) Green40 else Color.Red
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (lock.isLocked) "UNLOCK" else "LOCK",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
        }
    }
}