package com.jonecx.ibex.di

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.qualifier.named
import org.koin.dsl.module

// Koin qualifiers distinguishing the coroutine dispatchers.
val IoDispatcher = named("IoDispatcher")
val MainDispatcher = named("MainDispatcher")
val DefaultDispatcher = named("DefaultDispatcher")
val ApplicationScope = named("ApplicationScope")

val dispatcherModule = module {
    single<CoroutineDispatcher>(IoDispatcher) { Dispatchers.IO }
    single<CoroutineDispatcher>(MainDispatcher) { Dispatchers.Main }
    single<CoroutineDispatcher>(DefaultDispatcher) { Dispatchers.Default }
    single<CoroutineScope>(ApplicationScope) {
        CoroutineScope(SupervisorJob() + get<CoroutineDispatcher>(DefaultDispatcher))
    }
}
