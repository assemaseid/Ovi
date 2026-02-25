package com.example.ovi.presentation.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.ovi.R
import com.example.ovi.presentation.viewmodel.AuthViewModel
import com.example.ovi.ui.theme.BgBottom
import com.example.ovi.ui.theme.BgMiddle
import com.example.ovi.ui.theme.BgTop
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    onNavigateToAuth: () -> Unit,
    onNavigateToMain: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val alphaAnim = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(0.85f) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.coroutineScope {
            launch {
                alphaAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(900, easing = EaseOut)
                )
            }
            launch {
                scaleAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(900, easing = EaseOutBack)
                )
            }
        }
        kotlinx.coroutines.delay(1200)
        if (viewModel.checkLoginStatus()) onNavigateToMain()
        else onNavigateToAuth()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        BgTop,
                        BgMiddle,
                        BgBottom
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(400.dp)
                .offset(x = 120.dp, y = (-180).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .alpha(alphaAnim.value)
                .scale(scaleAnim.value)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ovi),
                contentDescription = "OVI Logo",
                modifier = Modifier.size(250.dp)
            )

            Box(
                modifier = Modifier
                    .width(150.dp)
                    .height(1.5.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.7f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Text(
                text = "SMART LOCK",
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.75f),
                letterSpacing = 5.sp
            )
        }

        Text(
            text = "v1.0",
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.35f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .alpha(alphaAnim.value)
        )
    }
}