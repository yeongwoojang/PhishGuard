package com.example.phishguard.domain.usecase

import com.example.phishguard.domain.model.ThreatResult
import com.example.phishguard.domain.repository.ThreatRepository
import javax.inject.Inject

class AnalyzeMessageUseCase @Inject constructor(
    private val repository: ThreatRepository
) {
    suspend operator fun invoke(
        text: String,
        sender: String
    ): ThreatResult {
        return repository.analyzeMessage(text, sender)
    }
}