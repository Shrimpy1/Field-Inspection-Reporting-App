package com.tiramisu.groupapplication.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiramisu.groupapplication.domain.model.Inspection
import com.tiramisu.groupapplication.domain.repository.InspectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardState(
    val items: List<Inspection> = emptyList(),
    val isSyncing: Boolean = true,
    val error: Throwable? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repo: InspectionRepository
) : ViewModel() {
    val uiState: StateFlow<DashboardState> =
        combine(repo.getAllReports(), repo.syncState) { items, sync ->
            DashboardState(items = items, isSyncing = sync.syncing, error = sync.error)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState())

    init {
        // Use the corrected function name
        repo.startRealtimeSync()
    }

    fun refresh() = viewModelScope.launch {
        // Use the corrected function signature
        repo.syncOnce()
    }
}

