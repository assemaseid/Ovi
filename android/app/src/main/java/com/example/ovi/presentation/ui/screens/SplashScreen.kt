package com.example.ovi.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.ovi.presentation.viewmodel.AuthViewModel

@Composable
fun SplashScreen (
    onNavigateToAuth: () -> Unit,
    onNavigateToMain: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
){
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1000)
        val idLoggedIn = viewModel.checkLoginStatus()
        if (idLoggedIn) {
            onNavigateToMain()
        } else {
            onNavigateToAuth()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ){
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Smart Lock",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )

            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onPrimary
            )

        }
    }
}