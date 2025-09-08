package com.tiramisu.groupapplication.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiramisu.groupapplication.domain.repository.InspectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed class ReportLocState {
    data object Loading : ReportLocState()
    data class Success(val lat: Double, val lng: Double) : ReportLocState()
    data class Error(val message: String) : ReportLocState()
}

@HiltViewModel
class InspectionLocationViewModel @Inject constructor(
    private val inspectionRepo: InspectionRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ReportLocState> =
        savedStateHandle.getStateFlow<String?>("inspectionId", null).flatMapLatest { reportId ->
            if (reportId.isNullOrBlank()) {
                flowOf(ReportLocState.Error("Missing report id"))
            } else {
                inspectionRepo.getReportById(reportId)
                    .map { report ->
                        when (report) {
                            null -> ReportLocState.Error("Report not found")
                            else -> {
                                val lat = report.lat
                                val lng = report.lng
                                if (lat == null || lng == null)
                                    ReportLocState.Error("Location not set")
                                else
                                    ReportLocState.Success(lat, lng)
                            }
                        }
                    }
            }
        }
            .onStart { emit(ReportLocState.Loading) }
            .catch { emit(ReportLocState.Error(it.message ?: "Error loading location")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportLocState.Loading)
}