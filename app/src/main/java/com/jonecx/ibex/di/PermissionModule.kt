package com.jonecx.ibex.di

import com.jonecx.ibex.ui.permission.PermissionChecker
import com.jonecx.ibex.ui.permission.RealPermissionChecker
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PermissionModule {

    @Binds
    @Singleton
    abstract fun bindPermissionChecker(
        realPermissionChecker: RealPermissionChecker,
    ): PermissionChecker
}
