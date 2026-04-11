package com.example.phishguard.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.phishguard.MainActivity
import com.example.phishguard.core.constants.SystemConstants
import com.example.phishguard.domain.model.RiskLevel
import com.example.phishguard.domain.model.ThreatResult
import com.example.phishguard.domain.usecase.AnalyzeMessageUseCase
import com.google.mlkit.vision.text.internal.LoggingUtils
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PhishGuardNotificationService : NotificationListenerService() {
    private val TAG = "PhishGuardNotificationService"

    @Inject
    lateinit var analyzeMessageUseCase: AnalyzeMessageUseCase

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    //_ 중복 방지를 위해 최근 분석한 문자 해시 저장
    private val recentlyAnalyzed = mutableSetOf<Int>()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)

        val extras = sbn.notification.extras
        val text = extras.getString(Notification.EXTRA_TEXT) ?: ""
        val sender = extras.getString(Notification.EXTRA_TITLE) ?: sbn.packageName

        //_ 시스템이 백그라운드에서 작업중일 때 보내는 알림이 온 경우 처리하지 않고 return
        val isOngoing = (sbn.notification.flags and Notification.FLAG_ONGOING_EVENT) != 0
        if (isOngoing) {
            return
        }

        //_ 빈 문자열이면 분석 스킵
        if (text.isBlank()) {
            Log.d(TAG, " 빈 문자열 스킵")
            return
        }

        if (!isMessageNotification(sbn.packageName, text)) {
            Log.d(TAG, "문자 알림 아님, 스킵")
            return
        }

        //_ 중복 체크 — 같은 문자는 5초 안에 재분석 하지 않도록 함.
        val messageHash = (sender + text).hashCode()
        if (recentlyAnalyzed.contains(messageHash)) {
            Log.d(TAG, "중복 알림 스킵")
            return
        }

        recentlyAnalyzed.add(messageHash)

        //_ 5초 후 해시 제거 (같은 문자 재수신 허용)
        serviceScope.launch {
            kotlinx.coroutines.delay(5000)
            recentlyAnalyzed.remove(messageHash)
        }

        Log.d(TAG, "→ 문자 알림 감지! 발신자: $sender")

        serviceScope.launch {
            try {
                val result = analyzeMessageUseCase(text, sender)
                if (result.riskLevel != RiskLevel.SAFE) {
                    showWarningNotification(result)
                }
            } catch (e: Exception) {
                Log.e(TAG, "분석 실패: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun isMessageNotification(
        packageName: String,
        text: String
    ): Boolean {
        //_ 문자 앱 패키지명
        val messagePackages = listOf(
            SystemConstants.MMS_PACKAGE_NAME,
            SystemConstants.SAMSUNG_ANDROID_MESSAGING_PACKAGE_NAME,
            SystemConstants.GOOGLE_ANDROID_MESSAGING_PACKAGE_NAME
        )

        //_ 카드사 알림 앱
        val financePackages = listOf(
            SystemConstants.KAKAOTALK_PACKAGE_NAME,
            SystemConstants.NHN_ANDROID_SEARCH_PACKAGE_NAME
        )

        if (packageName in messagePackages) return true
        if (packageName in financePackages) return true

        //_ 텍스트 안에 금융 관련된 키워드 있으면 처리
        val financialKeywords = listOf("승인", "결제", "이체", "입금", "출금")
        return financialKeywords.any { text.contains(it) }
    }

    private fun showWarningNotification(result: ThreatResult) {
        val channelId = "phishguard_warning"

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

        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_THREAT_ID, result.id)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this,
            result.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(icon)
            .setContentText(result.reason)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(
            System.currentTimeMillis().toInt(),
            notification
        )
    }
}