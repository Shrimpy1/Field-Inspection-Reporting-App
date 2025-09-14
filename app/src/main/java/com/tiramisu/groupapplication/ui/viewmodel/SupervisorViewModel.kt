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
import javax.inject.Inject

data class SupervisorDashboardState(
    val items: List<Inspection> = emptyList(),
    val isSyncing: Boolean = true,
    val error: Throwable? = null
)

@HiltViewModel
class SupervisorViewModel @Inject constructor(
    private val inspectionRepo: InspectionRepository
) : ViewModel() {
    val uiState: StateFlow<SupervisorDashboardState> =
        combine(inspectionRepo.getAllReports(), inspectionRepo.syncState) { items, sync ->
            SupervisorDashboardState(items = items, isSyncing = sync.syncing, error = sync.error)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SupervisorDashboardState())

    init {
        inspectionRepo.startRealtimeSync()
    }
}