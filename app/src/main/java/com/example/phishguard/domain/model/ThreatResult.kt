package com.example.phishguard.domain.model

data class ThreatResult(
    val messageText: String,      // 원본 문자 내용
    val sender: String,           // 발신자 번호
    val riskLevel: RiskLevel,     // 위험 등급
    val riskScore: Float,         // 위험도 점수 (0.0 ~ 1.0)
    val reason: String,           // 판별 이유 (Gemini 설명)
    val analyzedAt: Long,         // 분석 시각 (timestamp)
    val isGeminiAnalyzed: Boolean // Gemini 2차 분석 여부
)