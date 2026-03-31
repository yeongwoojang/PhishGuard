package com.example.phishguard.domain.model

enum class RiskLevel {
    SAFE,       // 0~30  → 안전
    CAUTION,    // 31~60 → 주의 (Gemini 2차 분석 트리거)
    DANGER;     // 61~100 → 위험

    companion object {
        fun from(score: Float): RiskLevel = when {
            score <= 0.3f -> SAFE
            score <= 0.6f -> CAUTION
            else -> DANGER
        }
    }
}