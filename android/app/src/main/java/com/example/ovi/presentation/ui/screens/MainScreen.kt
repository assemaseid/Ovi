package com.example.ovi.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.ovi.presentation.navigation.BottomNavItem
import com.example.ovi.presentation.navigation.BottomNavigationBar
import com.example.ovi.presentation.navigation.DEVICE_ID_KEY
import com.example.ovi.presentation.navigation.DEVICE_ROUTE
import com.example.ovi.presentation.viewmodel.DevicesViewModel

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
                    navController.navigate(BottomNavItem.Bluetooth.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
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
                    navController = navController
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
            BluetoothScreen(onBack = { navController.popBackStack() })
        }

        composable(BottomNavItem.Personal.route) {
            PersonalScreen(onLogout = onLogout)
        }
    }
}
