package com.tiramisu.groupapplication.data.remote.repository

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.toObjects
import com.tiramisu.groupapplication.data.local.InspectionDao
import com.tiramisu.groupapplication.data.remote.dto.InspectionDto
import com.tiramisu.groupapplication.data.remote.dto.toInspection
import com.tiramisu.groupapplication.data.remote.dto.toInspectionDto
import com.tiramisu.groupapplication.di.InspectionCollection
import com.tiramisu.groupapplication.di.IoDispatcher
import com.tiramisu.groupapplication.domain.model.Inspection
import com.tiramisu.groupapplication.domain.model.SyncState
import com.tiramisu.groupapplication.domain.repository.InspectionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InspectionRepositoryImpl @Inject constructor(
    private val dao: InspectionDao,
    @InspectionCollection private val inspectionCol: CollectionReference,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : InspectionRepository {

    private val scope = CoroutineScope(ioDispatcher + SupervisorJob())

    private val _syncState = MutableStateFlow(SyncState())
    override val syncState: Flow<SyncState> = _syncState.asStateFlow()

    override fun getAllReports(): Flow<List<Inspection>> = dao.getAllReports()

    override fun getReportById(id: String): Flow<Inspection?> = dao.getReportById(id)

    override fun startRealtimeSync() {
        inspectionCol.addSnapshotListener { snapshot, e ->
            if (e != null) {
                _syncState.value = _syncState.value.copy(error = e)
                return@addSnapshotListener
            }
            if (snapshot == null) return@addSnapshotListener
            scope.launch {
                syncOnce()
            }
        }
    }

    override suspend fun syncOnce() = withContext(ioDispatcher) {
        _syncState.value = SyncState(syncing = true, error = null)
        try {
            val snapshot = inspectionCol.get().await()
            val inspections = snapshot.toObjects<InspectionDto>().mapIndexed { index, dto ->
                dto.toInspection(snapshot.documents[index].id)
            }
            dao.upsertAll(inspections)
            _syncState.value = SyncState(syncing = false, error = null)
        } catch (e: Exception) {
            _syncState.value = SyncState(syncing = false, error = e)
        }
    }

    override suspend fun upsert(inspection: Inspection): String = withContext(ioDispatcher) {
        val doc = if (inspection.id.isBlank()) inspectionCol.document() else inspectionCol.document(inspection.id)
        doc.set(inspection.toInspectionDto(), SetOptions.merge()).await()
        doc.id
    }

    override suspend fun delete(id: String) = withContext(ioDispatcher) {
        inspectionCol.document(id).delete().await()
    }

    override suspend fun updateInspection(inspection: Inspection) {
        withContext(ioDispatcher) {
            inspectionCol.document(inspection.id).set(inspection.toInspectionDto(), SetOptions.merge()).await()
        }
    }
}

