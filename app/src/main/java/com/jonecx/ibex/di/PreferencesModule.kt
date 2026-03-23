package com.jonecx.ibex.di

import com.jonecx.ibex.data.preferences.NetworkConnectionsPreferences
import com.jonecx.ibex.data.preferences.NetworkConnectionsPreferencesContract
import com.jonecx.ibex.data.preferences.RecentFoldersPreferences
import com.jonecx.ibex.data.preferences.RecentFoldersPreferencesContract
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

    @Binds
    @Singleton
    abstract fun bindNetworkConnectionsPreferences(
        impl: NetworkConnectionsPreferences,
    ): NetworkConnectionsPreferencesContract

    @Binds
    @Singleton
    abstract fun bindRecentFoldersPreferences(
        impl: RecentFoldersPreferences,
    ): RecentFoldersPreferencesContract
}
