package com.tiramisu.groupapplication.di

import android.content.Context
import androidx.room.Room
import com.tiramisu.groupapplication.BuildConfig
import com.tiramisu.groupapplication.data.local.AppDatabase
import com.tiramisu.groupapplication.data.local.InspectionAttachmentDao
import com.tiramisu.groupapplication.data.local.InspectionDao
import com.tiramisu.groupapplication.data.local.LogDao
import com.tiramisu.groupapplication.data.local.SyncMetaDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RoomModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        val builder = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "inspection_database"
        )

        if (BuildConfig.DEBUG) {
            builder.fallbackToDestructiveMigration(true)
        }

        return builder.build()
    }

    @Provides
    fun provideInspectionDao(appDatabase: AppDatabase): InspectionDao {
        return appDatabase.inspectionDao()
    }

    @Provides
    fun provideAttachmentDao(appDatabase: AppDatabase): InspectionAttachmentDao {
        return appDatabase.inspectionAttachmentDao()
    }

    @Provides
    fun provideSyncMetaDao(appDatabase: AppDatabase): SyncMetaDao {
        return appDatabase.syncMetaDao()
    }

    @Provides
    fun provideLogDao(appDatabase: AppDatabase): LogDao {
        return appDatabase.logDao()
    }
}