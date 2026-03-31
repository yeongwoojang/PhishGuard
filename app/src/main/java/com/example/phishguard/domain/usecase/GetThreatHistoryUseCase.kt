package com.example.phishguard.domain.usecase

import com.example.phishguard.domain.model.ThreatResult
import com.example.phishguard.domain.repository.ThreatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetThreatHistoryUseCase @Inject constructor(
    private val repository: ThreatRepository
) {
    operator fun invoke(): Flow<List<ThreatResult>> {
        return repository.getThreatHistory()
    }
}