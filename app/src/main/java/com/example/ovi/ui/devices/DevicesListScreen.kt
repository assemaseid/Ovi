package com.example.ovi.ui.devices

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DeviceListScreen(
    viewModel: DevicesViewModel,
    onDeviceClick: (String) -> Unit
) {
    val devices by viewModel.devices.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 16.dp)

    ) {
        items(devices) { dev ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable {onDeviceClick(dev.id) }
            ){
                Column(Modifier.padding(16.dp)) {
                    Text(text = dev.name)
                    Text(text = "Status: ${dev.status}")
                }
            }
        }
    }
}