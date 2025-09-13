package com.tiramisu.groupapplication.data.remote.repository

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.SetOptions
import com.tiramisu.groupapplication.di.ProfileCollection
import com.tiramisu.groupapplication.domain.enums.UserRole
import com.tiramisu.groupapplication.domain.model.User
import com.tiramisu.groupapplication.domain.repository.UserRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    @ProfileCollection private val profileCol: CollectionReference,
) : UserRepository {
    override fun getUserProfile(uid: String): Flow<User?> = callbackFlow {
        val userDoc = profileCol.document(uid)
        val registration = userDoc.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val user = snapshot.toObject(User::class.java)
                trySend(user)
            } else {
                trySend(null)
            }
        }
        awaitClose { registration.remove() }
    }

    override fun getAllUsers(): Flow<List<User>> = callbackFlow {
        val registration = profileCol.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val users =
                snapshot?.documents?.mapNotNull { it.toObject(User::class.java) } ?: emptyList()
            trySend(users)
        }
        awaitClose { registration.remove() }
    }

    override suspend fun updateUserRole(uid: String, role: UserRole, upsert: Boolean) {
        val docRef = profileCol.document(uid)
        if (upsert) {
            // Merge only role (and uid if you want to ensure the field exists)
            docRef.set(
                mapOf(
                    "uid" to uid,
                    "role" to role.name
                ),
                SetOptions.merge()
            ).await()
        } else {
            // Update fails if the doc doesn't exist (safer if you expect it to exist)
            docRef.update("role", role.name).await()
        }
    }
}