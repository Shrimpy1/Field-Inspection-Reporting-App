package com.tiramisu.groupapplication.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiramisu.groupapplication.domain.enums.InspectionStatus
import com.tiramisu.groupapplication.domain.model.Inspection
import com.tiramisu.groupapplication.domain.repository.InspectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

sealed class InspectionFormState {
    object Loading : InspectionFormState()
    object Idle : InspectionFormState()
    object Success : InspectionFormState()
    data class Error(val message: String) : InspectionFormState()
}

data class InspectionFormData(
    val createdAtMillis: Long = System.currentTimeMillis(),
    val type: String = "",
    val title: String = "",
    val notes: String = "",
    val score: String = "",
    val outcome: String = "",
    val status: InspectionStatus = InspectionStatus.PENDING_REVIEW,
    val saving: Boolean = false,
    val errors: InspectionFormError = InspectionFormError(),
    val generalError: String? = null,
    val editingId: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
)

data class InspectionFormError(
    val type: String? = null,
    val title: String? = null,
    val notes: String? = null,
    val score: String? = null,
    val outcome: String? = null,
) {
    val hasAny: Boolean
        get() = listOf(type, title, notes, score, outcome).any { !it.isNullOrBlank() }
}

@HiltViewModel
class InspectionFormViewModel @Inject constructor(
    private val repo: InspectionRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<InspectionFormState>(InspectionFormState.Idle)
    val uiState: StateFlow<InspectionFormState> = _uiState.asStateFlow()
    val formData = MutableStateFlow(InspectionFormData())

    sealed interface UiEvent {
        data class Saved(val id: String) : UiEvent
        data class Error(val message: String) : UiEvent
    }

    private val _events = MutableSharedFlow<UiEvent>(replay = 0, extraBufferCapacity = 1)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    fun loadForEdit(inspectionId: String) = viewModelScope.launch {
        _uiState.value = InspectionFormState.Loading
        val inspection = repo.getReportById(inspectionId).firstOrNull()
        if (inspection == null) {
            _uiState.value = InspectionFormState.Error("Failed to get Inspection")
        } else {
            initForEdit(inspection)
            _uiState.value = InspectionFormState.Idle
        }
    }

    fun initForEdit(inspection: Inspection) {
        val cur = formData.value
        formData.value = cur.copy(
            createdAtMillis = inspection.createdAt.toEpochMilliseconds(),
            type = inspection.type,
            title = inspection.title,
            notes = inspection.notes,
            score = inspection.score,
            outcome = inspection.outcome,
            status = inspection.status,
            lat = inspection.lat,
            lng = inspection.lng,
            editingId = inspection.id,
            generalError = null
        )
    }

    fun setDateTime(epochMillis: Long) {
        val cur = formData.value
        formData.value = cur.copy(
            createdAtMillis = epochMillis,
            generalError = null
        )
    }

    fun setType(type: String) {
        val cur = formData.value
        formData.value = cur.copy(
            type = type,
            errors = cur.errors.copy(type = validateType(type)),
            generalError = null
        )
    }

    fun setTitle(title: String) {
        val cur = formData.value
        formData.value = cur.copy(
            title = title,
            errors = cur.errors.copy(title = validateTitle(title)),
            generalError = null
        )
    }

    fun setNotes(notes: String) {
        val cur = formData.value
        formData.value = cur.copy(
            notes = notes,
            errors = cur.errors.copy(notes = validateNotes(notes)),
            generalError = null
        )
    }

    fun setScore(score: String) {
        val cur = formData.value
        formData.value = cur.copy(
            score = score,
            errors = cur.errors.copy(score = validateScore(score)),
            generalError = null
        )
    }

    fun setOutcome(outcome: String) {
        val cur = formData.value
        formData.value = cur.copy(
            outcome = outcome,
            errors = cur.errors.copy(outcome = validateOutcome(outcome)),
            generalError = null
        )
    }

    fun setStatus(status: InspectionStatus) {
        val cur = formData.value
        formData.value = cur.copy(
            status = status,
            generalError = null
        )
    }

    fun setLocation(lat: Double, lng: Double) {
        val cur = formData.value
        formData.value = cur.copy(
            lat = lat,
            lng = lng,
            generalError = null
        )
    }

    fun setGeneralError(error: String?) {
        val cur = formData.value
        formData.value = cur.copy(generalError = error)
    }

    @OptIn(ExperimentalTime::class)
    fun save() {
        val data = formData.value

        // Validate all fields at once
        val e = InspectionFormError(
            type = validateType(data.type),
            title = validateTitle(data.title),
            notes = validateNotes(data.notes),
            score = validateScore(data.score),
            outcome = validateOutcome(data.outcome)
        )

        if (e.hasAny) {
            formData.value = data.copy(errors = e, generalError = null)
            return
        }

        viewModelScope.launch {
            _uiState.value = InspectionFormState.Loading

            try {
                formData.value = formData.value.copy(
                    saving = true,
                    errors = InspectionFormError(),
                    generalError = null
                )

                val inspection = Inspection(
                    id = data.editingId ?: UUID.randomUUID().toString(),
                    createdAt = Instant.fromEpochMilliseconds(data.createdAtMillis),
                    type = data.type.trim(),
                    title = data.title.trim(),
                    notes = data.notes.trim(),
                    score = data.score.trim(),
                    outcome = data.outcome.trim(),
                    status = data.status,
                    lat = data.lat,
                    lng = data.lng,
                    pendingSync = false
                )

                repo.upsertLocal(inspection)
                formData.value = formData.value.copy(saving = false, editingId = inspection.id)
                _uiState.value = InspectionFormState.Success
                _events.emit(UiEvent.Saved(inspection.id))
            } catch (t: Throwable) {
                formData.value = formData.value.copy(
                    saving = false,
                    generalError = t.message ?: "Failed to save"
                )
            } finally {
                _uiState.value = InspectionFormState.Idle
            }
        }
    }

    // Validation helpers
    private fun validateType(type: String): String? =
        if (type.isBlank()) "Type is required" else null

    private fun validateTitle(title: String): String? =
        if (title.isBlank()) "Title is required" else null

    private fun validateNotes(notes: String): String? =
        if (notes.isBlank()) "Notes is required" else null

    private fun validateScore(score: String): String? =
        if (score.isBlank()) "Score is required"
        else if (score.toIntOrNull() == null) "Score must be a number"
        else null

    private fun validateOutcome(outcome: String): String? =
        if (outcome.isBlank()) "Outcome is required" else null
}