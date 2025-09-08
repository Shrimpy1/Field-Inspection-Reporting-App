package com.tiramisu.groupapplication.data.remote.repository

import android.util.Log
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.tiramisu.groupapplication.data.local.InspectionDao
import com.tiramisu.groupapplication.data.remote.dto.InspectionDto
import com.tiramisu.groupapplication.data.remote.dto.toInspection
import com.tiramisu.groupapplication.data.remote.dto.toInspectionDto
import com.tiramisu.groupapplication.di.ApplicationScope
import com.tiramisu.groupapplication.di.InspectionCollection
import com.tiramisu.groupapplication.di.IoDispatcher
import com.tiramisu.groupapplication.domain.model.Inspection
import com.tiramisu.groupapplication.domain.repository.InspectionRepository
import com.tiramisu.groupapplication.domain.repository.SyncState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@Singleton
class InspectionRepositoryImpl @Inject constructor(
    private val dao: InspectionDao,
    @InspectionCollection private val inspectionCol: CollectionReference,
    @ApplicationScope private val appScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : InspectionRepository {
    @OptIn(ExperimentalAtomicApi::class)
    private val started = AtomicBoolean(false)
    private var reg: ListenerRegistration? = null

    private val _syncState = MutableStateFlow(SyncState())
    override val syncState: StateFlow<SyncState> = _syncState

    /*override fun observeRealtime(): Flow<Unit> = callbackFlow {
        val job = SupervisorJob()

        val reg = inspectionCol.addSnapshotListener { snapshot, e ->
            Log.i("FirestoreData", "snapshot: ${snapshot?.documentChanges}")
            if (e != null) {
                close(e)
                return@addSnapshotListener
            }
            if (snapshot == null) return@addSnapshotListener

            appScope.launch {
                val upserts =
                    snapshot.documentChanges.filter { it.type != DocumentChange.Type.REMOVED }
                        .mapNotNull { doc ->
                            doc.document.toObject(InspectionDto::class.java)
                                .toInspection(doc.document.id)
                        }
                Log.i("FirestoreData", "upserts: $upserts")
                if (upserts.isNotEmpty()) {
                    dao.upsertAll(upserts)
                }

                val removed = snapshot.documentChanges
                    .filter { it.type == DocumentChange.Type.REMOVED }
                    .map { it.document.id }
                Log.i("FirestoreData", "removed: $removed")

                if (removed.isNotEmpty()) {
                    dao.deleteByIds(removed)
                }

                trySend(Unit).isSuccess
            }
        }
        awaitClose {
            reg.remove()
            job.cancel()
        }
    }*/

    @OptIn(ExperimentalAtomicApi::class)
    override fun startRealtime() {
        if (!started.compareAndSet(expectedValue = false, newValue = true)) return

        _syncState.value = SyncState(syncing = true, error = null)
        reg = inspectionCol.addSnapshotListener { snapshot, e ->
            Log.i("FirestoreData", "snapshot: ${snapshot?.documentChanges}")
            if (e != null || snapshot == null) {
                _syncState.value = SyncState(syncing = false, error = e)
                return@addSnapshotListener
            }

            appScope.launch {
                val upserts =
                    snapshot.documentChanges.filter { it.type != DocumentChange.Type.REMOVED }
                        .mapNotNull { doc ->
                            doc.document.toObject(InspectionDto::class.java)
                                .toInspection(doc.document.id)
                        }
                Log.i("FirestoreData", "upserts: $upserts")
                if (upserts.isNotEmpty()) dao.upsertAll(upserts)

                val removed = snapshot.documentChanges
                    .filter { it.type == DocumentChange.Type.REMOVED }
                    .map { it.document.id }
                Log.i("FirestoreData", "removed: $removed")

                if (removed.isNotEmpty()) dao.deleteByIds(removed)

                _syncState.value = SyncState(syncing = false, error = null)
            }
        }
    }

    override fun getAllReports(): Flow<List<Inspection>> = dao.getAllReports()

    override fun getReportById(reportId: String): Flow<Inspection?> = dao.getReportById(reportId)

    override suspend fun upsert(inspection: Inspection): String = withContext(ioDispatcher) {
        val doc = if (inspection.id.isBlank()) inspectionCol.document() else inspectionCol.document(
            inspection.id
        )

        val dto = inspection.toInspectionDto()
        doc.set(dto, SetOptions.merge()).await()
        doc.id
    }

    override suspend fun delete(id: String): Void? = withContext(ioDispatcher) {
        inspectionCol.document(id).delete().await()
    }

    override suspend fun syncOnce(forceServer: Boolean) = withContext(ioDispatcher) {
        // 1) Fetch once
        val snap = if (forceServer)
            inspectionCol.get(Source.SERVER)
                .await()   // force network; throws on rules/network issues
        else
            inspectionCol.get().await()                // allows cache fallback

        // 2) Map to domain
        val remoteInspections = snap.documents.mapNotNull { d ->
            d.toObject(InspectionDto::class.java)?.toInspection(d.id)
        }

        // 3) Upsert all fetched docs
        if (remoteInspections.isNotEmpty()) {
            dao.upsertAll(remoteInspections)
        }

        // 4) Remove locals that no longer exist remotely
        val remoteIds = remoteInspections.map { it.id }.toSet()
        val localIds = dao.getAllIds().toSet()
        val toDelete = (localIds - remoteIds).toList()
        if (toDelete.isNotEmpty()) {
            dao.deleteByIds(toDelete)
        }
    }
}