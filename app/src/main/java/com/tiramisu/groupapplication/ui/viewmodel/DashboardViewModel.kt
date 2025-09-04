package com.tiramisu.groupapplication.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiramisu.groupapplication.domain.model.Inspection
import com.tiramisu.groupapplication.domain.repository.InspectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
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
    private val syncing = MutableStateFlow(true)
    private val lastError = MutableStateFlow<Throwable?>(null)

    val uiState: StateFlow<DashboardState> =
        combine(repo.getAllReports(), syncing, lastError) { items, isSyncing, err ->
            DashboardState(items = items, isSyncing = isSyncing, error = err)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState())

    init {
        startRealtime()
    }

    private fun startRealtime() {
        repo.observeRealtime()
            .onStart {
                syncing.value = true
                lastError.value = null
            }
            .onEach {
                syncing.value = false
                lastError.value = null
            }
            .catch { e ->
                syncing.value = false
                lastError.value = e
            }
            .launchIn(viewModelScope)
    }

    fun refresh() {
        syncing.value = true
    }
}