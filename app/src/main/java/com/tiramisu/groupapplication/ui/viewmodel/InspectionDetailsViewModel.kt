package com.tiramisu.groupapplication.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiramisu.groupapplication.domain.model.Inspection
import com.tiramisu.groupapplication.domain.repository.InspectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InspectionDetailsState(
    val inspection: Inspection? = null,
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val deleteInProgress: Boolean = false,
    val error: Throwable? = null
)

sealed interface InspectionDetailsEvent {
    data object Deleted : InspectionDetailsEvent
    data class Error(val message: String) : InspectionDetailsEvent
}

@HiltViewModel
class InspectionDetailsViewModel @Inject constructor(
    private val repo: InspectionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val inspectionId: String = checkNotNull(savedStateHandle["inspectionId"]) {
        "inspectionId is required"
    }

    private val deleteInProgress = MutableStateFlow(false)
    private val lastError = MutableStateFlow<Throwable?>(null)

    val uiState: StateFlow<InspectionDetailsState> =
        combine(
            repo.getReportById(inspectionId),
            repo.syncState,
            deleteInProgress,
            lastError
        ) { inspection, sync, deleting, error ->
            InspectionDetailsState(
                inspection = inspection,
                isLoading = (inspection == null && error == null),
                isSyncing = sync.syncing,
                deleteInProgress = deleting,
                error = error
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = InspectionDetailsState()
        )

    private val _events =
        Channel<InspectionDetailsEvent>(capacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val events = _events.receiveAsFlow()

    fun deleteInspection() = viewModelScope.launch {
        if (deleteInProgress.value) return@launch
        deleteInProgress.value = true
        runCatching { repo.deleteLocal(inspectionId) }
            .onSuccess { _events.send(InspectionDetailsEvent.Deleted) }
            .onFailure {
                lastError.value = it
                _events.send(
                    InspectionDetailsEvent.Error(
                        it.message ?: "Failed to delete inspection"
                    )
                )
            }
        deleteInProgress.value = false
    }
}