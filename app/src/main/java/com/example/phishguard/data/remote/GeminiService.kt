package com.example.phishguard.data.remote

import android.util.Log
import com.example.phishguard.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import javax.inject.Inject  // jakarta → javax 수정

class GeminiService @Inject constructor() {

    private val model = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    suspend fun analyzeMessage(
        text: String,
        sender: String
    ): GeminiAnalysisResult {
        return try {
            val response = model.generateContent(buildPrompt(text, sender))
            val rawText = response.text ?: ""
            Log.d("PhishGuard", "Gemini 원본 응답: $rawText")
            parseResponse(rawText)
        } catch (e: Exception) {
            GeminiAnalysisResult(
                isPhishing = false,
                confidence = 0f,
                reason = "분석 실패: ${e.message}"
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
            // 마크다운 제거
            val cleaned = raw
                .replace("```json", "")
                .replace("```", "")
                .trim()

            // is_phishing 파싱
            val isPhishing = cleaned
                .substringAfter("\"is_phishing\":")
                .substringBefore(",")
                .trim()
                .toBoolean()

            // confidence 파싱
            val confidence = cleaned
                .substringAfter("\"confidence\":")
                .substringBefore(",")
                .trim()
                .toFloat()

            // reason 파싱 — 공백 포함 처리
            val reason = cleaned
                .substringAfter("\"reason\":")
                .trim()
                .removePrefix("\"")   // 앞 따옴표 제거
                .substringBefore("\"") // 뒤 따옴표까지 자르기
                .trim()

            Log.d("PhishGuard", "파싱 결과 → isPhishing: $isPhishing, confidence: $confidence, reason: $reason")

            GeminiAnalysisResult(isPhishing, confidence, reason)

        } catch (e: Exception) {
            Log.e("PhishGuard", "파싱 실패: ${e.message}, 원본: $raw")
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