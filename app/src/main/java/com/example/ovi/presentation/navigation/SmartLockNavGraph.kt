package com.example.ovi.presentation.navigation


import androidx.compose.material3.Text
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.example.ovi.presentation.ui.screens.AuthScreen
import com.example.ovi.presentation.ui.screens.MainScreen

//import com.example.ovi.presentation.ui.screens.SplashScreen


@Composable
fun SmartLockNavGraph(){
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Auth.route
    ) {
        composable(Screen.Auth.route) {
            AuthScreen(
                onNavigateToMain = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Auth.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(Screen.Main.route) {
            MainScreen()
        }
    }
}


