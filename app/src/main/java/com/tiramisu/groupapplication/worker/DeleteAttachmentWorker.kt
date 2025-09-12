package com.tiramisu.groupapplication.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tiramisu.groupapplication.domain.repository.AttachmentRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class DeleteAttachmentWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted private val params: WorkerParameters,
    private val repo: AttachmentRepository
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = try {
        val id = inputData.getString("attachmentId") ?: return Result.failure()
        repo.deleteAttachment(id)
        Result.success()
    } catch (e: Exception) {
        Result.retry()
    }
}