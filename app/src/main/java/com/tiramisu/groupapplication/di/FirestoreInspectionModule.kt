package com.tiramisu.groupapplication.di

import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirestoreInspectionModule {
    @Provides
    @Singleton
    fun provideInspectionCollection(db: FirebaseFirestore) = db.collection(
        "inspection"
    )
}