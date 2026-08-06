package com.jonecx.ibex

import android.app.Application
import com.jonecx.ibex.di.testModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

// Starts one Koin graph for the whole instrumented run, before any activity launches.
// Fakes are process-global singletons; each test resets them in @Before.
class IbexTestApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@IbexTestApplication)
            modules(testModules)
        }
    }
}
