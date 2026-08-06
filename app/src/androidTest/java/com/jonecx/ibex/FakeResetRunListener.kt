package com.jonecx.ibex

import com.jonecx.ibex.fixtures.FakeNetworkConnectionsPreferences
import com.jonecx.ibex.fixtures.FakeRecentFoldersPreferences
import com.jonecx.ibex.fixtures.FakeSettingsPreferences
import com.jonecx.ibex.fixtures.FakeStorageAnalyzer
import org.junit.runner.Description
import org.junit.runner.notification.RunListener
import org.koin.core.context.GlobalContext

// The Koin graph is process-global, so its fake singletons persist across tests. Hilt gave each
// test a fresh component; this restores that isolation by resetting the mutable fakes before
// every test, ahead of the compose rule launching the activity.
class FakeResetRunListener : RunListener() {
    override fun testStarted(description: Description) {
        val koin = GlobalContext.getOrNull() ?: return
        koin.get<FakeSettingsPreferences>().reset()
        koin.get<FakeNetworkConnectionsPreferences>().reset()
        koin.get<FakeRecentFoldersPreferences>().reset()
        koin.get<FakeStorageAnalyzer>().reset()
    }
}
