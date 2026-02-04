package com.example.ovi.presentation.navigation

sealed class Screen(val route: String) {
//    object Splash : Screen()
    object Auth : Screen("auth_screen")
    object Main : Screen("main_screen")
    object DeviceList : Screen("device_list_screen")
    object Device : Screen("device_screen")
}