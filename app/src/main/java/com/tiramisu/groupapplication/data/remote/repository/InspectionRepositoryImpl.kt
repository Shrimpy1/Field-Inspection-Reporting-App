package com.tiramisu.groupapplication.data.remote.repository

import android.util.Log
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.SetOptions
import com.tiramisu.groupapplication.data.local.InspectionDao
import com.tiramisu.groupapplication.data.remote.dto.InspectionDto
import com.tiramisu.groupapplication.data.remote.dto.toInspection
import com.tiramisu.groupapplication.data.remote.dto.toInspectionDto
import com.tiramisu.groupapplication.di.InspectionCollection
import com.tiramisu.groupapplication.di.IoDispatcher
import com.tiramisu.groupapplication.domain.model.Inspection
import com.tiramisu.groupapplication.domain.repository.InspectionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

class InspectionRepositoryImpl @Inject constructor(
    private val dao: InspectionDao,
    @InspectionCollection private val inspectionCol: CollectionReference,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : InspectionRepository {
    override fun getAllReports(): Flow<List<Inspection>> = dao.getAllReports()

    override fun observeRealtime(): Flow<Unit> = callbackFlow {
        val job = SupervisorJob()
        val scope = CoroutineScope(ioDispatcher + job)

        val reg = inspectionCol.addSnapshotListener { snapshot, e ->
            Log.i("FirestoreData", "snapshot: ${snapshot?.documentChanges}")
            if (e != null) {
                close(e)
                return@addSnapshotListener
            }
            if (snapshot == null) return@addSnapshotListener

            scope.launch {
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
    }

    override fun getReportById(reportId: String): Flow<Inspection?> = runBlocking(ioDispatcher) {
        dao.getReportById(reportId)
    }

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
}