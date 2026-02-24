package com.example.ovi.presentation.navigation


import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.example.ovi.presentation.ui.screens.AuthScreen
import com.example.ovi.presentation.ui.screens.MainScreen
import com.example.ovi.presentation.ui.screens.PersonalScreen
import com.example.ovi.presentation.ui.screens.SplashScreen


@Composable
fun SmartLockNavGraph(){
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToAuth = {
                    navController.navigate(Screen.Auth.route){
                        popUpTo(Screen.Splash.route) {
                            inclusive = true
                        }
                    }
                },
                onNavigateToMain = {
                    navController.navigate(Screen.Main.route){
                        popUpTo(Screen.Splash.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }

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
            MainScreen(
                onLogout = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(Screen.Main.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(Screen.Personal.route) {
            PersonalScreen(
                onLogout = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(Screen.Main.route) {
                            inclusive = true
                        }
                    }
                }
            )

        }
    }
}


