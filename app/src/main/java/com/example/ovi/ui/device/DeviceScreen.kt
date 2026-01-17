package com.example.ovi.ui.device

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DeviceScreen(deviceId: String) {
    var status by remember { mutableStateOf("Waiting...") }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Device: $deviceId")

        Spacer(Modifier.height(16.dp))

        Button(onClick = {
            status = "Test: got unlock token (mock)"
        }) {
            Text("Request Unlock Token")
        }

        Spacer(Modifier.height(16.dp))
        Text(status)
    }
}