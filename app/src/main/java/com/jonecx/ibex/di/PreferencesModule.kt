package com.jonecx.ibex.di

import com.jonecx.ibex.data.preferences.SettingsPreferences
import com.jonecx.ibex.data.preferences.SettingsPreferencesContract
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PreferencesModule {

    @Binds
    @Singleton
    abstract fun bindSettingsPreferences(
        impl: SettingsPreferences,
    ): SettingsPreferencesContract
}
