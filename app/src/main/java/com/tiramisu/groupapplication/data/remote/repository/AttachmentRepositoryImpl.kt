package com.tiramisu.groupapplication.data.remote.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.tiramisu.groupapplication.data.local.InspectionAttachmentDao
import com.tiramisu.groupapplication.di.AttachmentBucket
import com.tiramisu.groupapplication.di.IoDispatcher
import com.tiramisu.groupapplication.domain.enums.AttachmentStatus
import com.tiramisu.groupapplication.domain.model.InspectionAttachment
import com.tiramisu.groupapplication.domain.repository.AttachmentRepository
import com.tiramisu.groupapplication.util.ext.isOnline
import com.tiramisu.groupapplication.util.ext.queryMime
import com.tiramisu.groupapplication.util.ext.querySize
import com.tiramisu.groupapplication.util.image.extensionForMime
import com.tiramisu.groupapplication.worker.DeleteAttachmentWorker
import com.tiramisu.groupapplication.worker.UploadAttachmentWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttachmentRepositoryImpl @Inject constructor(
    private val attachmentDao: InspectionAttachmentDao,
    private val work: WorkManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationContext private val context: Context,
    private val supabase: SupabaseClient,
    @AttachmentBucket private val bucket: String
) : AttachmentRepository {
    override fun getAttachments(inspectionId: String): Flow<List<InspectionAttachment>> =
        attachmentDao.getForInspection(inspectionId)

    override suspend fun addAttachment(inspectionId: String, uri: Uri) = withContext(ioDispatcher) {
        val local = persistOrCopy(uri)

        val id = UUID.randomUUID().toString()
        val mime = context.queryMime(local) ?: "image/jpeg"
        val size = context.querySize(local) ?: 0L

        attachmentDao.upsert(
            InspectionAttachment(
                id = id,
                inspectionId = inspectionId,
                localUri = local.toString(),
                remoteKey = null,
                mimeType = mime,
                sizeBytes = size,
                status = AttachmentStatus.PENDING_UPLOAD,
            )
        )

        if (context.isOnline()) {
            runCatching { uploadOne(id) }.onFailure {
                scheduleUploadWorker(id)
            }
        } else {
            scheduleUploadWorker(id)
        }
    }

    override suspend fun uploadOne(attachmentId: String) = withContext(ioDispatcher) {
        val row = attachmentDao.getById(attachmentId) ?: return@withContext
        attachmentDao.updateStatus(row.id, AttachmentStatus.UPLOADING)

        val key = "${row.inspectionId}/${row.id}${extensionForMime(row.mimeType)}"
        val bytes =
            context.contentResolver.openInputStream(row.localUri!!.toUri())!!.use { it.readBytes() }

        Log.d("AttachmentRepo", "Uploading $key on $bucket bucket with ${bytes.size} bytes")
        supabase.storage.from("inspections").upload(
            path = key,
            data = bytes,
            options = {
                upsert = false
            }
        )

        attachmentDao.markUploaded(row.id, key)
        deleteLocalFile(row.localUri)
    }

    override suspend fun retryUpload(attachmentId: String) = withContext(ioDispatcher) {
        val row = attachmentDao.getById(attachmentId) ?: return@withContext
        attachmentDao.updateStatus(row.id, AttachmentStatus.PENDING_UPLOAD)
        if (context.isOnline()) {
            runCatching { uploadOne(attachmentId) }.onFailure { scheduleUploadWorker(attachmentId) }
        } else {
            scheduleUploadWorker(attachmentId)
        }
    }

    override suspend fun deleteAttachment(attachmentId: String) = withContext(ioDispatcher) {
        val row = attachmentDao.getById(attachmentId) ?: return@withContext

        // Delete local file immediately (optional; or keep until remote deletion succeeds)
        row.localUri?.let { deleteLocalFile(it) }

        if (row.remoteKey.isNullOrBlank()) {
            // never uploaded → just drop the row
            attachmentDao.deleteById(row.id)
            return@withContext
        }

        // Has a remote object → mark and schedule remote delete
        attachmentDao.updateStatus(row.id, AttachmentStatus.DELETE_PENDING)
        if (context.isOnline()) {
            runCatching { deleteOneNow(row.id) }.onFailure { scheduleDeleteWorker(row.id) }
        } else {
            scheduleDeleteWorker(row.id)
        }
    }

    /** Deletes remote object and then removes the DB row. */
    suspend fun deleteOneNow(attachmentId: String) = withContext(ioDispatcher) {
        val row = attachmentDao.getById(attachmentId) ?: return@withContext
        val key = row.remoteKey ?: return@withContext attachmentDao.deleteById(row.id)

        // Supabase-kt delete (remove) accepts a list of keys
        supabase.storage.from(bucket).delete(listOf(key))
        attachmentDao.deleteById(row.id)
    }

    override suspend fun hydrateFromRemoteIfEmpty(inspectionId: String) =
        withContext(ioDispatcher) {
            val cnt = attachmentDao.countForInspection(inspectionId)
            if (cnt > 0) return@withContext
            hydrateFromRemote(inspectionId)
        }

    suspend fun hydrateFromRemote(inspectionId: String) = withContext(ioDispatcher) {
        val objects = supabase.storage.from(bucket).list(prefix = inspectionId)
        val rows = objects.map { obj ->
            InspectionAttachment(
                id = obj.name.substringBefore('.'),  // if you used {uuid}.ext
                inspectionId = inspectionId,
                localUri = null,
                remoteKey = "$inspectionId/${obj.name}",
                mimeType = guessMimeFromName(obj.name),
                sizeBytes = obj.metadata?.size?.toLong() ?: 0L,
                status = AttachmentStatus.SYNCED
            )
        }
        attachmentDao.upsertAll(rows)
    }

    private fun scheduleUploadWorker(attachmentId: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val req = OneTimeWorkRequestBuilder<UploadAttachmentWorker>()
            .setInputData(workDataOf("attachmentId" to attachmentId))
            .setConstraints(constraints)
            .build()

        work.enqueueUniqueWork("upload-attachment-$attachmentId", ExistingWorkPolicy.REPLACE, req)
    }

    private fun scheduleDeleteWorker(attachmentId: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val req = OneTimeWorkRequestBuilder<DeleteAttachmentWorker>()
            .setInputData(workDataOf("attachmentId" to attachmentId))
            .setConstraints(constraints)
            .build()

        work.enqueueUniqueWork("delete-attachment-$attachmentId", ExistingWorkPolicy.REPLACE, req)
    }

    private fun persistOrCopy(uri: Uri): Uri {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {
        }

        val dst = context.filesDir.resolve("attachments/${UUID.randomUUID()}.bin")
            .apply { parentFile?.mkdirs() }

        context.contentResolver.openInputStream(uri).use { src ->
            FileOutputStream(dst).use { dst -> src?.copyTo(dst) }
        }

        return dst.toUri()
    }

    private fun deleteLocalFile(localUri: String) {
        runCatching {
            val f = java.io.File(localUri.toUri().path ?: return)
            if (f.exists()) f.delete()
        }
    }

    private fun guessMimeFromName(name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "png" -> "image/png"
            "webp" -> "image/webp"
            "heic", "heif" -> "image/heic"
            else -> "image/jpeg"
        }
    }
}