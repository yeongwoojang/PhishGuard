package com.example.phishguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.phishguard.core.security.BiometricAuthManager
import com.example.phishguard.presentation.auth.BiometricScreen
import com.example.phishguard.presentation.home.HomeScreen
import com.example.phishguard.ui.theme.PhishGuardTheme
import com.google.ai.client.generativeai.GenerativeModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
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

                if (isAuthenticated) {
                    HomeScreen()
                } else {
                    BiometricScreen(
                        biometricAuthManager = biometricAuthManager,
                        onAuthSuccess = { isAuthenticated = true }
                    )
                }
            }
        }
    }
}

suspend fun testGemini(): String {
    return try {
        val model = GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY
        )

        // 테스트용 가짜 피싱 문자
        val testMessage = "[Web발신] 고객님 계좌가 정지되었습니다. 지금 바로 인증하세요 → http://bit.ly/kb-auth"

        val prompt = """
            다음 문자가 피싱인지 판단해줘.
            문자 내용: "$testMessage"
            
            아래 JSON 형식으로만 답해줘. 다른 말은 하지 마:
            {
              "is_phishing": true,
              "confidence": 0.0~1.0 사이 숫자,
              "reason": "판단 이유 한 문장"
            }
        """.trimIndent()

        val response = model.generateContent(prompt)
        response.text ?: "응답 없음"

    } catch (e: Exception) {
        "에러 발생: ${e.message}"
    }
}