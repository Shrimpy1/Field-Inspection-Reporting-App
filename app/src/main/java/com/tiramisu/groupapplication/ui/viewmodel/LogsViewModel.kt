package com.tiramisu.groupapplication.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiramisu.groupapplication.domain.model.InspectionLog
import com.tiramisu.groupapplication.domain.repository.LogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LogsViewModel @Inject constructor(
    private val logsRepo: LogRepository
) : ViewModel() {
    val logs: StateFlow<List<InspectionLog>> =
        logsRepo.getAllLogs()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun exportAllLogs(target: Uri) {
        viewModelScope.launch {
            _isExporting.value = true
            _message.value = null
            _error.value = null
            try {
                // Use the repository's CSV export
                logsRepo.exportToCsv(targetUri = target)
                _message.value = "Exported logs to CSV"
            } catch (t: Throwable) {
                _error.value = t.message ?: "Failed to export CSV"
            } finally {
                _isExporting.value = false
            }
        }
    }
}