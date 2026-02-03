package com.example.ovi.ui.device

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
import com.example.ovi.ui.devices.DevicesViewModel

@Composable
fun DeviceScreen(
    deviceId: String,
    viewModel: DevicesViewModel
) {
    val devices by viewModel.devices.collectAsState()
    val device = devices.find { it.id == deviceId } ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {

        Icon(
            imageVector =
                if(device.locked) Icons.Default.Lock else Icons.Default.LockOpen,
            contentDescription = null,
            modifier = Modifier
                .size(140.dp)
                .align(Alignment.CenterHorizontally),
            tint =
                if (device.locked) Color.Red else Color.Green
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Smart Lock",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Device ID: ${device.id}",
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = {viewModel.toggleLock((device.id))},
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (device.locked) "Unlock" else "Lock")
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Status: ${device.status}",
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}