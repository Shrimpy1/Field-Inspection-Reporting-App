package com.tiramisu.groupapplication.di

import com.tiramisu.groupapplication.data.remote.repository.AttachmentRepositoryImpl
import com.tiramisu.groupapplication.data.remote.repository.InspectionRepositoryImpl
import com.tiramisu.groupapplication.domain.repository.AttachmentRepository
import com.tiramisu.groupapplication.domain.repository.InspectionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class InspectionModule {
    @Binds
    abstract fun bindInspectionRepository(impl: InspectionRepositoryImpl): InspectionRepository

    @Binds
    abstract fun bindAttachmentRepository(impl: AttachmentRepositoryImpl): AttachmentRepository
}