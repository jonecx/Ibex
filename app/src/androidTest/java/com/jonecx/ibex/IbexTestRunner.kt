package com.jonecx.ibex

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

// Swaps in IbexTestApplication so the real app never starts Koin; the test graph owns it instead.
class IbexTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?,
    ): Application {
        return super.newApplication(cl, IbexTestApplication::class.java.name, context)
    }
}
