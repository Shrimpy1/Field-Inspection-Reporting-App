package com.tiramisu.groupapplication.data.remote.repository

import android.content.Context
import android.net.Uri
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
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
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
    @OptIn(ExperimentalAtomicApi::class)
    private var listening = AtomicBoolean(false)

    override fun getAllLogs() = dao.getAllLogs().also {
        // start remote → local mirror once
        remoteListenOnce()
    }

    @OptIn(ExperimentalAtomicApi::class)
    private fun remoteListenOnce() {
        if (!listening.compareAndSet(false, true)) return

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

        remote.pushBatch(pending)

        dao.markSynced(pending.map { it.id })
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

    override suspend fun exportToCsv(
        targetUri: Uri,
        inspectionId: String?,
        since: Long?,
        until: Long?
    ): Unit = withContext(ioDispatcher) {
        context.contentResolver.openOutputStream(targetUri)?.bufferedWriter(Charsets.UTF_8)
            .use { out ->
                // UTF-8 BOM helps Excel detect encoding
                out?.write("\uFEFF")
                out?.write("id,createdAt,createdAt_iso,inspectionId,info\n")

                // ISO timestamp in UTC
                val sdf =
                    java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
                        .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }

                dao.exportCursor(inspectionId, since, until).use { c ->
                    val idxId = c.getColumnIndexOrThrow("id")
                    val idxCreatedAt = c.getColumnIndexOrThrow("createdAt")
                    val idxInspectionId = c.getColumnIndexOrThrow("inspectionId")
                    val idxInfo = c.getColumnIndexOrThrow("info")

                    while (c.moveToNext()) {
                        val id = c.getString(idxId) ?: ""
                        val createdAtMs = c.getLong(idxCreatedAt)
                        val createdAtIso = sdf.format(java.util.Date(createdAtMs))
                        val inspectionId = c.getString(idxInspectionId) ?: ""
                        val info = c.getString(idxInfo) ?: ""

                        out?.write(
                            csvRow(
                                listOf(
                                    id,
                                    createdAtMs.toString(),
                                    createdAtIso,
                                    inspectionId,
                                    info,
                                )
                            )
                        )
                        out?.write("\n")
                    }
                }
                out?.flush()
            }
    }

    private fun csvRow(fields: List<String>) =
        fields.joinToString(",") { escapeCsv(it) }

    private fun escapeCsv(s: String): String {
        val needsQuote = s.indexOfFirst { it == ',' || it == '"' || it == '\n' || it == '\r' } >= 0
        val doubled = s.replace("\"", "\"\"")
        return if (needsQuote) "\"$doubled\"" else doubled
    }
}