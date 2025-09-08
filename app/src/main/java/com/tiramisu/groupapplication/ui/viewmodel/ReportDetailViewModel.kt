package com.tiramisu.groupapplication.ui.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiramisu.groupapplication.data.local.InspectionDao
import com.tiramisu.groupapplication.domain.enums.InspectionStatus
import com.tiramisu.groupapplication.domain.model.Inspection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@HiltViewModel
class ReportDetailViewModel @Inject constructor(
    private val inspectionDao: InspectionDao
) : ViewModel() {

    // UI state for the form fields, now including location and images
    var reportTitle by mutableStateOf("")
    var reportNotes by mutableStateOf("")
    var reportStatus by mutableStateOf(InspectionStatus.PENDING_REVIEW)
    var latitude by mutableStateOf<Double?>(null)
    var longitude by mutableStateOf<Double?>(null)
    var imageUris by mutableStateOf<List<Uri>>(emptyList())
    var type by mutableStateOf("")
    var score by mutableStateOf("")
    var outcome by mutableStateOf("")


    @OptIn(ExperimentalTime::class)
    fun saveReport() {
        // Basic validation
        if (reportTitle.isBlank()) {
            // In a real app, show an error message to the user
            return
        }

        val newReport = Inspection(
            createdAt = Clock.System.now(),
            type = type,
            title = reportTitle,
            notes = reportNotes,
            score = score,
            outcome = outcome,
            status = reportStatus,
            lat = latitude,
            lng = longitude
        )

        viewModelScope.launch {
            inspectionDao.upsert(newReport)
        }
    }
}