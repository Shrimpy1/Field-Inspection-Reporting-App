package com.tiramisu.groupapplication

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.tiramisu.groupapplication.worker.InspectionSyncWorker
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class MainApplication : Application(), Configuration.Provider {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface HiltWorkerFactoryEntryPoint {
        fun workerFactory(): HiltWorkerFactory
    }

    override val workManagerConfiguration: Configuration =
        Configuration.Builder()
            .setWorkerFactory(
                EntryPoints.get(this, HiltWorkerFactoryEntryPoint::class.java).workerFactory()
            )
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        schedulePeriodicPull(WorkManager.getInstance(this))
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