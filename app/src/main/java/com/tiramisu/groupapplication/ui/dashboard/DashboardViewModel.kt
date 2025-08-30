package com.tiramisu.groupapplication.ui.dashboard

import androidx.lifecycle.ViewModel
import com.tiramisu.groupapplication.data.local.InspectionDao
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val inspectionDao: InspectionDao
) : ViewModel() {
    val reports = inspectionDao.getAllReports()
}