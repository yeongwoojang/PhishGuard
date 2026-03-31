package com.example.phishguard.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phishguard.domain.model.ThreatResult
import com.example.phishguard.domain.usecase.AnalyzeMessageUseCase
import com.example.phishguard.domain.usecase.GetThreatHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val analyzeMessageUseCase: AnalyzeMessageUseCase,
    private val getThreatHistoryUseCase: GetThreatHistoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Idle)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val threatHistory = getThreatHistoryUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun analyzeMessage(text: String, sender: String) {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                val result = analyzeMessageUseCase(text, sender)
                _uiState.value = HomeUiState.Success(result)
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "알 수 없는 오류")
            }
        }
    }

    fun resetState() {
        _uiState.value = HomeUiState.Idle
    }
}

sealed class HomeUiState {
    object Idle : HomeUiState()
    object Loading : HomeUiState()
    data class Success(val result: ThreatResult) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}