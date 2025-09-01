package com.tiramisu.groupapplication.domain.repository

import com.google.firebase.firestore.ListenerRegistration
import com.tiramisu.groupapplication.domain.model.Inspection
import kotlinx.coroutines.flow.Flow

interface InspectionRepository {
    fun getAllReports(): Flow<List<Inspection>>
    fun observeRealtime(): ListenerRegistration

    suspend fun upsert(inspection: Inspection): String
    suspend fun delete(id: String): Void?
}