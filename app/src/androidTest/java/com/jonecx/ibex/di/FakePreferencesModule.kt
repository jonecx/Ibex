package com.jonecx.ibex.di

import com.jonecx.ibex.data.preferences.NetworkConnectionsPreferencesContract
import com.jonecx.ibex.data.preferences.RecentFoldersPreferencesContract
import com.jonecx.ibex.data.preferences.SettingsPreferencesContract
import com.jonecx.ibex.fixtures.FakeNetworkConnectionsPreferences
import com.jonecx.ibex.fixtures.FakeRecentFoldersPreferences
import com.jonecx.ibex.fixtures.FakeSettingsPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [PreferencesModule::class],
)
object FakePreferencesModule {

    @Provides
    @Singleton
    fun provideFakeSettingsPreferences(): FakeSettingsPreferences {
        return FakeSettingsPreferences()
    }

    @Provides
    @Singleton
    fun provideSettingsPreferencesContract(
        fake: FakeSettingsPreferences,
    ): SettingsPreferencesContract = fake

    @Provides
    @Singleton
    fun provideFakeNetworkConnectionsPreferences(): FakeNetworkConnectionsPreferences {
        return FakeNetworkConnectionsPreferences()
    }

    @Provides
    @Singleton
    fun provideNetworkConnectionsPreferencesContract(
        fake: FakeNetworkConnectionsPreferences,
    ): NetworkConnectionsPreferencesContract = fake

    @Provides
    @Singleton
    fun provideFakeRecentFoldersPreferences(): FakeRecentFoldersPreferences {
        return FakeRecentFoldersPreferences()
    }

    @Provides
    @Singleton
    fun provideRecentFoldersPreferencesContract(
        fake: FakeRecentFoldersPreferences,
    ): RecentFoldersPreferencesContract = fake
}
