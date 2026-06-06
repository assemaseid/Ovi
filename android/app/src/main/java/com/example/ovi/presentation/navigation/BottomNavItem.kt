package com.example.ovi.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem (
    val route: String,
    val title: String,
    val icon: ImageVector
){
    object Devices : BottomNavItem(
        route = "devices",
        title = "Locks",
        icon = Icons.Default.Lock
    )
    object Bluetooth : BottomNavItem(
        route = "bluetooth",
        title = "Pair lock",
        icon = Icons.Default.Bluetooth
    )
    object Messages : BottomNavItem(
        route = "messages",
        title = "Messages",
        icon = Icons.Default.Notifications
    )
    object Personal : BottomNavItem(
        route = "personal",
        title = "Profile",
        icon = Icons.Default.Person
    )
}