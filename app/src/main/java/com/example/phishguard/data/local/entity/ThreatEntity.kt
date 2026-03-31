package com.example.phishguard.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.phishguard.domain.model.RiskLevel
import com.example.phishguard.domain.model.ThreatResult

@Entity(tableName = "threats")
data class ThreatEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val messageText: String,
    val sender: String,
    val riskLevel: String,      // RiskLevel enum → String으로 저장
    val riskScore: Float,
    val reason: String,
    val analyzedAt: Long,
    val isGeminiAnalyzed: Boolean
)

// Entity → Domain 모델 변환
fun ThreatEntity.toDomain(): ThreatResult = ThreatResult(
    messageText = messageText,
    sender = sender,
    riskLevel = RiskLevel.valueOf(riskLevel),
    riskScore = riskScore,
    reason = reason,
    analyzedAt = analyzedAt,
    isGeminiAnalyzed = isGeminiAnalyzed
)

// Domain 모델 → Entity 변환
fun ThreatResult.toEntity(): ThreatEntity = ThreatEntity(
    messageText = messageText,
    sender = sender,
    riskLevel = riskLevel.name,
    riskScore = riskScore,
    reason = reason,
    analyzedAt = analyzedAt,
    isGeminiAnalyzed = isGeminiAnalyzed
)