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

@Composable
fun DevicesListScreen(
    viewModel: DevicesViewModel,
    onDeviceClick: (String) -> Unit
) {

    val devices by viewModel.devices.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)

    ) {
        items(devices) { dev ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { onDeviceClick(dev.id) },
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.cardElevation(6.dp)
            ){
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column{
                        Text(dev.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Status: ${dev.status}",
                            color = if (dev.status == "ON") Color.Green else Color.Red
                        )
                    }

                    Icon(
                        imageVector =
                            if (dev.locked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = null,
                        tint =
                            if (dev.locked) Color.Red else Color.Green
                    )
                }
            }
        }
    }
}