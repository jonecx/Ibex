package com.jonecx.ibex.di

import com.jonecx.ibex.data.crypto.CryptoManager
import com.jonecx.ibex.data.crypto.TinkCryptoManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val cryptoModule = module {
    single<CryptoManager> { TinkCryptoManager(androidContext()) }
}
