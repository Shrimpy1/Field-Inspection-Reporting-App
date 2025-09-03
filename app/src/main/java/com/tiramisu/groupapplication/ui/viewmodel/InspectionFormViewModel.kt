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

data class InspectionFormState(
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
    val savedId: String? = null
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
    private val _state = MutableStateFlow(InspectionFormState())
    val state: StateFlow<InspectionFormState> = _state.asStateFlow()

    fun setDateTime(epochMillis: Long) {
        val cur = _state.value
        _state.value = cur.copy(
            createdAtMillis = epochMillis,
            savedId = null,
            generalError = null
        )
    }

    fun setType(type: String) {
        val cur = _state.value
        _state.value = cur.copy(
            type = type,
            errors = cur.errors.copy(type = validateType(type)),
            savedId = null,
            generalError = null
        )
    }

    fun setTitle(title: String) {
        val cur = _state.value
        _state.value = cur.copy(
            title = title,
            errors = cur.errors.copy(title = validateTitle(title)),
            savedId = null,
            generalError = null
        )
    }

    fun setNotes(notes: String) {
        val cur = _state.value
        _state.value = cur.copy(
            notes = notes,
            errors = cur.errors.copy(notes = validateNotes(notes)),
            savedId = null,
            generalError = null
        )
    }

    fun setScore(score: String) {
        val cur = _state.value
        _state.value = cur.copy(
            score = score,
            errors = cur.errors.copy(score = validateScore(score)),
            savedId = null,
            generalError = null
        )
    }

    fun setOutcome(outcome: String) {
        val cur = _state.value
        _state.value = cur.copy(
            outcome = outcome,
            errors = cur.errors.copy(outcome = validateOutcome(outcome)),
            savedId = null,
            generalError = null
        )
    }

    fun setStatus(status: InspectionStatus) {
        val cur = _state.value
        _state.value = cur.copy(
            status = status,
            savedId = null,
            generalError = null
        )
    }

    @OptIn(ExperimentalTime::class)
    fun save() {
        val s = _state.value

        // Validate all fields at once
        val e = InspectionFormError(
            type = validateType(s.type),
            title = validateTitle(s.title),
            notes = validateNotes(s.notes),
            score = validateScore(s.score),
            outcome = validateOutcome(s.outcome)
        )

        if (e.hasAny) {
            _state.value = s.copy(errors = e, generalError = null)
            return
        }

        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(
                    saving = true,
                    errors = InspectionFormError(),
                    generalError = null,
                    savedId = null
                )

                val inspection = Inspection(
                    id = "",
                    createdAt = Instant.fromEpochMilliseconds(s.createdAtMillis),
                    type = s.type.trim(),
                    title = s.title.trim(),
                    notes = s.notes.trim(),
                    score = s.score.trim(),
                    outcome = s.outcome.trim(),
                    status = s.status,
                    lat = null, // TODO implement map
                    long = null, // TODO implement map
                    isSynced = false
                )

                val id = repo.upsert(inspection)
                _state.value = _state.value.copy(saving = false, savedId = id)
            } catch (t: Throwable) {
                _state.value = _state.value.copy(
                    saving = false,
                    generalError = t.message ?: "Failed to save"
                )
            }
        }
    }

    fun clearSavedFlag() {
        _state.value = _state.value.copy(savedId = null)
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