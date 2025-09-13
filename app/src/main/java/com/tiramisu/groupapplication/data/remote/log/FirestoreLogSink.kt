package com.tiramisu.groupapplication.data.remote.log

import com.google.firebase.firestore.CollectionReference
import com.tiramisu.groupapplication.data.remote.dto.InspectionLogDto
import com.tiramisu.groupapplication.data.remote.dto.toDomain
import com.tiramisu.groupapplication.data.remote.dto.toDto
import com.tiramisu.groupapplication.di.LogCollection
import com.tiramisu.groupapplication.domain.model.InspectionLog
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreLogSink @Inject constructor(
    @LogCollection private val col: CollectionReference
) : LogRemoteSink {
    override fun listen(): Flow<List<InspectionLog>> = callbackFlow {
        val q = col.orderBy("createdAt")
        val reg = q.addSnapshotListener { snap, e ->
            if (e != null) {
                close(e); return@addSnapshotListener
            }
            val items = snap?.documents?.map { doc ->
                InspectionLogDto(
                    id = doc.id,
                    inspectionId = doc.getString("inspectionId") ?: "",
                    info = doc.getString("info") ?: "",
                    createdAt = doc.getTimestamp("createdAt")
                ).toDomain()
            }.orEmpty()
            trySend(items)
        }
        awaitClose { reg.remove() }
    }

    override suspend fun push(log: InspectionLog) {
        col.document(log.id).set(log.toDto()).await()
    }

    override suspend fun pushBatch(batch: List<InspectionLog>) {
        val writes = col.firestore.batch()
        batch.forEach { e -> writes.set(col.document(e.id), e.toDto()) }
        writes.commit().await()
    }
}