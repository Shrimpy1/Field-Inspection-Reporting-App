package com.tiramisu.groupapplication.domain.repository

import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

/**
 * Interface for all authentication-related operations.
 */
interface AuthRepository {
    /**
     * A flow that emits the current FirebaseUser or null if not signed in.
     * This allows the UI to reactively observe the authentication state.
     */
    val currentUser: Flow<FirebaseUser?>

    /**
     * Attempts to sign in a user and verifies their role.
     * @return A Result containing the FirebaseUser on success or an exception on failure.
     */
    suspend fun signIn(email: String, pass: String, role: String): Result<FirebaseUser>

    /**
     * Attempts to create a new user account with the given email, password and role.
     * @return A Result containing the new FirebaseUser on success or an exception on failure.
     */
    suspend fun signUp(email: String, pass: String, role: String): Result<FirebaseUser>

    /**
     * Signs out the currently authenticated user.
     */
    suspend fun signOut()
}

