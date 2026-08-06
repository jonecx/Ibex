package com.jonecx.ibex.di

import com.jonecx.ibex.logging.AppLogger
import com.jonecx.ibex.logging.TimberLogger
import org.koin.dsl.module

val loggerModule = module {
    // Lazy tree resolution breaks the logger <-> analytics cycle at construction time.
    single<AppLogger> { TimberLogger { get() } }
}
