package com.tiramisu.groupapplication.domain.model

import com.tiramisu.groupapplication.domain.enums.UserRole

data class User(
    val uid: String = "",
    val email: String = "",
    val role: UserRole = UserRole.INSPECTOR
)