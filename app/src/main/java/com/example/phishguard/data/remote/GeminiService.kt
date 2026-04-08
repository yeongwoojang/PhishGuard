package com.example.phishguard.data.remote

import android.util.Log
import com.example.phishguard.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import javax.inject.Inject

class GeminiService @Inject constructor() {
    companion object Constants {
        const val geminiModelName = "gemini-2.5-flash"
    }
    private val TAG = "GeminiService"

    private val model = GenerativeModel(
        modelName = geminiModelName,
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    suspend fun analyzeMessage(
        text: String,
        sender: String
    ): GeminiAnalysisResult {
        return try {
            val response = model.generateContent(buildPrompt(text, sender))
            val rawText = response.text ?: ""
            Log.d(TAG, "Gemini 원본 응답: $rawText")
            parseResponse(rawText)
        } catch (e: Exception) {
            Log.e(TAG, "Gemini 실패: ${e.message}")
            //_ API 실패 시 CAUTION으로 처리 — 실패했다고 안전으로 판정하면 위험
            GeminiAnalysisResult(
                isPhishing = true,
                confidence = 0.5f,  // 0f → 0.5f 로 변경
                reason = "AI 분석 중 오류가 발생했습니다. 주의가 필요합니다."
            )
        }
    }

    private fun buildPrompt(text: String, sender: String): String = """
        다음 문자가 피싱인지 판단해줘.
        
        발신자: $sender
        문자 내용: $text
        
        아래 JSON 형식으로만 답해줘. 다른 말은 절대 하지 마:
        {
          "is_phishing": true 또는 false,
          "confidence": 0.0~1.0 사이 숫자,
          "reason": "판단 이유 한 문장"
        }
    """.trimIndent()

    private fun parseResponse(raw: String): GeminiAnalysisResult {
        return try {
            val cleaned = raw
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val isPhishing = cleaned
                .substringAfter("\"is_phishing\":")
                .substringBefore(",")
                .trim()
                .toBoolean()

            val confidence = cleaned
                .substringAfter("\"confidence\":")
                .substringBefore(",")
                .trim()
                .toFloat()

            val reason = cleaned
                .substringAfter("\"reason\":")
                .trim()
                .removePrefix("\"") //_ 앞 따옴표 제거
                .substringBefore("\"") //_ 뒤 따옴표까지 자르기
                .trim()

            Log.d(TAG, "파싱 결과 → isPhishing: $isPhishing, confidence: $confidence, reason: $reason")

            GeminiAnalysisResult(isPhishing, confidence, reason)

        } catch (e: Exception) {
            Log.e(TAG, "파싱 실패: ${e.message}, 원본: $raw")
            GeminiAnalysisResult(
                isPhishing = false,
                confidence = 0f,
                reason = "응답 파싱 실패"
            )
        }
    }
}

data class GeminiAnalysisResult(
    val isPhishing: Boolean,
    val confidence: Float,
    val reason: String
)