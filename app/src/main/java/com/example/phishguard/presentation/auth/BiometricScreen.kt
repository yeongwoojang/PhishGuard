package com.example.phishguard.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.phishguard.R
import com.example.phishguard.core.security.BiometricAuthManager

private val PrimaryBlue = Color(0xFF185FA5)

object BiometricConstants {
    const val CANCEL_KOREAN = "취소"
    const val CANCEL_ENGLISH = "cancel"
}

@Composable
fun BiometricScreen(
    biometricAuthManager: BiometricAuthManager,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as FragmentActivity
    var errorMessage by remember { mutableStateOf("") }
    var isAuthenticating by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!biometricAuthManager.isBiometricAvailable(context)) {
            onAuthSuccess()
            return@LaunchedEffect
        }

        isAuthenticating = true
        biometricAuthManager.showBiometricPrompt(
            activity = activity,
            onSuccess = {
                isAuthenticating = false
                onAuthSuccess()
            },
            onFailed = {
                isAuthenticating = false
            },
            onError = { error ->
                isAuthenticating = false
                if (!error.contains(BiometricConstants.CANCEL_KOREAN)
                    && !error.contains(BiometricConstants.CANCEL_ENGLISH, ignoreCase = true)
                ) {
                    errorMessage = error
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text("🛡", fontSize = 48.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "PhishGuard",
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                color = PrimaryBlue
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.biometric_desc),
                fontSize = 14.sp,
                color = Color(0xFF888780),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {
                    errorMessage = ""
                    isAuthenticating = true
                    biometricAuthManager.showBiometricPrompt(
                        activity = activity,
                        onSuccess = {
                            isAuthenticating = false
                            onAuthSuccess()
                        },
                        onFailed = {
                            isAuthenticating = false
                        },
                        onError = { error ->
                            isAuthenticating = false
                            if (!error.contains(BiometricConstants.CANCEL_KOREAN)
                                && !error.contains(
                                    BiometricConstants.CANCEL_ENGLISH,
                                    ignoreCase = true
                                )
                            ) {
                                errorMessage = error
                            }
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                enabled = !isAuthenticating
            ) {
                Text(
                    text = if (isAuthenticating) stringResource(R.string.btn_authenticating)
                    else stringResource(R.string.btn_biometric),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage,
                    fontSize = 13.sp,
                    color = Color(0xFFE24B4A),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}