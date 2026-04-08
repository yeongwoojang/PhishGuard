package com.example.phishguard.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phishguard.domain.model.ThreatResult
import com.example.phishguard.domain.usecase.AnalyzeMessageUseCase
import com.example.phishguard.domain.usecase.DeleteAllThreatsUseCase
import com.example.phishguard.domain.usecase.DeleteThreatUseCase
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
    private val getThreatHistoryUseCase: GetThreatHistoryUseCase,
    private val deleteThreatUseCase: DeleteThreatUseCase,
    private val deleteAllThreatsUseCase: DeleteAllThreatsUseCase
) : ViewModel() {

    private val _messageTestState = MutableStateFlow<HomeUiState>(HomeUiState.Idle)
    val messageTestState: StateFlow<HomeUiState> = _messageTestState.asStateFlow()

    //_ Room DB에 있는 메세지 문석 이력 Flow
    val threatHistory = getThreatHistoryUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * 문자 직접 입력하여 분석 테스트 메소드
     * @param text: 테스트할 문자 내용
     * @param sender: 테스트할 보낸 사람 명
     */
    fun analyzeMessage(text: String, sender: String) {
        viewModelScope.launch {
            _messageTestState.value = HomeUiState.Loading
            try {
                val result = analyzeMessageUseCase(text, sender)
                _messageTestState.value = HomeUiState.Success(result)
            } catch (e: Exception) {
                _messageTestState.value = HomeUiState.Error(e.message ?: "알 수 없는 오류")
            }
        }
    }

    //_ 개별 삭제
    fun deleteThreat(id: Long) {
        viewModelScope.launch {
            deleteThreatUseCase(id)
        }
    }

    //_ 전체 삭제
    fun deleteAllThreats() {
        viewModelScope.launch {
            deleteAllThreatsUseCase()
        }
    }
}

sealed class HomeUiState {
    object Idle : HomeUiState()
    object Loading : HomeUiState()
    data class Success(val result: ThreatResult) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}