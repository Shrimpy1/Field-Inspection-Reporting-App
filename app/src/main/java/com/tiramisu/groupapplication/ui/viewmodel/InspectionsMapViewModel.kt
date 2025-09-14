package com.tiramisu.groupapplication.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiramisu.groupapplication.domain.repository.InspectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed class InspectionsLocState {
    object Loading : InspectionsLocState()
    data class Success(val pins: List<InspectionPin>) : InspectionsLocState()
    data class Error(val message: String) : InspectionsLocState()
}

data class InspectionPin(
    val id: String,
    val title: String?,
    val lat: Double,
    val lng: Double
)

@HiltViewModel
class InspectionsMapViewModel @Inject constructor(
    private val repo: InspectionRepository
) : ViewModel() {

    val uiState: StateFlow<InspectionsLocState> =
        repo.getAllReports()
            .map { inspections ->
                val pins = inspections.mapNotNull { ins ->
                    val lat = ins.lat      // <-- replace if your field is different
                    val lng = ins.lng      // <--
                    if (lat == null || lng == null) return@mapNotNull null
                    InspectionPin(
                        id = ins.id,       // <--
                        title = ins.title, // <--
                        lat = lat,
                        lng = lng
                    )
                }
                InspectionsLocState.Success(pins) as InspectionsLocState
            }
            .onStart { emit(InspectionsLocState.Loading) }
            .catch { emit(InspectionsLocState.Error(it.message ?: "Unknown error")) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = InspectionsLocState.Loading
            )
}