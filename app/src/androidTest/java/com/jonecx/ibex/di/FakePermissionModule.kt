package com.jonecx.ibex.di

import com.jonecx.ibex.ui.permission.PermissionChecker
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [PermissionModule::class],
)
object FakePermissionModule {

    @Provides
    @Singleton
    fun providePermissionChecker(): PermissionChecker {
        return FakePermissionChecker()
    }
}

class FakePermissionChecker : PermissionChecker {
    override fun hasStoragePermission(): Boolean = true
}
