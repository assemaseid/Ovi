package com.example.ovi.presentation.ui.screens.devices

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color

@Composable
fun DeviceListScreen(
    viewModel: DevicesViewModel,
    onDeviceClick: (String) -> Unit
) {
    val devices by viewModel.devices.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        items(devices) { lock ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable {onDeviceClick(lock.id) },
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.cardElevation(4.dp)
            ){
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column{
                        Text(lock.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = if (lock.isConnected) "Connected" else "Disconnected",
                            color = if (lock.isConnected) Color(0xFF4CAF50) else Color.Gray
                        )
                    }

                    Icon(
                        imageVector =
                            if (lock.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = null,
                        tint = if (lock.isLocked) Color.Red else Color.Green
                    )
                }
            }
        }
    }
}