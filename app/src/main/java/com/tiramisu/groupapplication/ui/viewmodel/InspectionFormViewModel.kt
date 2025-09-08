package com.tiramisu.groupapplication.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiramisu.groupapplication.domain.enums.InspectionStatus
import com.tiramisu.groupapplication.domain.model.Inspection
import com.tiramisu.groupapplication.domain.repository.InspectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

sealed class InspectionFormState {
    object Loading : InspectionFormState()
    object Idle : InspectionFormState()
    object Success : InspectionFormState()
    data class Error(val message: String) : InspectionFormState()
}

data class InspectionFormInfo(
    val createdAtMillis: Long = System.currentTimeMillis(),
    val type: String = "",
    val title: String = "",
    val notes: String = "",
    val score: String = "",
    val outcome: String = "",
    val status: InspectionStatus = InspectionStatus.PENDING_REVIEW,
    val saving: Boolean = false,
    val errors: InspectionFormError = InspectionFormError(),
    val generalError: String? = null, // non-field error (e.g., repo failure)
    val savedId: String? = null,
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
    val formData = MutableStateFlow(InspectionFormInfo())

    suspend fun loadForEdit(inspectionId: String) {
        _uiState.value = InspectionFormState.Loading
        repo.getReportById(inspectionId).collect { inspection ->
            if (inspection == null) {
                _uiState.value = InspectionFormState.Error("Failed to get Inspection")
                return@collect
            }
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
            savedId = inspection.id,
        )
    }

    fun setDateTime(epochMillis: Long) {
        val cur = formData.value
        formData.value = cur.copy(
            createdAtMillis = epochMillis,
            savedId = null,
            generalError = null
        )
    }

    fun setType(type: String) {
        val cur = formData.value
        formData.value = cur.copy(
            type = type,
            errors = cur.errors.copy(type = validateType(type)),
            savedId = null,
            generalError = null
        )
    }

    fun setTitle(title: String) {
        val cur = formData.value
        formData.value = cur.copy(
            title = title,
            errors = cur.errors.copy(title = validateTitle(title)),
            savedId = null,
            generalError = null
        )
    }

    fun setNotes(notes: String) {
        val cur = formData.value
        formData.value = cur.copy(
            notes = notes,
            errors = cur.errors.copy(notes = validateNotes(notes)),
            savedId = null,
            generalError = null
        )
    }

    fun setScore(score: String) {
        val cur = formData.value
        formData.value = cur.copy(
            score = score,
            errors = cur.errors.copy(score = validateScore(score)),
            savedId = null,
            generalError = null
        )
    }

    fun setOutcome(outcome: String) {
        val cur = formData.value
        formData.value = cur.copy(
            outcome = outcome,
            errors = cur.errors.copy(outcome = validateOutcome(outcome)),
            savedId = null,
            generalError = null
        )
    }

    fun setStatus(status: InspectionStatus) {
        val cur = formData.value
        formData.value = cur.copy(
            status = status,
            savedId = null,
            generalError = null
        )
    }

    fun setLocation(lat: Double, lng: Double) {
        val cur = formData.value
        formData.value = cur.copy(
            lat = lat,
            lng = lng,
            savedId = null,
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
                    generalError = null,
                    savedId = null
                )

                val inspection = Inspection(
                    id = "",
                    createdAt = Instant.fromEpochMilliseconds(data.createdAtMillis),
                    type = data.type.trim(),
                    title = data.title.trim(),
                    notes = data.notes.trim(),
                    score = data.score.trim(),
                    outcome = data.outcome.trim(),
                    status = data.status,
                    lat = data.lat,
                    lng = data.lng,
                    isSynced = false
                )

                val id = repo.upsert(inspection)
                formData.value = formData.value.copy(saving = false, savedId = id)
                _uiState.value = InspectionFormState.Success
            } catch (t: Throwable) {
                formData.value = formData.value.copy(
                    saving = false,
                    generalError = t.message ?: "Failed to save"
                )
            }
        }
    }

    fun clearSavedFlag() {
        formData.value = formData.value.copy(savedId = null)
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