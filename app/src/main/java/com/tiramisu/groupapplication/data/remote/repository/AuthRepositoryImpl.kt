package com.tiramisu.groupapplication.data.remote.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.tiramisu.groupapplication.di.IoDispatcher
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
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : AuthRepository {

    override val currentUser: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener {
            trySend(it.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun signIn(email: String, pass: String): Result<FirebaseUser> =
        withContext(ioDispatcher) {
            runCatching {
                auth.signInWithEmailAndPassword(email, pass).await().user!!
            }
        }

    override suspend fun signUp(email: String, pass: String): Result<FirebaseUser> =
        withContext(ioDispatcher) {
            runCatching {
                auth.createUserWithEmailAndPassword(email, pass).await().user!!
            }
        }

    override suspend fun signOut() = withContext(ioDispatcher) {
        auth.signOut()
    }
}

