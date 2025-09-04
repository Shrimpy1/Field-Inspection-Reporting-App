package com.tiramisu.groupapplication.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.tiramisu.groupapplication.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    internal val authRepo: AuthRepository
) : ViewModel() {
    val currentUser = authRepo.currentUser
}
