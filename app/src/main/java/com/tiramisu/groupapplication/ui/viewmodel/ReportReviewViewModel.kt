package com.tiramisu.groupapplication.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiramisu.groupapplication.domain.enums.InspectionStatus
import com.tiramisu.groupapplication.domain.model.Inspection
import com.tiramisu.groupapplication.domain.repository.AuthRepository
import com.tiramisu.groupapplication.domain.repository.InspectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class ReportReviewState(
    val inspection: Inspection? = null,
    val reviewNotes: String = "",
    val isSaving: Boolean = false,
    val error: String? = null,
    val isFinished: Boolean = false
)

@HiltViewModel
@OptIn(ExperimentalTime::class)
class ReportReviewViewModel @Inject constructor(
    private val inspectionRepo: InspectionRepository,
    private val authRepo: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val inspectionId: String = savedStateHandle.get<String>("inspectionId")!!

    private val _state = MutableStateFlow(ReportReviewState())
    val state = _state.asStateFlow()

    init {
        loadInspection()
    }

    private fun loadInspection() {
        viewModelScope.launch {
            val inspection = inspectionRepo.getAllReports().first().find { it.id == inspectionId }
            _state.value = _state.value.copy(inspection = inspection)
        }
    }

    fun onReviewNotesChanged(notes: String) {
        _state.value = _state.value.copy(reviewNotes = notes)
    }

    fun approveReport() {
        updateReportStatus(InspectionStatus.PASSED, "Approved")
    }

    fun rejectReport() {
        if (_state.value.reviewNotes.isBlank()) {
            _state.value = _state.value.copy(error = "Review notes are required for rejection.")
            return
        }
        updateReportStatus(InspectionStatus.FAILED, _state.value.reviewNotes)
    }

    private fun updateReportStatus(newStatus: InspectionStatus, notes: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)
            val currentUser = authRepo.currentUser.first()
            val originalInspection = _state.value.inspection

            if (currentUser == null || originalInspection == null) {
                _state.value = _state.value.copy(error = "User or inspection not found.", isSaving = false)
                return@launch
            }

            val updatedInspection = originalInspection.copy(
                status = newStatus,
                reviewNotes = notes,
                reviewedBy = currentUser.email,
                reviewedAt = Clock.System.now()
            )

            try {
                inspectionRepo.updateInspection(updatedInspection)
                _state.value = _state.value.copy(isSaving = false, isFinished = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSaving = false, error = e.message)
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
