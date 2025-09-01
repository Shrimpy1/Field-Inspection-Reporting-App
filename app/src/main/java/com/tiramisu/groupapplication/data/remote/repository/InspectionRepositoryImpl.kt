package com.tiramisu.groupapplication.data.remote.repository

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.tiramisu.groupapplication.data.local.InspectionDao
import com.tiramisu.groupapplication.data.remote.dto.InspectionDto
import com.tiramisu.groupapplication.data.remote.dto.toInspection
import com.tiramisu.groupapplication.di.IoDispatcher
import com.tiramisu.groupapplication.domain.model.Inspection
import com.tiramisu.groupapplication.domain.repository.InspectionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

class InspectionRepositoryImpl @Inject constructor(
    private val dao: InspectionDao,
    private val inspectionCol: CollectionReference,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : InspectionRepository {
    override fun getAllReports(): Flow<List<Inspection>> = dao.getAllReports()

    override fun observeRealtime(): ListenerRegistration {
        return inspectionCol.addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null) {
                return@addSnapshotListener
            }
            GlobalScope.launch(ioDispatcher) {
                val upserts = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(InspectionDto::class.java)?.toInspection(doc.id)
                }
                dao.upsertAll(upserts)

                val removed = snapshot.documentChanges
                    .filter { it.type == DocumentChange.Type.REMOVED }
                    .map { it.document.id }

                if (removed.isNotEmpty()) {
                    dao.deleteByIds(removed)
                }
            }
        }
    }

    override suspend fun upsert(inspection: Inspection): String = withContext(ioDispatcher) {
        val doc = if (inspection.id.isBlank()) inspectionCol.document() else inspectionCol.document(
            inspection.id
        )
        doc.set(inspection, SetOptions.merge()).await()
        doc.id
    }

    override suspend fun delete(id: String): Void? = withContext(ioDispatcher) {
        inspectionCol.document(id).delete().await()
    }
}