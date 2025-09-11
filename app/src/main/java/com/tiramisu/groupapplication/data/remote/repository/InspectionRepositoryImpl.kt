package com.tiramisu.groupapplication.data.remote.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.Timestamp
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
import com.tiramisu.groupapplication.worker.InspectionSyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock

@Singleton
class InspectionRepositoryImpl @Inject constructor(
    private val dao: InspectionDao,
    private val work: WorkManager,
    private val prefs: DataStore<Preferences>,
    @InspectionCollection private val inspectionCol: CollectionReference,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationContext private val context: Context
) : InspectionRepository {
    private val scope = CoroutineScope(ioDispatcher + SupervisorJob())
    private val _syncState = MutableStateFlow(SyncState())
    override val syncState: Flow<SyncState> = _syncState.asStateFlow()

    override fun getAllReports(): Flow<List<Inspection>> = dao.getAllReports()

    override fun getReportById(id: String): Flow<Inspection?> = dao.getReportById(id)

    override suspend fun upsert(inspection: Inspection): Void? = withContext(ioDispatcher) {
        require(inspection.id.isNotBlank()) { "Inspection.id must be set before remote upsert" }
        inspectionCol.document(inspection.id)
            .set(inspection.toInspectionDto(), SetOptions.merge()).await()
    }

    override suspend fun delete(id: String): Void? = withContext(ioDispatcher) {
        inspectionCol.document(id).delete().await()
    }

    override suspend fun updateInspection(inspection: Inspection) {
        withContext(ioDispatcher) {
            inspectionCol.document(inspection.id)
                .set(inspection.toInspectionDto(), SetOptions.merge()).await()
        }
    }

    override suspend fun upsertLocal(inspection: Inspection) = withContext(ioDispatcher) {
        dao.upsert(
            inspection.copy(
                updatedAt = now(),
                pendingSync = true
            )
        )
        sync()
    }

    override suspend fun deleteLocal(id: String) = withContext(ioDispatcher) {
        dao.delete(id)
        sync()
    }

    override suspend fun pushPendingChanges() = withContext(ioDispatcher) {
        val pending = dao.getPending()
        if (pending.isEmpty()) return@withContext
        val pushedIds = mutableListOf<String>()

        val batch = inspectionCol.firestore.batch()
        for (it in pending) {
            val doc = inspectionCol.document(it.id)
            batch.set(doc, it.toInspectionDto(), SetOptions.merge())
            pushedIds += doc.id.ifBlank { it.id }
        }
        batch.commit().await()

        dao.markSynced(pushedIds)
    }

    override suspend fun pullRemoteChanges() = withContext(ioDispatcher) {
        val last = prefs.data.first()[KEY_LAST_PULL_EPOCH] ?: 0L
        val query = if (last > 0) {
            inspectionCol.whereGreaterThan(
                "updatedAt",
                Timestamp(last / 1_000, (last % 1_000).toInt() * 1_000_000)
            )
        } else inspectionCol

        val snap = query.get().await()
        val dtos = snap.toObjects<InspectionDto>()
        val inspections = dtos.mapIndexed { index, dto ->
            dto.toInspection(snap.documents[index].id).copy(pendingSync = false)
        }

        dao.upsertAll(inspections)

        prefs.updateData {
            it.toMutablePreferences().apply {
                this[KEY_LAST_PULL_EPOCH] = nowEpochMillis()
            }
        }
    }

//    override fun startRealtimeSync() {
//        inspectionCol.addSnapshotListener { snapshot, e ->
//            if (e != null) {
//                _syncState.value = _syncState.value.copy(error = e)
//                return@addSnapshotListener
//            }
//            if (snapshot == null) return@addSnapshotListener
//            scope.launch {
//                syncOnce()
//            }
//        }
//    }

    override fun startRealtimeSync() {
        inspectionCol.addSnapshotListener { _, e ->
            if (e == null) CoroutineScope(ioDispatcher).launch { pullRemoteChanges() }
        }
    }

//    override suspend fun syncOnce() = withContext(ioDispatcher) {
//        _syncState.value = SyncState(syncing = true, error = null)
//        try {
//            val snapshot = inspectionCol.get().await()
//            val inspections = snapshot.toObjects<InspectionDto>().mapIndexed { index, dto ->
//                dto.toInspection(snapshot.documents[index].id)
//            }
//            dao.upsertAll(inspections)
//            _syncState.value = SyncState(syncing = false, error = null)
//        } catch (e: Exception) {
//            _syncState.value = SyncState(syncing = false, error = e)
//        }
//    }

    private suspend fun sync() {
        if (isOnline(context = context)) {
            runCatching { pushPendingChanges() }
                .onFailure { scheduleImmediateSync() }
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
    private fun nowEpochMillis() = now().toEpochMilliseconds()
    private val KEY_LAST_PULL_EPOCH = longPreferencesKey("inspections_last_pull_epoch")
    private fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val nw = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(nw) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}