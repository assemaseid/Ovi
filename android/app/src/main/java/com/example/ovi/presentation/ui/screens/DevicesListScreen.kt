package com.example.ovi.presentation.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.example.ovi.presentation.viewmodel.DevicesViewModel
import com.example.ovi.ui.theme.Green40

@Composable
fun DevicesListScreen(
    viewModel: DevicesViewModel,
    onDeviceClick: (String) -> Unit
) {

    val devices by viewModel.devices.collectAsState()

    if (devices.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No locks paired yet.", color = Color.Gray)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        items(devices) { lock ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { onDeviceClick(lock.id) },
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.cardElevation(6.dp)
            ){
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column{
                        Text(lock.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Firmware: ${lock.firmwareVersion ?: "Unknown"}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Battery: ${lock.batteryLevel}%",
                            color = if (lock.batteryLevel < 20) Color.Red else Color.Unspecified
                        )
                    }

                    Icon(
                        imageVector =
                            if (lock.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = null,
                        tint =
                            if (lock.isLocked) Color.Red else Green40
                    )
                }
            }
        }
    }
}