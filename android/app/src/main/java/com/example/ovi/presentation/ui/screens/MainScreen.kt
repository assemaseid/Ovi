package com.example.ovi.presentation.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.ovi.presentation.navigation.BottomNavItem
import com.example.ovi.presentation.navigation.BottomNavigationBar
import com.example.ovi.presentation.viewmodel.DevicesViewModel
import com.example.ovi.ui.theme.AccentBlue

@Composable
fun MainScreen(
    onLogout: () -> Unit = {}
) {
    val mainNavController = rememberNavController()
    Box(modifier = Modifier.fillMaxSize()) {
        MainNavigationGraph(
            navController = mainNavController,
            onLogout = onLogout
        )
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            BottomNavigationBar(navController = mainNavController)
        }
    }
}

@Composable
fun NavBarItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) AccentBlue else Color.Gray.copy(alpha = 0.5f),
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (selected) AccentBlue else Color.Gray.copy(alpha = 0.5f),
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

const val DEVICE_ID_KEY = "deviceId"
const val DEVICE_ROUTE = "device"

@Composable
fun MainNavigationGraph(
    navController: NavHostController,
    onLogout: () -> Unit
) {
    val devicesViewModel: DevicesViewModel = hiltViewModel()

    NavHost(
        navController = navController,
        startDestination = BottomNavItem.Devices.route,
    ) {
        composable(BottomNavItem.Devices.route) {
            DevicesListScreen(
                viewModel = devicesViewModel,
                onDeviceClick = { deviceId ->
                    navController.navigate("$DEVICE_ROUTE/$deviceId")
                },
                onAddDevice = {
                    navController.navigate(BottomNavItem.Bluetooth.route)
                }
            )
        }

        composable(
            route = "$DEVICE_ROUTE/{$DEVICE_ID_KEY}",
            arguments = listOf(navArgument(DEVICE_ID_KEY) { type = NavType.StringType })
        ) { backStackEntry ->
            val deviceId = backStackEntry.arguments?.getString(DEVICE_ID_KEY)
            if (deviceId != null) {
                DeviceScreen(
                    deviceId = deviceId,
                    viewModel = devicesViewModel,
                    onBack = { navController.popBackStack() },
                    navController = navController  // ← вот это было missing
                )
            }
        }

        composable(
            route = "event_log/{lockId}/{lockName}",
            arguments = listOf(
                navArgument("lockId") { type = NavType.StringType },
                navArgument("lockName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val lockId = backStackEntry.arguments?.getString("lockId") ?: ""
            val lockName = backStackEntry.arguments?.getString("lockName") ?: "Lock"
            EventLogScreen(
                lockId = lockId,
                lockName = lockName,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "auto_pin/{lockId}/{lockName}",
            arguments = listOf(
                navArgument("lockId") { type = NavType.StringType },
                navArgument("lockName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val lockId = backStackEntry.arguments?.getString("lockId") ?: ""
            val lockName = backStackEntry.arguments?.getString("lockName") ?: "Lock"
            AutoPinScreen(
                lockId = lockId,
                lockName = lockName,
                onBack = { navController.popBackStack() }
            )
        }

        composable(BottomNavItem.Bluetooth.route) {
            BluetoothScreen()
        }

        composable(BottomNavItem.Personal.route) {
            PersonalScreen(onLogout = onLogout)
        }
    }
}