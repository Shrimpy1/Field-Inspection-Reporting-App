package com.tiramisu.groupapplication.domain.repository

import androidx.datastore.preferences.core.Preferences
import com.tiramisu.groupapplication.domain.model.Inspection
import com.tiramisu.groupapplication.domain.model.SyncState
import kotlinx.coroutines.flow.Flow

interface InspectionRepository {
    /**
     * A flow that emits the current state of data synchronization with the remote server.
     */
    val syncState: Flow<SyncState>

    /**
     * Gets a continuous flow of all inspection reports from the local database.
     */
    fun getAllReports(): Flow<List<Inspection>>

    /**
     * Gets a continuous flow of a single inspection report by its unique ID.
     */
    fun getReportById(id: String): Flow<Inspection?>

    /**
     * Starts the real-time listener to keep the local database in sync with Firestore.
     */
    fun startRealtimeSync()

    /**
     * Triggers a one-time manual sync to fetch all data from Firestore.
     */
//    suspend fun syncOnce()

    /**
     * Creates or updates an inspection report on the server and in the local cache.
     * @return The unique ID of the saved inspection.
     */
    suspend fun upsert(inspection: Inspection): Void?

    /**
     * Deletes an inspection report from the server and local cache.
     */
    suspend fun delete(id: String): Void?

    /**
     * Updates an existing inspection, typically used for supervisor reviews.
     */
    suspend fun updateInspection(inspection: Inspection)

    suspend fun upsertLocal(inspection: Inspection): Any
    suspend fun deleteLocal(id: String): Any
    suspend fun pushPendingChanges()
    suspend fun pullRemoteChanges(): Preferences
}