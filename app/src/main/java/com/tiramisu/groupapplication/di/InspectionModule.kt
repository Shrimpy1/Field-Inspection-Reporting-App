package com.tiramisu.groupapplication.di

import com.tiramisu.groupapplication.data.remote.log.FirestoreLogSink
import com.tiramisu.groupapplication.data.remote.log.LogRemoteSink
import com.tiramisu.groupapplication.data.remote.repository.AttachmentRepositoryImpl
import com.tiramisu.groupapplication.data.remote.repository.InspectionRepositoryImpl
import com.tiramisu.groupapplication.data.remote.repository.LogRepositoryImpl
import com.tiramisu.groupapplication.domain.repository.AttachmentRepository
import com.tiramisu.groupapplication.domain.repository.InspectionRepository
import com.tiramisu.groupapplication.domain.repository.LogRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class InspectionModule {
    @Binds
    @Singleton
    abstract fun bindInspectionRepository(impl: InspectionRepositoryImpl): InspectionRepository

    @Binds
    @Singleton
    abstract fun bindAttachmentRepository(impl: AttachmentRepositoryImpl): AttachmentRepository

    @Binds
    @Singleton
    abstract fun bindLogRepository(impl: LogRepositoryImpl): LogRepository

    @Binds
    @Firestore
    @Singleton
    abstract fun bindLogSink(impl: FirestoreLogSink): LogRemoteSink
}