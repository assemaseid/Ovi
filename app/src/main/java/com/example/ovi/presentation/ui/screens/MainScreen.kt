package com.example.ovi.presentation.ui.screens


import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.ovi.presentation.navigation.BottomNavItem
import com.example.ovi.presentation.viewmodel.DevicesViewModel

const val DEVICE_ID_KEY = "deviceId"
const val DEVICE_ROUTE = "device"
@Composable
fun MainNavigationGraph(
    navController: NavHostController,
    innerPadding: PaddingValues
) {
    val devicesViewModel: DevicesViewModel = hiltViewModel()
    NavHost(
        navController = navController,
        startDestination = BottomNavItem.Devices.route,
        modifier = Modifier.padding(innerPadding)
    ) {
        composable(BottomNavItem.Devices.route) {
            DevicesListScreen(viewModel = devicesViewModel,
                onDeviceClick = { deviceId ->

                    navController.navigate("$DEVICE_ROUTE/$deviceId")                })
        }
        composable(
            route = "$DEVICE_ROUTE/{$DEVICE_ID_KEY}", // Маршрут с аргументом
            arguments = listOf(navArgument(DEVICE_ID_KEY) { type = NavType.StringType })
        ) { backStackEntry ->
            // Извлекаем deviceId из аргументов навигации
            val deviceId = backStackEntry.arguments?.getString(DEVICE_ID_KEY)
            if (deviceId != null) {
                DeviceScreen(
                    deviceId = deviceId,
                    viewModel = devicesViewModel // Передаем ту же самую ViewModel
                )
            }
        }
        composable(BottomNavItem.Settings.route) {
            SettingsScreen()
        }
        composable(BottomNavItem.Bluetooth.route) {
            BluetoothScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val mainNavController = rememberNavController() // Локальный NavController для MainScreen

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = mainNavController)
        }
    ) { innerPadding ->
        // Передаем NavController и отступы в наш вложенный граф навигации
        MainNavigationGraph(navController = mainNavController, innerPadding = innerPadding)
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        BottomNavItem.Devices,
        BottomNavItem.Bluetooth,
        BottomNavItem.Settings
    )

    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            NavigationBarItem(
                selected = (currentRoute == item.route) || (currentRoute?.startsWith(DEVICE_ROUTE) == true && item.route == BottomNavItem.Devices.route),
                label = { Text(text = item.title) },
                icon = { Icon(imageVector = item.icon, contentDescription = item.title) },
                onClick = {
                    navController.navigate(item.route) {
                        // Чтобы не создавать большой стек экранов при переключении
                        // Используем popUpTo для возврата к стартовому экрану графа
                        navController.graph.startDestinationRoute?.let { startRoute ->
                            popUpTo(startRoute) {
                                saveState = true
                            }
                        }
                        // Гарантируем, что у нас будет только одна копия каждого экрана
                        launchSingleTop = true
                        // Восстанавливаем состояние при повторном выборе
                        restoreState = true
                    }
                }
            )
        }
    }
}
