package com.tiramisu.groupapplication.ui.reportdetail

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiramisu.groupapplication.data.local.InspectionDao
import com.tiramisu.groupapplication.data.model.InspectionReport
import com.tiramisu.groupapplication.data.model.InspectionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

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


    fun saveReport() {
        // Basic validation
        if (reportTitle.isBlank()) {
            // In a real app, show an error message to the user
            return
        }

        val newReport = InspectionReport(
            title = reportTitle,
            notes = reportNotes,
            status = reportStatus,
            timestamp = System.currentTimeMillis(),
            gpsLatitude = latitude,
            gpsLongitude = longitude
            // Note: Saving imageUris will require a TypeConverter in Room
        )

        viewModelScope.launch {
            inspectionDao.insertOrUpdateReport(newReport)
        }
    }
}