package com.tiramisu.groupapplication.di

import com.google.firebase.Firebase
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirestoreModule {
    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = Firebase.firestore.apply {
        firestoreSettings = firestoreSettings {
            setLocalCacheSettings(persistentCacheSettings { })
        }
    }

    @Provides
    @InspectionCollection
    fun provideInspectionCollection(db: FirebaseFirestore): CollectionReference = db.collection(
        "inspection"
    )

    @Provides
    @ProfileCollection
    fun provideProfileCollection(db: FirebaseFirestore): CollectionReference = db.collection(
        "users"
    )

    @Provides
    @LogCollection
    fun provideLogCollection(db: FirebaseFirestore): CollectionReference = db.collection(
        "log"
    )
}