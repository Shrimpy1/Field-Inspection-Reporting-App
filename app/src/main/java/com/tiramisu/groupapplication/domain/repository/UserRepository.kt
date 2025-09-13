package com.tiramisu.groupapplication.domain.repository

import com.tiramisu.groupapplication.domain.enums.UserRole
import com.tiramisu.groupapplication.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    /**
     * A flow that emits the user profile for the given UID.
     */
    fun getUserProfile(uid: String): Flow<User?>

    /**
     * A flow that emits a list of all users. For Admin use.
     */
    fun getAllUsers(): Flow<List<User>>

    /** Update only the role field. If upsert = true, create/merge the doc if it doesn't exist. */
    suspend fun updateUserRole(uid: String, role: UserRole, upsert: Boolean = false)
}