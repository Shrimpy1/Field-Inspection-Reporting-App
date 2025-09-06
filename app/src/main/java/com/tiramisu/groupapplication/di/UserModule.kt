package com.tiramisu.groupapplication.di

import com.tiramisu.groupapplication.data.remote.repository.UserRepositoryImpl
import com.tiramisu.groupapplication.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class UserModule {
    @Binds
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository
}
