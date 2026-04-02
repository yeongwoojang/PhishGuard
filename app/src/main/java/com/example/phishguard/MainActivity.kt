package com.example.phishguard

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.phishguard.core.security.BiometricAuthManager
import com.example.phishguard.presentation.auth.BiometricScreen
import com.example.phishguard.presentation.home.HomeScreen
import com.example.phishguard.presentation.permission.NotificationPermissionScreen
import com.example.phishguard.ui.theme.PhishGuardTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var biometricAuthManager: BiometricAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PhishGuardTheme {
                var isAuthenticated by remember { mutableStateOf(false) }
                var hasNotificationPermission by remember {
                    mutableStateOf(isNotificationListenerEnabled())
                }

                // 설정에서 돌아오면 권한 재확인
                DisposableEffect(Unit) {
                    val observer = object : DefaultLifecycleObserver {
                        override fun onResume(owner: LifecycleOwner) {
                            hasNotificationPermission = isNotificationListenerEnabled()
                        }
                    }
                    lifecycle.addObserver(observer)
                    onDispose { lifecycle.removeObserver(observer) }
                }

                when {
                    !hasNotificationPermission -> {
                        NotificationPermissionScreen(
                            onGoToSettings = {
                                startActivity(
                                    Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                                )
                            }
                        )
                    }
                    !isAuthenticated -> {
                        BiometricScreen(
                            biometricAuthManager = biometricAuthManager,
                            onAuthSuccess = { isAuthenticated = true }
                        )
                    }
                    else -> HomeScreen()
                }
            }
        }
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val flat = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        )
        return flat?.contains(packageName) == true
    }
}