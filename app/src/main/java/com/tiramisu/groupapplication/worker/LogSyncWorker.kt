package com.tiramisu.groupapplication.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tiramisu.groupapplication.domain.repository.LogRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class LogSyncWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted private val params: WorkerParameters,
    private val repo: LogRepository,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = try {
        repo.pushPendingChanges()
        Result.success()
    } catch (e: Exception) {
        Result.failure()
    }
}