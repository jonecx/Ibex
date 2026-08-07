package com.jonecx.ibex

import com.jonecx.ibex.fixtures.FakeNetworkConnectionsPreferences
import com.jonecx.ibex.fixtures.FakePlayerSettingsPreferences
import com.jonecx.ibex.fixtures.FakeRecentFoldersPreferences
import com.jonecx.ibex.fixtures.FakeSettingsPreferences
import com.jonecx.ibex.fixtures.FakeStorageAnalyzer
import org.junit.runner.Description
import org.junit.runner.notification.RunListener
import org.koin.core.context.GlobalContext

// The Koin graph is process-global, so its fake singletons persist across tests. Reset the mutable
// fakes before every test, ahead of the compose rule launching the activity, to isolate each run.
class FakeResetRunListener : RunListener() {
    override fun testStarted(description: Description) {
        val koin = GlobalContext.getOrNull() ?: return
        koin.get<FakeSettingsPreferences>().reset()
        koin.get<FakePlayerSettingsPreferences>().reset()
        koin.get<FakeNetworkConnectionsPreferences>().reset()
        koin.get<FakeRecentFoldersPreferences>().reset()
        koin.get<FakeStorageAnalyzer>().reset()
    }
}
