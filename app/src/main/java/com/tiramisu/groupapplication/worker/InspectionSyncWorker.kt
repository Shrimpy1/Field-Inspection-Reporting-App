package com.tiramisu.groupapplication.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.tiramisu.groupapplication.domain.repository.InspectionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class InspectionSyncWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted private val params: WorkerParameters,
    private val repo: InspectionRepository,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = try {
        repo.pushPendingChanges()
        repo.pullRemoteChanges()
        Result.success()
    } catch (e: Exception) {
        Result.failure()
    }
}

fun schedulePeriodicPull(work: WorkManager) {
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    val req = PeriodicWorkRequestBuilder<InspectionSyncWorker>(15, TimeUnit.MINUTES)
        .setConstraints(constraints)
        .build()

    work.enqueueUniquePeriodicWork("inspection-periodic-sync", ExistingPeriodicWorkPolicy.KEEP, req)
}