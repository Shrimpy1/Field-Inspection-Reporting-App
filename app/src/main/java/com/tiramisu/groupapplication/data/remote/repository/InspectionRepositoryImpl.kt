package com.tiramisu.groupapplication.data.remote.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.toObjects
import com.tiramisu.groupapplication.data.local.InspectionDao
import com.tiramisu.groupapplication.data.local.SyncMeta
import com.tiramisu.groupapplication.data.local.SyncMetaDao
import com.tiramisu.groupapplication.data.remote.dto.InspectionDto
import com.tiramisu.groupapplication.data.remote.dto.toInspection
import com.tiramisu.groupapplication.data.remote.dto.toInspectionDto
import com.tiramisu.groupapplication.di.InspectionCollection
import com.tiramisu.groupapplication.di.IoDispatcher
import com.tiramisu.groupapplication.domain.model.Inspection
import com.tiramisu.groupapplication.domain.model.SyncState
import com.tiramisu.groupapplication.domain.repository.InspectionRepository
import com.tiramisu.groupapplication.worker.InspectionSyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock

@Singleton
class InspectionRepositoryImpl @Inject constructor(
    private val inspectionDao: InspectionDao,
    private val syncMetaDao: SyncMetaDao,
    private val work: WorkManager,
    @InspectionCollection private val inspectionCol: CollectionReference,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationContext private val context: Context,
) : InspectionRepository {
    private val _syncState = MutableStateFlow(SyncState())
    override val syncState: Flow<SyncState> = _syncState.asStateFlow()

    private var listener: ListenerRegistration? = null

    @Volatile
    private var started = false
    private fun metaKey(): String = "inspections:${inspectionCol.path}"

    override fun getAllReports(): Flow<List<Inspection>> = inspectionDao.getAllReports()

    override fun getReportById(id: String): Flow<Inspection?> = inspectionDao.getReportById(id)

    // For Server only. For normal usage, use upsertLocal instead
    override suspend fun upsert(inspection: Inspection): Void? = withContext(ioDispatcher) {
        require(inspection.id.isNotBlank()) { "Inspection.id must be set before remote upsert" }
        inspectionCol.document(inspection.id)
            .set(inspection.toInspectionDto(), SetOptions.merge()).await()
    }

    // For Server only. For normal usage, use deleteLocal instead
    override suspend fun delete(id: String): Void? = withContext(ioDispatcher) {
        inspectionCol.document(id).delete().await()
    }

    override suspend fun upsertLocal(inspection: Inspection) = withContext(ioDispatcher) {
        inspectionDao.upsert(
            inspection.copy(
                updatedAt = now(),
                pendingSync = true
            )
        )
        sync()
    }

    override suspend fun deleteLocal(id: String) = withContext(ioDispatcher) {
        inspectionDao.delete(id)
        sync()
    }

    override suspend fun pushPendingChanges() = withContext(ioDispatcher) {
        val pending = inspectionDao.getPending()
        if (pending.isEmpty()) return@withContext
        val pushedIds = mutableListOf<String>()

        val batch = inspectionCol.firestore.batch()
        for (it in pending) {
            val doc = inspectionCol.document(it.id)
            batch.set(doc, it.toInspectionDto(), SetOptions.merge())
            pushedIds += doc.id.ifBlank { it.id }
        }
        batch.commit().await()

        inspectionDao.markSynced(pushedIds)
    }

    override suspend fun pullRemoteChanges() = withContext(ioDispatcher) {
        val key = metaKey()
        val last = syncMetaDao.getLast(key) ?: 0L
        Log.d("InspectionRepo", "last pull: $last")

        val query = if (last > 0) {
            inspectionCol.whereGreaterThan(
                "updatedAt",
                Timestamp(last / 1_000, (last % 1_000).toInt() * 1_000_000)
            )
        } else inspectionCol

        val snap = query.get().await()
        val dtos = snap.toObjects<InspectionDto>()
        Log.d("InspectionRepo", "pullRemoteChanges: ${dtos.size}")

        if (dtos.isEmpty()) {
            // Important: do NOT advance watermark on empty fetch
            return@withContext
        }

        val inspections = dtos.mapIndexed { index, dto ->
            dto.toInspection(snap.documents[index].id).copy(pendingSync = false)
        }

        inspectionDao.upsertAll(inspections)

        // Advance watermark to the max remote updatedAt we actually wrote
        val maxEpoch = inspections.maxOf { it.updatedAt.toEpochMilliseconds() }
        syncMetaDao.upsert(SyncMeta(key = key, lastEpoch = maxEpoch))
        Log.d("InspectionRepo", "pullRemoteChanges(): newLast=$maxEpoch key=$key")
    }

    override fun startRealtimeSync() {
        if (started) return
        started = true
        listener?.remove()
        listener = inspectionCol.addSnapshotListener { _, e ->
            if (e == null) CoroutineScope(ioDispatcher).launch { pullRemoteChanges() }
            else _syncState.value = _syncState.value.copy(error = e)
        }
    }

    override fun stopRealtimeSync() {
        listener?.remove()
        listener = null
        started = false
    }

    private suspend fun sync() {
        if (isOnline()) {
            runCatching { pushPendingChanges() }
                .onFailure { scheduleImmediateSync() }
            runCatching { pullRemoteChanges() }
        } else {
            scheduleImmediateSync()
        }
    }

    private fun scheduleImmediateSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val req = OneTimeWorkRequestBuilder<InspectionSyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        work.enqueueUniqueWork("inspection-push", ExistingWorkPolicy.APPEND_OR_REPLACE, req)
    }

    private fun now() = Clock.System.now()
    private fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val nw = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(nw) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun resetInspectionWatermark() = withContext(ioDispatcher) {
        syncMetaDao.upsert(SyncMeta(key = metaKey(), lastEpoch = 0L))
    }

}