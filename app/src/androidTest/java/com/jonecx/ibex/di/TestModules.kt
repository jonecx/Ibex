package com.jonecx.ibex.di

import coil.ImageLoader
import com.jonecx.ibex.analytics.AnalyticsProvider
import com.jonecx.ibex.data.preferences.NetworkConnectionsPreferencesContract
import com.jonecx.ibex.data.preferences.PlayerSettingsPreferencesContract
import com.jonecx.ibex.data.preferences.RecentFoldersPreferencesContract
import com.jonecx.ibex.data.preferences.SettingsPreferencesContract
import com.jonecx.ibex.data.repository.FakeFileRepository
import com.jonecx.ibex.data.repository.FileClipboardManager
import com.jonecx.ibex.data.repository.FileMoveManager
import com.jonecx.ibex.data.repository.FileTrashManager
import com.jonecx.ibex.data.repository.SmbContextProviderContract
import com.jonecx.ibex.data.repository.StorageAnalyzer
import com.jonecx.ibex.fixtures.FakeAnalyticsProvider
import com.jonecx.ibex.fixtures.FakeAppLogger
import com.jonecx.ibex.fixtures.FakeFileClipboardManager
import com.jonecx.ibex.fixtures.FakeFileImageRequestFactory
import com.jonecx.ibex.fixtures.FakeFileMoveManager
import com.jonecx.ibex.fixtures.FakeFileRepositoryFactory
import com.jonecx.ibex.fixtures.FakeFileTrashManager
import com.jonecx.ibex.fixtures.FakeNetworkConnectionsPreferences
import com.jonecx.ibex.fixtures.FakePlayerSettingsPreferences
import com.jonecx.ibex.fixtures.FakeRecentFoldersPreferences
import com.jonecx.ibex.fixtures.FakeSettingsPreferences
import com.jonecx.ibex.fixtures.FakeSmbContextProvider
import com.jonecx.ibex.fixtures.FakeStorageAnalyzer
import com.jonecx.ibex.logging.AppLogger
import com.jonecx.ibex.ui.explorer.components.FileImageRequestFactory
import com.jonecx.ibex.ui.permission.PermissionChecker
import com.jonecx.ibex.util.FakeImageLoader
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

class FakePermissionChecker : PermissionChecker {
    override fun hasStoragePermission(): Boolean = true
}

// Loaded after appModules; these fake definitions override their production counterparts.
// Concrete fake types are also exposed so tests can resolve and reset them via inject().
@OptIn(ExperimentalCoroutinesApi::class)
val testOverridesModule = module {
    single<CoroutineDispatcher>(IoDispatcher) { UnconfinedTestDispatcher() }
    single<CoroutineDispatcher>(MainDispatcher) { UnconfinedTestDispatcher() }
    single<CoroutineDispatcher>(DefaultDispatcher) { UnconfinedTestDispatcher() }

    single<AnalyticsProvider> { FakeAnalyticsProvider() }
    single<AppLogger> { FakeAppLogger() }
    single<PermissionChecker> { FakePermissionChecker() }
    single<ImageLoader> { FakeImageLoader(androidContext()) }
    single<FileImageRequestFactory> { FakeFileImageRequestFactory() }

    single { FakeSettingsPreferences() }
    single<SettingsPreferencesContract> { get<FakeSettingsPreferences>() }
    single { FakePlayerSettingsPreferences() }
    single<PlayerSettingsPreferencesContract> { get<FakePlayerSettingsPreferences>() }
    single { FakeNetworkConnectionsPreferences() }
    single<NetworkConnectionsPreferencesContract> { get<FakeNetworkConnectionsPreferences>() }
    single { FakeRecentFoldersPreferences() }
    single<RecentFoldersPreferencesContract> { get<FakeRecentFoldersPreferences>() }

    single { FakeStorageAnalyzer() }
    single<StorageAnalyzer> { get<FakeStorageAnalyzer>() }

    single<FileRepositoryFactory> { FakeFileRepositoryFactory(FakeFileRepository()) }
    single<FileTrashManager> { FakeFileTrashManager() }
    single<FileMoveManager> { FakeFileMoveManager() }
    single<FileClipboardManager> { FakeFileClipboardManager(get()) }
    single<SmbContextProviderContract> { FakeSmbContextProvider() }
}

val testModules = appModules + testOverridesModule
