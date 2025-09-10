package com.tiramisu.groupapplication.data.remote.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.tiramisu.groupapplication.domain.model.User
import com.tiramisu.groupapplication.domain.repository.UserRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : UserRepository {

    override fun getUserProfile(uid: String): Flow<User?> = callbackFlow {
        val userDoc = firestore.collection("users").document(uid)
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
        val colRef = firestore.collection("users")
        val registration = colRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val users = snapshot?.documents?.mapNotNull { it.toObject(User::class.java) } ?: emptyList()
            trySend(users)
        }
        awaitClose { registration.remove() }
    }
}

