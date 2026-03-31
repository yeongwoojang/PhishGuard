package com.example.phishguard.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.phishguard.domain.model.RiskLevel
import com.example.phishguard.domain.model.ThreatResult
import com.example.phishguard.domain.usecase.AnalyzeMessageUseCase
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PhishGuardNotificationService : NotificationListenerService() {

    @Inject
    lateinit var analyzeMessageUseCase: AnalyzeMessageUseCase

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // 최근 분석한 문자 해시 저장 (중복 방지)
    private val recentlyAnalyzed = mutableSetOf<Int>()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)

        val extras = sbn.notification.extras
        val text = extras.getString(Notification.EXTRA_TEXT) ?: ""
        val sender = extras.getString(Notification.EXTRA_TITLE) ?: sbn.packageName

        if (!isMessageNotification(sbn.packageName, "", text)) return

        // 중복 체크 — 같은 문자는 5초 안에 재분석 안 함
        val messageHash = (sender + text).hashCode()
        if (recentlyAnalyzed.contains(messageHash)) {
            Log.d("PhishGuard", "→ 중복 알림 스킵")
            return
        }

        recentlyAnalyzed.add(messageHash)

        // 5초 후 해시 제거 (같은 문자 재수신 허용)
        serviceScope.launch {
            kotlinx.coroutines.delay(5000)
            recentlyAnalyzed.remove(messageHash)
        }

        Log.d("PhishGuard", "→ 문자 알림 감지! 발신자: $sender")

        serviceScope.launch {
            try {
                val result = analyzeMessageUseCase(text, sender)
                if (result.riskLevel != RiskLevel.SAFE) {
                    showWarningNotification(result)
                }
            } catch (e: Exception) {
                Log.e("PhishGuard", "분석 실패: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun isMessageNotification(
        packageName: String,
        title: String,
        text: String
    ): Boolean {
        // 문자 앱 패키지명
        val messagePackages = listOf(
            "com.android.mms",
            "com.samsung.android.messaging",
            "com.google.android.apps.messaging"
        )

        // 카드사 알림 앱
        val financePackages = listOf(
            "com.kakao.talk",
            "com.nhn.android.search"
        )

        if (packageName in messagePackages) return true
        if (packageName in financePackages) return true

        // 텍스트 안에 금융 키워드 있으면 처리
        val financialKeywords = listOf("승인", "결제", "이체", "입금", "출금")
        return financialKeywords.any { text.contains(it) }
    }

    private fun showWarningNotification(result: ThreatResult) {
        val channelId = "phishguard_warning"

        // 알림 채널 생성 (Android 8.0+)
        val channel = NotificationChannel(
            channelId,
            "PhishGuard 경고",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "피싱 문자 탐지 경고"
            enableVibration(true)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)

        val icon = when (result.riskLevel) {
            RiskLevel.DANGER -> "위험 문자 탐지"
            RiskLevel.CAUTION -> "의심 문자 탐지"
            RiskLevel.SAFE -> return
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(icon)
            .setContentText(result.reason)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(
            System.currentTimeMillis().toInt(),
            notification
        )
    }
}