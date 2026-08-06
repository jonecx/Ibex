package com.jonecx.ibex.di

import com.jonecx.ibex.ui.explorer.components.DefaultFileImageRequestFactory
import com.jonecx.ibex.ui.explorer.components.FileImageRequestFactory
import org.koin.dsl.module

val imageRequestModule = module {
    single<FileImageRequestFactory> { DefaultFileImageRequestFactory() }
}
