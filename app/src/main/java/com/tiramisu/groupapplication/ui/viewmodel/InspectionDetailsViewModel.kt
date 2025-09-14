package com.tiramisu.groupapplication.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiramisu.groupapplication.domain.model.Inspection
import com.tiramisu.groupapplication.domain.repository.AttachmentRepository
import com.tiramisu.groupapplication.domain.repository.AuthRepository
import com.tiramisu.groupapplication.domain.repository.InspectionRepository
import com.tiramisu.groupapplication.domain.repository.LogRepository
import com.tiramisu.groupapplication.ui.viewmodel.ui_state.AttachmentUi
import com.tiramisu.groupapplication.ui.viewmodel.ui_state.toUiList
import com.tiramisu.groupapplication.util.AttachmentLinkResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InspectionDetailsState(
    val userId: String? = null,
    val inspection: Inspection? = null,
    val attachmentItems: List<AttachmentUi> = emptyList(),
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
    private val attachmentRepo: AttachmentRepository,
    private val logRepo: LogRepository,
    private val authRepo: AuthRepository,
    savedStateHandle: SavedStateHandle,
    private val linkResolver: AttachmentLinkResolver
) : ViewModel() {
    private val inspectionId: String = checkNotNull(savedStateHandle["inspectionId"]) {
        "inspectionId is required"
    }

    private val deleteInProgress = MutableStateFlow(false)
    private val lastError = MutableStateFlow<Throwable?>(null)
    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()
    private val _exportMessage = MutableStateFlow<String?>(null)
    val exportMessage: StateFlow<String?> = _exportMessage.asStateFlow()
    private val _exportError = MutableStateFlow<String?>(null)
    val exportError: StateFlow<String?> = _exportError.asStateFlow()


    val uiState: StateFlow<InspectionDetailsState> =
        combine(
            repo.getReportById(inspectionId),
            attachmentRepo.getAttachments(inspectionId)
                .onStart { attachmentRepo.hydrateFromRemoteIfEmpty(inspectionId) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()),
            repo.syncState,
            deleteInProgress,
            lastError
        ) { inspection, attachments, sync, deleting, error ->
            InspectionDetailsState(
                userId = authRepo.currentUser.firstOrNull()?.uid,
                inspection = inspection,
                attachmentItems = toUiList(attachments, linkResolver),
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

    init {
        viewModelScope.launch {
            attachmentRepo.hydrateFromRemoteIfEmpty(inspectionId)
        }
    }

    fun deleteInspection() = viewModelScope.launch {
        if (deleteInProgress.value) return@launch
        deleteInProgress.value = true
        runCatching { repo.deleteLocal(inspectionId) }
            .onSuccess {
                _events.send(InspectionDetailsEvent.Deleted)
                runCatching { logRepo.log(inspectionId, "DELETED") }
            }
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

    fun exportLogs(target: Uri) {
        viewModelScope.launch {
            _isExporting.value = true
            _exportMessage.value = null
            _exportError.value = null
            try {
                // Use the repository's CSV export
                logRepo.exportToCsv(targetUri = target, inspectionId = inspectionId)
                _exportMessage.value = "Exported logs to CSV"
            } catch (t: Throwable) {
                _exportError.value = t.message ?: "Failed to export CSV"
            } finally {
                _isExporting.value = false
            }
        }
    }
}