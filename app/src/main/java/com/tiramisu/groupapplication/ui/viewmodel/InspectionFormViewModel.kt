package com.tiramisu.groupapplication.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiramisu.groupapplication.domain.enums.InspectionStatus
import com.tiramisu.groupapplication.domain.model.Inspection
import com.tiramisu.groupapplication.domain.model.InspectionAttachment
import com.tiramisu.groupapplication.domain.repository.AttachmentRepository
import com.tiramisu.groupapplication.domain.repository.InspectionRepository
import com.tiramisu.groupapplication.ui.viewmodel.ui_state.AttachmentUi
import com.tiramisu.groupapplication.ui.viewmodel.ui_state.toUi
import com.tiramisu.groupapplication.ui.viewmodel.ui_state.toUiList
import com.tiramisu.groupapplication.util.AttachmentLinkResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
    val attachments: List<AttachmentUi> = emptyList(),
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
    private val repo: InspectionRepository,
    private val attachmentRepo: AttachmentRepository,
    savedStateHandle: SavedStateHandle,
    private val linkResolver: AttachmentLinkResolver
) : ViewModel() {
    private companion object {
        const val KEY_DRAFT_ID = "draftInspectionId"
    }

    private val draftId: String =
        savedStateHandle.get<String>(KEY_DRAFT_ID)
            ?: UUID.randomUUID().toString().also { savedStateHandle[KEY_DRAFT_ID] = it }

    private val _uiState = MutableStateFlow<InspectionFormState>(InspectionFormState.Idle)
    val uiState: StateFlow<InspectionFormState> = _uiState.asStateFlow()
    val formData = MutableStateFlow(InspectionFormData(editingId = draftId))

    private var originalAttachmentIds: Set<String> = emptySet()

    sealed interface UiEvent {
        data class Saved(val id: String) : UiEvent
        data class Error(val message: String) : UiEvent
    }

    private val _events = MutableSharedFlow<UiEvent>(replay = 0, extraBufferCapacity = 1)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    // ----- Attachments stream for current inspection id -----

    /** Emits the current editingId (non-null), used to scope attachment flows. */
    private val editingIdFlow: Flow<String> =
        formData.map { it.editingId }.filterNotNull().distinctUntilChanged()

    /** UI collects this to render images (AsyncImage can take local Uri or public URL later). */
    @OptIn(ExperimentalCoroutinesApi::class)
    val attachments: StateFlow<List<InspectionAttachment>> =
        editingIdFlow
            .flatMapLatest { id -> attachmentRepo.getAttachments(id) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ----- Load/Edit -----

    fun loadForEdit(inspectionId: String) = viewModelScope.launch {
        _uiState.value = InspectionFormState.Loading
        val inspection = repo.getReportById(inspectionId).firstOrNull()
        if (inspection == null) {
            _uiState.value = InspectionFormState.Error("Failed to get Inspection")
        } else {
            runCatching { attachmentRepo.hydrateFromRemoteIfEmpty(inspectionId) }
            val current = attachmentRepo.getAttachments(inspectionId).firstOrNull().orEmpty()
            originalAttachmentIds = current.map { it.id }.toSet()

            initForEdit(inspection)

            formData.value = formData.value.copy(attachments = toUiList(current, linkResolver))
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

    fun addAttachments(picked: List<Uri>) {
        val id = formData.value.editingId ?: draftId
        val current = formData.value.attachments.toMutableList()

        picked.forEach { uri ->
            current += toUi(
                InspectionAttachment(
                    id = UUID.randomUUID().toString(), // temp client id
                    inspectionId = id,
                    localUri = uri.toString(),         // showable offline via Coil
                    remoteKey = null,                  // not uploaded yet
                    mimeType = "image/*",              // optional placeholder
                    sizeBytes = 0L,                    // unknown at this layer
                    status = com.tiramisu.groupapplication.domain.enums.AttachmentStatus.PENDING_UPLOAD,
                    lastError = null
                ), linkResolver
            )
        }

        formData.value = formData.value.copy(attachments = current)
    }

    fun removeAttachment(attachmentId: String) {
        val current = formData.value.attachments.filterNot { it.id == attachmentId }
        formData.value = formData.value.copy(attachments = current)
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
                    id = data.editingId!!,
                    createdAt = Instant.fromEpochMilliseconds(data.createdAtMillis),
                    type = data.type.trim(),
                    title = data.title.trim(),
                    notes = data.notes.trim(),
                    score = data.score.trim(),
                    outcome = data.outcome.trim(),
                    status = InspectionStatus.PENDING_REVIEW,
                    lat = data.lat,
                    lng = data.lng,
                    pendingSync = false
                )

                repo.upsertLocal(inspection)

                val draft = formData.value.attachments
                val draftIds = draft.map { it.id }.toSet()

                val toAdd = draft.filter { it.id !in originalAttachmentIds }
                val toDelete = originalAttachmentIds - draftIds

                toAdd.forEach { att ->
                    // we only stored localUri in form; repo will copy & upload later
                    val local = att.raw.localUri ?: return@forEach
                    runCatching { attachmentRepo.addAttachment(data.editingId, Uri.parse(local)) }
                        .onFailure { setGeneralError(it.message) }
                }

                toDelete.forEach { attId ->
                    runCatching { attachmentRepo.deleteAttachment(attId) }
                        .onFailure { setGeneralError(it.message) }
                }

                // 4) Update the original snapshot so repeated Saves won’t re-delete/re-add
                originalAttachmentIds = draftIds

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

    // ----- Attachments actions -----

    /** Picked URIs from system picker/camera go here. */
//    fun addAttachments(picked: List<Uri>) = viewModelScope.launch {
//        val id = formData.value.editingId!!
//        picked.forEach { uri ->
//            runCatching { attachmentRepo.addAttachment(id, uri) }
//                .onFailure { setGeneralError(it.message) }
//        }
//    }
//
//    fun removeAttachment(attachmentId: String) = viewModelScope.launch {
//        runCatching { attachmentRepo.deleteAttachment(attachmentId) }
//            .onFailure { setGeneralError(it.message) }
//    }
//
//    fun retryAttachment(attachmentId: String) = viewModelScope.launch {
//        runCatching { attachmentRepo.retryUpload(attachmentId) }
//            .onFailure { setGeneralError(it.message) }
//    }


    // ----- Validation helpers -----

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