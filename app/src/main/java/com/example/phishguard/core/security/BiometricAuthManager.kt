package com.example.phishguard.core.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import javax.inject.Inject
import javax.inject.Singleton

object BiometricAuthManagerConstants {
    const val biometricTitle = "PhishGuard 잠금 해제"
    const val biometricSubtitle = "생체 인증으로 앱에 접근하세요"
}


@Singleton
class BiometricAuthManager @Inject constructor() {
    fun isBiometricAvailable(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)

        return when (biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }

    /**
     * 생체 인증 다이얼로그를 표시하고 결과를 콜백으로 전달
     *
     * @param activity 생체 인증 다이얼로그를 띄울 Activity
     * @param onSuccess 인증 성공 시 호출
     * @param onFailed 인증 시도 실패 시 호출 (지문 불일치 등, 재시도 가능)
     * @param onError 인증 불가 상태 시 호출 (취소, 잠금 등, 다이얼로그 닫힘)
     */
    fun showBiometricPrompt(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onFailed: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            //_ 인증 성공 — HomeScreen으로 진입
            override fun onAuthenticationSucceeded(
                result: BiometricPrompt.AuthenticationResult
            ) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            //_ 인증 시도 실패 — 지문 불일치, 얼굴 인식 실패 등
            //_ 다이얼로그는 유지되어 자동으로 재시도 가능
            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onFailed()
            }

            //_ 인증 불가 상태 — 취소, 연속 실패 잠금, 센서 오류 등
            //_ 다이얼로그가 닫히므로 버튼을 눌러 재시도해야 함
            override fun onAuthenticationError(
                errorCode: Int,
                errString: CharSequence
            ) {
                super.onAuthenticationError(errorCode, errString)
                onError(errString.toString())
            }
        }

        val biometricPrompt = BiometricPrompt(activity, executor, callback)

        //_ BIOMETRIC_STRONG: 지문/얼굴 인식
        //_ DEVICE_CREDENTIAL: PIN/패턴/비밀번호 (생체 인증 불가 시 대체 수단)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(BiometricAuthManagerConstants.biometricTitle)
            .setSubtitle(BiometricAuthManagerConstants.biometricSubtitle)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        //_ 인증 시작 — 다이얼로그 표시
        biometricPrompt.authenticate(promptInfo)
    }
}