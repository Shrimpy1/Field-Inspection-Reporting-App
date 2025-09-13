package com.tiramisu.groupapplication.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiramisu.groupapplication.domain.enums.UserRole
import com.tiramisu.groupapplication.domain.model.User
import com.tiramisu.groupapplication.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val userRepo: UserRepository
) : ViewModel() {

    // Admins can see all users
    val allUsers: StateFlow<List<User>> = userRepo.getAllUsers()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Change a specific user's role
    fun changeUserRole(uid: String, role: UserRole, upsert: Boolean = false) {
        viewModelScope.launch {
            userRepo.updateUserRole(uid, role, upsert)
        }
    }

}