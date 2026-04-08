package com.example.phishguard.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phishguard.data.local.dao.ThreatDao
import com.example.phishguard.data.local.entity.toDomain
import com.example.phishguard.domain.model.ThreatResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val threatDao: ThreatDao
) : ViewModel() {

    private val _threat = MutableStateFlow<ThreatResult?>(null)
    val threat: StateFlow<ThreatResult?> = _threat.asStateFlow()

    fun loadThreat(id: Long) {
        viewModelScope.launch {
            val entity = threatDao.getThreatById(id)
            _threat.value = entity?.toDomain()
        }
    }
}