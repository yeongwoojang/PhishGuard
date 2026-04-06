package com.example.phishguard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.phishguard.core.constants.SystemConstants
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            //_ API 33 이후는 직접 퍼미션 요청
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }

        setContent {
            PhishGuardTheme {
                var isAuthenticated by remember { mutableStateOf(false) }
                var hasNotificationPermission by remember {
                    mutableStateOf(isNotificationListenerEnabled())
                }

                //_ 설정에서 돌아오면 권한 재확인
                DisposableEffect(Unit) {
                    val observer = object : DefaultLifecycleObserver { //_ Activity 생명주기를 관찰
                        override fun onResume(owner: LifecycleOwner) { //_ 설정화면 -> 앱으로 돌아오면 실행
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
                                startActivity(Intent(SystemConstants.NOTIFICATION_LISTENER_ACTION))
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

    /**
     * 앱이 알림 접근 권한을 가지고 있는지 체크
     */
    private fun isNotificationListenerEnabled(): Boolean {
        val flat = Settings.Secure.getString(
            contentResolver,
            SystemConstants.NOTIFICATION_LISTENER_SETTINGS
        )
        return flat?.contains(packageName) == true
    }

}