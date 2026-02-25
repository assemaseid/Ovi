package com.example.ovi.presentation.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash" )
    object Auth : Screen("auth_screen")
    object Main : Screen("main_screen")
    object Personal : Screen("personal_screen")

    object EventLog : Screen("event_log/{lockId}/{lockName}") {
        fun createRoute(lockId: String, lockName: String) = "event_log/$lockId/$lockName"
    }


}