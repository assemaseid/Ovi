package com.example.ovi.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem (
    val route: String,
    val title: String,
    val icon: ImageVector
){
    object Devices : BottomNavItem(
        route = "devices",
        title = "Locks",
        icon = Icons.Default.List
    )
    object Settings : BottomNavItem(
        route = "settings",
        title = "Settings",
        icon = Icons.Default.Settings
    )
    object Bluetooth : BottomNavItem(
        route = "bluetooth",
        title = "Pair lock",
        icon = Icons.Default.Bluetooth
    )
}