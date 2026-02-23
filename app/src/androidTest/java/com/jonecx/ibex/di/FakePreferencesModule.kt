package com.jonecx.ibex.di

import com.jonecx.ibex.data.preferences.SettingsPreferencesContract
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
}
