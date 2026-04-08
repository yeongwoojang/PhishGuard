package com.example.phishguard.domain.usecase

import com.example.phishguard.domain.repository.ThreatRepository
import javax.inject.Inject

class DeleteAllThreatsUseCase @Inject constructor(
    private val repository: ThreatRepository
) {
    suspend operator fun invoke() {
        repository.deleteAllThreats()
    }
}