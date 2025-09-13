package com.tiramisu.groupapplication.data.remote.repository

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.tiramisu.groupapplication.data.local.LogDao
import com.tiramisu.groupapplication.data.remote.log.LogRemoteSink
import com.tiramisu.groupapplication.di.ApplicationScope
import com.tiramisu.groupapplication.di.Firestore
import com.tiramisu.groupapplication.di.IoDispatcher
import com.tiramisu.groupapplication.domain.model.InspectionLog
import com.tiramisu.groupapplication.domain.repository.LogRepository
import com.tiramisu.groupapplication.util.ext.isOnline
import com.tiramisu.groupapplication.worker.InspectionSyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Instant

@Singleton
class LogRepositoryImpl @Inject constructor(
    private val dao: LogDao,
    private val work: WorkManager,
    @Firestore private val remote: LogRemoteSink,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope private val appScope: CoroutineScope,
    @ApplicationContext private val context: Context,
) : LogRepository {
    private var listening: Boolean
        get() {
            return this.listening
        }
        set(value) {
            this.listening = value
        }

    fun getAllLogs() = dao.getAllLogs().also {
        // start remote → local mirror once
        remoteListenOnce()
    }

    private fun remoteListenOnce() {
        if (listening) return

        listening = true

        remote.listen().onEach { remoteList ->
            withContext(ioDispatcher) { dao.insertAll(remoteList) }
        }.launchIn(appScope)
    }

    override suspend fun log(
        inspectionId: String,
        info: String,
    ) = withContext(ioDispatcher) {
        val log = InspectionLog(
            inspectionId = inspectionId,
            info = info,
            pendingSync = true,
        )
        dao.insert(log)

        if (context.isOnline()) {
            runCatching { pushPendingChanges() }
                .onFailure { schedulePush() }
        } else {
            schedulePush()
        }
    }

    override suspend fun filter(days: Int) = withContext(ioDispatcher) {
        dao.filterOlderThan(Instant.fromEpochMilliseconds(System.currentTimeMillis() - days * 86_400_000L))
    }

    override suspend fun pushPendingChanges() = withContext(ioDispatcher) {
        val pending = dao.getPending()
        if (pending.isEmpty()) return@withContext
        val pushedIds = mutableListOf<String>()

        remote.pushBatch(pending)

        dao.markSynced(pushedIds)
    }

    private fun schedulePush() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val req = OneTimeWorkRequestBuilder<InspectionSyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        work.enqueueUniqueWork("inspection-push", ExistingWorkPolicy.APPEND_OR_REPLACE, req)
    }
}