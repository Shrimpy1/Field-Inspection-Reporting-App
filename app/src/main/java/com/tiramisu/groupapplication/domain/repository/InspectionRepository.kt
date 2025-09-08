package com.tiramisu.groupapplication.domain.repository

import com.tiramisu.groupapplication.domain.model.Inspection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface InspectionRepository {
    val syncState: StateFlow<SyncState>
    fun getAllReports(): Flow<List<Inspection>>
    fun startRealtime()
    fun getReportById(reportId: String): Flow<Inspection?>

    suspend fun upsert(inspection: Inspection): String
    suspend fun delete(id: String): Void?
    suspend fun syncOnce(forceServer: Boolean = true)
}

data class SyncState(val syncing: Boolean = false, val error: Throwable? = null)