package com.example.ovi.presentation.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.ovi.ui.theme.BgBottom
import com.example.ovi.ui.theme.BgMiddle
import com.example.ovi.ui.theme.BgTop
import com.example.ovi.ui.theme.BorderFocused
import com.example.ovi.ui.theme.BorderUnfocused
import com.example.ovi.ui.theme.CardBackground
import com.example.ovi.ui.theme.ErrorRed
import com.example.ovi.ui.theme.SignInBtn
import com.example.ovi.ui.theme.TextHint
import com.example.ovi.ui.theme.TextWhite

@Composable
fun AuthScreen(
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = hiltViewModel(),
    onNavigateToMain: () -> Unit
) {
    var isLoginMode by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthViewModel.AuthState.Success) {
            kotlinx.coroutines.delay(300)
            onNavigateToMain()
            viewModel.resetState()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(BgTop, BgMiddle, BgBottom)
                )
            )
    ) {
        Box(
            modifier = Modifier
                .size(350.dp)
                .offset(x = 120.dp, y = (-100).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ovi),
                contentDescription = "OVI Logo",
                modifier = Modifier.size(150.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "SECURE ACCESS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextHint,
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(36.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = CardBackground,
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = if (isLoginMode) "Sign in to continue" else "Create Account",
                        fontSize = 15.sp,
                        color = TextWhite.copy(alpha = 1f)
                    )

                    if (!isLoginMode) {
                        AuthTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = "Full Name",
                            icon = Icons.Default.Person,
                            keyboardType = KeyboardType.Text,
                            isError = authState is AuthViewModel.AuthState.Error
                        )
                    }

                    AuthTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email Address",
                        icon = Icons.Default.Email,
                        keyboardType = KeyboardType.Email,
                        isError = authState is AuthViewModel.AuthState.Error
                    )

                    AuthTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Password",
                        icon = Icons.Default.Lock,
                        keyboardType = KeyboardType.Password,
                        isPassword = true,
                        isPasswordVisible = isPasswordVisible,
                        onPasswordToggle = { isPasswordVisible = !isPasswordVisible },
                        isError = authState is AuthViewModel.AuthState.Error
                    )

                    if (authState is AuthViewModel.AuthState.Error) {
                        Text(
                            text = (authState as AuthViewModel.AuthState.Error).message,
                            color = Color(0xFFFF6B6B),
                            fontSize = 12.sp
                        )
                    }

                    Button(
                        onClick = {
                            if (isLoginMode) viewModel.login(email, password)
                            else viewModel.register(name, email, password)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = email.isNotBlank() && password.isNotBlank() &&
                                (isLoginMode || name.isNotBlank()) &&
                                authState !is AuthViewModel.AuthState.Loading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.3f),
                            disabledContainerColor = Color.White.copy(alpha = 0.08f)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (authState is AuthViewModel.AuthState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = TextWhite
                            )
                        } else {
                            Text(
                                text = if (isLoginMode) "Sign In" else "Create Account",
                                color = TextWhite,
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp
                            )
                        }
                    }

                    Button(
                        onClick = {
                            isLoginMode = !isLoginMode
                            name = ""
                            viewModel.resetState()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SignInBtn.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = if (isLoginMode) "Sign up" else "Back to Sign In",
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = false,
    onPasswordToggle: () -> Unit = {},
    isError: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = androidx.compose.ui.text.TextStyle(
            color = Color(0xFF1A3A52),
            fontSize = 15.sp
        ),
        label = { Text(label, color = TextHint, fontSize = 13.sp) },
        leadingIcon = {
            Icon(imageVector = icon, contentDescription = null,
                tint = TextHint, modifier = Modifier.size(20.dp))
        },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = onPasswordToggle) {
                    Icon(
                        imageVector = if (isPasswordVisible)
                            Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = TextHint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !isPasswordVisible)
            PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        isError = isError,
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            unfocusedBorderColor = BorderUnfocused,
            focusedBorderColor = BorderFocused,
            errorBorderColor = ErrorRed,
            cursorColor = TextWhite,
            unfocusedLabelColor = TextHint,
            focusedLabelColor = TextWhite,
            unfocusedLeadingIconColor = TextHint,
            focusedLeadingIconColor = TextWhite,
        )
    )
}