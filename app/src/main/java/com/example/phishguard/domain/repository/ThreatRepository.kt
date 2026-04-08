package com.example.phishguard.domain.repository

import com.example.phishguard.domain.model.ThreatResult
import kotlinx.coroutines.flow.Flow

interface ThreatRepository {

    //_ 문자 분석 요청
    suspend fun analyzeMessage(
        text: String,
        sender: String
    ): ThreatResult

    //_ 탐지 이력 조회
    fun getThreatHistory(): Flow<List<ThreatResult>>

    //_ 위험/주의 이력만 조회
    fun getDangerousThreats(): Flow<List<ThreatResult>>
    suspend fun deleteThreat(id: Long)

    suspend fun deleteAllThreats()
}