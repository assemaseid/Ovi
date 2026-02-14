package com.example.ovi.presentation.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.ovi.R
import com.example.ovi.presentation.viewmodel.AuthViewModel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AuthScreen(
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = hiltViewModel(),
    onNavigateToMain: () -> Unit
) {
    var isLoginMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    val authState by viewModel.authState.collectAsState()

    // Animated gradient background
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset"
    )

    LaunchedEffect(authState) {
        when (authState) {
            is AuthViewModel.AuthState.Success -> {
                kotlinx.coroutines.delay(500)
                onNavigateToMain()
                viewModel.resetState()
            }
            else -> {}
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF50AEEE),
                        Color(0xFF03A9F4),
                        Color(0xFF0277AD)
                    ),
                    start = Offset(
                        x = 500f * cos(animatedOffset * PI / 180).toFloat(),
                        y = 500f * sin(animatedOffset * PI / 180).toFloat()
                    ),
                    end = Offset(
                        x = 500f * cos((animatedOffset + 180) * PI / 180).toFloat(),
                        y = 500f * sin((animatedOffset + 180) * PI / 180).toFloat()
                    )
                )
            )
    ) {
        // Floating particles
        FloatingParticles()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo from resources
            Image(
                painter = painterResource(id = R.drawable.ovi),
                contentDescription = "OVI Logo",
                modifier = Modifier
                    .size(220.dp)
                    .padding(bottom = 16.dp)
            )

            Text(
                text = "SECURE ACCESS",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.8f),
                letterSpacing = 4.sp,
                modifier = Modifier.padding(bottom = 48.dp)
            )

            // Glassmorphism card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color.White.copy(alpha = 0.1f),
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isLoginMode) "Sign in to continue" else "Create Account",
                        fontSize = 15.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    // Name field (registration only)
                    if (!isLoginMode) {
                        GlassTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = "Full Name",
                            leadingIcon = Icons.Default.Person,
                            isError = authState is AuthViewModel.AuthState.Error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Email field
                    GlassTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email Address",
                        leadingIcon = Icons.Default.Email,
                        keyboardType = KeyboardType.Email,
                        isError = authState is AuthViewModel.AuthState.Error
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password field
                    GlassTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Password",
                        leadingIcon = Icons.Default.Lock,
                        keyboardType = KeyboardType.Password,
                        isPassword = true,
                        isPasswordVisible = isPasswordVisible,
                        onPasswordVisibilityToggle = { isPasswordVisible = !isPasswordVisible },
                        isError = authState is AuthViewModel.AuthState.Error
                    )

                    // Error message
                    if (authState is AuthViewModel.AuthState.Error) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = (authState as AuthViewModel.AuthState.Error).message,
                            color = Color(0xFFFF6B6B),
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Sign In button (gray)
                    Button(
                        onClick = {
                            if (isLoginMode) {
                                viewModel.login(email, password)
                            } else {
                                viewModel.register(email, password, name)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        enabled = email.isNotBlank() && password.isNotBlank() &&
                                (isLoginMode || name.isNotBlank()) &&
                                authState !is AuthViewModel.AuthState.Loading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.2f),
                            disabledContainerColor = Color.Gray.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (authState is AuthViewModel.AuthState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF8BCDFF)
                            )
                        } else {
                            Text(
                                text = if (isLoginMode) "Sign In" else "Create Account",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Sign Up button (cyan)
                    Button(
                        onClick = {
                            isLoginMode = !isLoginMode
                            viewModel.resetState()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0592E3)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = if (isLoginMode) "Sign up" else "Back to Sign In",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F0F1E)
                        )
                    }
                }
            }

            // Skip button
            TextButton(
                onClick = { onNavigateToMain() },
                modifier = Modifier.padding(top = 20.dp)
            ) {
                Text(
                    text = "Skip for now",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 13.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = false,
    onPasswordVisibilityToggle: () -> Unit = {},
    isError: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 13.sp
            )
        },
        leadingIcon = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = label,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = onPasswordVisibilityToggle) {
                    Icon(
                        imageVector = if (isPasswordVisible)
                            Icons.Default.Visibility
                        else
                            Icons.Default.VisibilityOff,
                        contentDescription = "Toggle password",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !isPasswordVisible)
            PasswordVisualTransformation()
        else
            VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        colors = TextFieldDefaults.outlinedTextFieldColors(
//            unfocusedContainerColor = Color.Transparent,
//            focusedContainerColor = Color.Transparent,
//            errorContainerColor = Color.Transparent,

            // Цвета рамки

            unfocusedBorderColor = Color.White,
            focusedBorderColor = Color.White,
            errorBorderColor = Color(0xFFFF6B6B),

            // Цвета иконок и текста подсказок
            unfocusedLabelColor = Color.White,
            focusedLabelColor = Color.White,
            unfocusedLeadingIconColor = Color.White,
            focusedLeadingIconColor = Color.White,
            unfocusedTrailingIconColor = Color.White,
            focusedTrailingIconColor = Color.White,

            // Цвет курсора
        ),
        isError = isError,
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
fun FloatingParticles() {
    val particles = remember {
        List(25) {
            ParticleState(
                x = (0..100).random().toFloat(),
                y = (0..100).random().toFloat(),
                size = (1..4).random().dp,
                speed = (8000..20000).random()
            )
        }
    }

    particles.forEach { particle ->
        val infiniteTransition = rememberInfiniteTransition(label = "particle_${particle.x}")

        val animatedY by infiniteTransition.animateFloat(
            initialValue = particle.y,
            targetValue = particle.y + 100f,
            animationSpec = infiniteRepeatable(
                animation = tween(particle.speed, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "y"
        )

        val animatedAlpha by infiniteTransition.animateFloat(
            initialValue = 0.1f,
            targetValue = 0.6f,
            animationSpec = infiniteRepeatable(
                animation = tween(particle.speed / 2, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.TopStart)
                .offset(
                    x = (particle.x * 10).dp,
                    y = ((animatedY % 100) * 10).dp
                )
                .size(particle.size)
                .alpha(animatedAlpha)
                .background(
                    color = Color(0xFF00FFF5),
                    shape = RoundedCornerShape(50)
                )
        )
    }
}

data class ParticleState(
    val x: Float,
    val y: Float,
    val size: androidx.compose.ui.unit.Dp,
    val speed: Int
)