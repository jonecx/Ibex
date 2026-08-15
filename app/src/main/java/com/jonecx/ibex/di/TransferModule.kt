package com.jonecx.ibex.di

import com.jonecx.ibex.data.repository.ProtocolFileHandler
import com.jonecx.ibex.data.transfer.DefaultTransferManager
import com.jonecx.ibex.data.transfer.TransferEngine
import com.jonecx.ibex.data.transfer.TransferJournal
import com.jonecx.ibex.data.transfer.TransferManager
import com.jonecx.ibex.data.transfer.TransferScheduler
import com.jonecx.ibex.data.transfer.WorkManagerTransferScheduler
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import java.io.File

val transferModule = module {
    single { TransferJournal(File(androidContext().filesDir, "transfers/journal.json"), get(IoDispatcher)) }

    // Same handler set the clipboard/move stack uses, so transfers stay protocol-agnostic.
    single { TransferEngine(getAll<ProtocolFileHandler>().toSet(), get(IoDispatcher)) }

    single<TransferScheduler> { WorkManagerTransferScheduler(androidContext()) }

    single<TransferManager> {
        DefaultTransferManager(
            scheduler = get(),
            engine = get(),
            journal = get(),
            appScope = get(ApplicationScope),
        )
    }
}
