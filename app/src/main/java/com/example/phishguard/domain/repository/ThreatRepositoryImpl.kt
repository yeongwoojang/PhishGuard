package com.example.phishguard.domain.repository

import android.util.Log
import com.example.phishguard.data.local.dao.ThreatDao
import com.example.phishguard.data.local.entity.toDomain
import com.example.phishguard.data.local.entity.toEntity
import com.example.phishguard.data.remote.GeminiService
import com.example.phishguard.domain.model.RiskLevel
import com.example.phishguard.domain.model.ThreatResult
import com.example.phishguard.ml.PhishingDetector
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThreatRepositoryImpl @Inject constructor(
    private val geminiService: GeminiService,
    private val threatDao: ThreatDao,
    private val phishingDetector: PhishingDetector
) : ThreatRepository {

    private val TAG = "ThreatRepositoryImpl"

    /**
     * 메세지 분석
     * 1차 하드코딩 데이터 포함여부 분석
     * 2차 TFLite로 학습한 데이터로 분석 (피싱문자59개, 정상문자 43개를 50번 반복해서 쌓은 데이터) - phishing_model.tflite
     * 3차 Gemini를 통해 분석 요청
     */
    override suspend fun analyzeMessage(
        text: String,
        sender: String
    ): ThreatResult {

        Log.d(TAG, "===== 분석 시작 =====")
        Log.d(TAG, "발신자: $sender")
        Log.d(TAG, "문자내용: ${text.take(50)}")

        val quickScore = quickAnalyze(text) //_ 하드코딩한 데이터로 1차 분석
        val tfliteScore = phishingDetector.analyze(text)

        val finalScore = if (tfliteScore >= 0f) {
            Log.d(TAG, "TFLite: $tfliteScore / 규칙: $quickScore")
            (tfliteScore * 0.2f) + (quickScore * 0.8f) //_ tfLite 모델 학습 데이터가 적기 때문에 우선은 하드코딩 분석 가중치를 높게 잡음.
        } else {
            Log.d(TAG, "TFLite 실패 → 규칙 기반 폴백: $quickScore")
            quickScore
        }
        Log.d(TAG, "1차 판별 점수: $finalScore")

        val result = if (finalScore in 0.31f..0.60f) {
            Log.d(TAG, "→ 애매한 케이스 Gemini 호출")
            val geminiResult = geminiService.analyzeMessage(text, sender)
            Log.d(TAG, "→ Gemini 결과: ${geminiResult.isPhishing} / ${geminiResult.reason}")
            ThreatResult(
                messageText = text,
                sender = sender,
                riskLevel = RiskLevel.from(geminiResult.confidence),
                riskScore = geminiResult.confidence,
                reason = geminiResult.reason,
                analyzedAt = System.currentTimeMillis(),
                isGeminiAnalyzed = true
            )
        } else {
            Log.d(TAG, "→ Gemini 호출 없음 / ${RiskLevel.from(finalScore)}")
            ThreatResult(
                messageText = text,
                sender = sender,
                riskLevel = RiskLevel.from(quickScore),
                riskScore = finalScore,
                reason = quickAnalyzeReason(quickScore),
                analyzedAt = System.currentTimeMillis(),
                isGeminiAnalyzed = false
            )
        }

        Log.d(TAG, "최종: ${result.riskLevel} (${result.riskScore})")
        Log.d(TAG, "===== 분석 완료 =====")

        val oneMinuteAgo = System.currentTimeMillis() - 60_000
        val isDuplicate = threatDao.countRecentDuplicate(text, oneMinuteAgo) > 0

        if (!isDuplicate) {
            threatDao.insert(result.toEntity())
            Log.d(TAG, "DB 저장 완료")
        } else {
            Log.d(TAG, "중복 문자 → DB 저장 스킵")
        }
        return result
    }

    override fun getThreatHistory(): Flow<List<ThreatResult>> {
        return threatDao.getAllThreats()
            .map { entities -> entities.map { it.toDomain() } }
    }

    override fun getDangerousThreats(): Flow<List<ThreatResult>> {
        return threatDao.getDangerousThreats()
            .map { entities -> entities.map { it.toDomain() } }
    }

    /**
     * 하드코딩 데이터로 1차 분석
     *
     * @param text: 분석할 메세지
     */
    private fun quickAnalyze(text: String): Float {
        var score = 0f

        // 위험 키워드 체크
        val dangerKeywords = listOf(
            "계좌정지", "계좌 정지", "인증하세요", "긴급",
            "당첨", "환급", "클릭하세요", "개인정보"
        )
        val cautionKeywords = listOf(
            "확인하세요", "안내드립니다", "문의", "배송"
        )

        dangerKeywords.forEach { keyword ->
            if (text.contains(keyword)) score += 0.25f
        }
        cautionKeywords.forEach { keyword ->
            if (text.contains(keyword)) score += 0.1f
        }

        // 단축 URL 체크
        val shortUrlPatterns = listOf("bit.ly", "tinyurl", "goo.gl", "han.gl")
        shortUrlPatterns.forEach { pattern ->
            if (text.contains(pattern)) score += 0.3f
        }

        // Web발신 체크
        if (text.contains("[Web발신]")) score += 0.15f

        return score.coerceIn(0f, 1f)
    }

    private fun quickAnalyzeReason(score: Float): String = when {
        score > 0.6f -> "위험 키워드 및 의심 URL이 다수 감지되었습니다."
        score > 0.3f -> "일부 의심 패턴이 감지되었습니다."
        else -> "위험 요소가 감지되지 않았습니다."
    }
}