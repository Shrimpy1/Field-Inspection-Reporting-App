package com.tiramisu.groupapplication.domain.repository

import android.net.Uri
import com.tiramisu.groupapplication.domain.model.InspectionLog
import kotlinx.coroutines.flow.Flow

interface LogRepository {
    suspend fun log(
        inspectionId: String,
        info: String = "",
    ): Any

    suspend fun filter(days: Int = 7)
    suspend fun pushPendingChanges()
    suspend fun exportToCsv(
        targetUri: Uri,
        inspectionId: String? = null,
        since: Long? = null,
        until: Long? = null
    )

    fun getAllLogs(): Flow<List<InspectionLog>>
}