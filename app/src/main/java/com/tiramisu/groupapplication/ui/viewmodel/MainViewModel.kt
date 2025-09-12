package com.tiramisu.groupapplication.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiramisu.groupapplication.domain.model.User
import com.tiramisu.groupapplication.domain.repository.AuthRepository
import com.tiramisu.groupapplication.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    internal val authRepo: AuthRepository,
    private val userRepo: UserRepository
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentUser: StateFlow<User?> = authRepo.currentUser
        .flatMapLatest { firebaseUser ->
            if (firebaseUser != null) {
                userRepo.getUserProfile(firebaseUser.uid)
            } else {
                flowOf(null)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}