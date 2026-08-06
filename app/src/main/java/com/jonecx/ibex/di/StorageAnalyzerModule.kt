package com.jonecx.ibex.di

import com.jonecx.ibex.data.repository.MediaStoreStorageAnalyzer
import com.jonecx.ibex.data.repository.StorageAnalyzer
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val storageAnalyzerModule = module {
    single<StorageAnalyzer> { MediaStoreStorageAnalyzer(androidContext(), get(IoDispatcher)) }
}
