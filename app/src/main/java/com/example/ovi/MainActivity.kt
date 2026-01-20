package com.example.ovi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.example.ovi.ui.device.DeviceScreen
import com.example.ovi.ui.devices.DeviceListScreen
import com.example.ovi.ui.devices.DevicesViewModel
import com.example.ovi.ui.theme.OviTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            OviTheme {

                val  viewModel = remember { DevicesViewModel() }
                var selectedDeviceId by remember { mutableStateOf<String?>(null)}

                BackHandler(enabled = selectedDeviceId != null) {
                    selectedDeviceId = null
                }

                if (selectedDeviceId == null) {
                    DeviceListScreen(
                        viewModel = viewModel,
                        onDeviceClick = { id -> selectedDeviceId = id }
                    )
                } else {
                    DeviceScreen(
                        deviceId = selectedDeviceId!!,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

