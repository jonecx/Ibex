package com.jonecx.ibex.di

import com.jonecx.ibex.data.preferences.NetworkConnectionsPreferences
import com.jonecx.ibex.data.preferences.NetworkConnectionsPreferencesContract
import com.jonecx.ibex.data.preferences.PlayerSettingsPreferences
import com.jonecx.ibex.data.preferences.PlayerSettingsPreferencesContract
import com.jonecx.ibex.data.preferences.RecentFoldersPreferences
import com.jonecx.ibex.data.preferences.RecentFoldersPreferencesContract
import com.jonecx.ibex.data.preferences.SettingsPreferences
import com.jonecx.ibex.data.preferences.SettingsPreferencesContract
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val preferencesModule = module {
    single<SettingsPreferencesContract> { SettingsPreferences(androidContext()) }
    single<PlayerSettingsPreferencesContract> { PlayerSettingsPreferences(androidContext()) }
    single<NetworkConnectionsPreferencesContract> {
        NetworkConnectionsPreferences(androidContext(), get())
    }
    single<RecentFoldersPreferencesContract> { RecentFoldersPreferences(androidContext()) }
}
