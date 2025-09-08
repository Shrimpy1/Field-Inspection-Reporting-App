package com.tiramisu.groupapplication.domain.repository

import com.tiramisu.groupapplication.domain.model.Inspection
import kotlinx.coroutines.flow.Flow

interface InspectionRepository {
    fun getAllReports(): Flow<List<Inspection>>
    fun observeRealtime(): Flow<Unit>
    fun getReportById(reportId: String): Flow<Inspection?>

    suspend fun upsert(inspection: Inspection): String
    suspend fun delete(id: String): Void?
}