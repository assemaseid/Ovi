package com.example.ovi.presentation.navigation


import androidx.compose.material3.Text
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.ovi.OviApplication
import com.example.ovi.presentation.ui.screens.AuthScreen
import com.example.ovi.presentation.ui.screens.devices.DeviceListScreen
import com.example.ovi.presentation.ui.screens.devices.DevicesViewModel
import com.example.ovi.presentation.ui.screens.device.DeviceScreen
import com.example.ovi.presentation.viewmodel.AuthViewModel
import com.example.ovi.presentation.viewmodel.ViewModelFactory
//import com.example.ovi.presentation.ui.screens.SplashScreen


@Composable
fun SmartLockNavGraph(){
    val navController = rememberNavController()
    val context = LocalContext.current
    val app = context.applicationContext as OviApplication

    NavHost(
        navController = navController,
        startDestination = Screen.Auth.route
    ) {
        composable(Screen.Auth.route) {
            val authViewModel: AuthViewModel = viewModel(
                factory = ViewModelFactory(authRepository = app.authRepository)
            )
            AuthScreen(
                viewModel = authViewModel,
                onNavigateToMain = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Main.route) {
            val devicesViewModel: DevicesViewModel = viewModel(
                factory = ViewModelFactory(lockRepository = app.lockRepository)
            )
            DeviceListScreen(
                viewModel = devicesViewModel,
                onDeviceClick = { deviceId ->
                    navController.navigate("device_details/$deviceId")
                }
            )
        }

        composable("device_details/{deviceId}") {backStackEntry ->
            val deviceId = backStackEntry.arguments?.getString("deviceId") ?: ""
            val deviceViewModel: DevicesViewModel = viewModel(
                factory = ViewModelFactory(lockRepository = app.lockRepository)
            )
            DeviceScreen(deviceId = deviceId, viewModel = deviceViewModel)
        }
    }
}


