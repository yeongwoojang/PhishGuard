package com.example.phishguard.domain.model

data class ThreatResult(
    val id: Long = 0,
    val messageText: String, //_ 원본 문자 내용
    val sender: String, //_ 발신자 번호
    val riskLevel: RiskLevel, //_ 위험 등급
    val riskScore: Float, //_ 위험도 점수 (0.0 ~ 1.0)
    val reason: String, //_ 판별 이유 (Gemini 설명)
    val analyzedAt: Long, //_ 분석 시각 (timestamp)
    val isGeminiAnalyzed: Boolean //_ Gemini 2차 분석 여부
)