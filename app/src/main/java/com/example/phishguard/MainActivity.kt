package com.example.phishguard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavHostController
import com.example.phishguard.presentation.navigation.Screen
import com.example.phishguard.core.constants.SystemConstants
import com.example.phishguard.core.security.BiometricAuthManager
import com.example.phishguard.presentation.auth.BiometricScreen
import com.example.phishguard.presentation.navigation.PhishGuardNavHost
import com.example.phishguard.presentation.permission.NotificationPermissionScreen
import com.example.phishguard.ui.theme.PhishGuardTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    companion object {
        const val EXTRA_THREAT_ID = "extra_threat_id"
    }

    @Inject
    lateinit var biometricAuthManager: BiometricAuthManager

    private var navController: NavHostController? = null
    private var pendingThreatId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) { //_ 알림 클릭으로 앱 최초 실행되어 onCreate 실행한 경우에만 저장하여 처리하기 위함.
            pendingThreatId = intent.getLongExtra(EXTRA_THREAT_ID, -1L)
        }

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
                var isAuthenticated by rememberSaveable { mutableStateOf(false) }
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

                    else -> PhishGuardNavHost(
                        onNavControllerReady = { nc ->
                            navController = nc
                            val pending = pendingThreatId
                            if (pending != -1L) {
                                pendingThreatId = -1L
                                nc.navigate(Screen.Detail.createRoute(pending))
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val threatId = intent.getLongExtra(EXTRA_THREAT_ID, -1L)
        if (threatId != -1L) {
            navController?.navigate(Screen.Detail.createRoute(threatId))
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