package com.example.ovi.presentation.ui.screens.device

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ovi.presentation.ui.screens.devices.DevicesViewModel

@Composable
fun DeviceScreen(
    deviceId: String,
    viewModel: DevicesViewModel
) {
    val devices by viewModel.devices.collectAsState()
    val lock = devices.find { it.id == deviceId } ?: return

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if(lock.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = if (lock.isLocked) Color.Red else Color.Green
        )

        Spacer(Modifier.height(24.dp))

        Text(text = lock.name, style = MaterialTheme.typography.headlineMedium)
        Text(text = "ID: ${lock.id}", style = MaterialTheme.typography.bodySmall)

        Spacer(Modifier.height(16.dp))

        Text(text = "Battery: ${lock.batteryLevel}%", color = if(lock.batteryLevel < 20) Color.Red else Color.Unspecified)

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {viewModel.toggleLock((lock.id))},
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(
                contentColor = if (lock.isLocked) MaterialTheme.colorScheme.primary else Color.Gray
            )
        ) {
            Text(if (lock.isLocked) "Unlock Door" else "Lock Door")
        }

        Spacer(Modifier.height(32.dp))
    }
}