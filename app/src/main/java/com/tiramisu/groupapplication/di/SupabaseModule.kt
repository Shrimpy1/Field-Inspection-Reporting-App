package com.tiramisu.groupapplication.di

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import com.tiramisu.groupapplication.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.resumable.MemoryResumableCache
import io.github.jan.supabase.storage.storage
import okhttp3.Cache
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {
    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient =
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_KEY
        ) {
            install(Storage) {
                resumable {
                    cache = MemoryResumableCache()
                }
            }
        }

    @Provides
    @AttachmentBucket
    fun provideAttachmentBucketName(): String = "inspections"

    @Provides
    fun providePublicUrl(
        supabase: SupabaseClient,
        @AttachmentBucket bucket: String
    ): (String) -> String = { key ->
        supabase.storage.from(bucket).publicUrl(key)
    }

    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context
    ): ImageLoader =
        ImageLoader.Builder(context)
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("coil_disk_cache"))
                    .maxSizeBytes(256L * 1024 * 1024)
                    .build()
            }
            .okHttpClient {
                OkHttpClient.Builder()
                    .cache(Cache(context.cacheDir.resolve("okhttp_http_cache"), 50L * 1024 * 1024))
                    .build()
            }
            .build()
}