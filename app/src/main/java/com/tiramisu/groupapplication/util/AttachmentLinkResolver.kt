package com.tiramisu.groupapplication.util

import com.tiramisu.groupapplication.di.AttachmentBucket
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttachmentLinkResolver @Inject constructor(
    private val supabase: SupabaseClient,
    @AttachmentBucket private val bucket: String
) {
    fun publicUrl(remoteKey: String): String = supabase.storage.from(bucket).publicUrl(remoteKey)
}