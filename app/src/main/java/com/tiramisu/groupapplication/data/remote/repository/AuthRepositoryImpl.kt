package com.tiramisu.groupapplication.data.remote.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.tiramisu.groupapplication.di.IoDispatcher
import com.tiramisu.groupapplication.domain.model.User
import com.tiramisu.groupapplication.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : AuthRepository {

    override val currentUser: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener {
            trySend(it.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun signIn(email: String, pass: String, role: String): Result<FirebaseUser> =
        withContext(ioDispatcher) {
            runCatching {
                // 1. Authenticate with Firebase Auth
                val authResult = auth.signInWithEmailAndPassword(email, pass).await()
                val firebaseUser = authResult.user!!

                // 2. Fetch user profile from Firestore
                val userDoc = firestore.collection("users").document(firebaseUser.uid).get().await()
                val userProfile = userDoc.toObject(User::class.java)

                // 3. Verify the role
                if (userProfile?.role == role) {
                    firebaseUser // Success, return the user
                } else {
                    // Roles do not match, throw an error
                    auth.signOut() // Sign out the user immediately for security
                    throw IllegalStateException("The selected role does not match your account.")
                }
            }
        }

    override suspend fun signUp(email: String, pass: String, role: String): Result<FirebaseUser> =
        withContext(ioDispatcher) {
            runCatching {
                val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
                val firebaseUser = authResult.user!!
                val userProfile = User(uid = firebaseUser.uid, email = email, role = role)
                firestore.collection("users").document(firebaseUser.uid).set(userProfile).await()
                firebaseUser
            }
        }

    override suspend fun signOut() = withContext(ioDispatcher) {
        auth.signOut()
    }
}

