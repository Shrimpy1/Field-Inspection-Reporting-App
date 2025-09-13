package com.tiramisu.groupapplication.data.remote.log

import com.tiramisu.groupapplication.domain.model.InspectionLog
import kotlinx.coroutines.flow.Flow

interface LogRemoteSink {
    fun listen(): Flow<List<InspectionLog>>
    suspend fun push(log: InspectionLog)
    suspend fun pushBatch(batch: List<InspectionLog>)
}