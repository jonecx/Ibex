package com.jonecx.ibex.di

import com.jonecx.ibex.ui.analysis.StorageAnalysisViewModel
import com.jonecx.ibex.ui.explorer.FileExplorerViewModel
import com.jonecx.ibex.ui.network.NetworkConnectionsViewModel
import com.jonecx.ibex.ui.settings.SettingsViewModel
import com.jonecx.ibex.ui.viewer.MediaViewerViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

// SavedStateHandle is resolved via get() by koinViewModel() at the call site.
val viewModelModule = module {
    viewModel { SettingsViewModel(get(), get(IoDispatcher)) }
    viewModel { StorageAnalysisViewModel(get(), get(MainDispatcher)) }
    viewModel { NetworkConnectionsViewModel(get(), get(), get(IoDispatcher)) }
    viewModel {
        FileExplorerViewModel(get(), get(), get(), get(), get(), get(), get(), get(MainDispatcher))
    }
    viewModel { MediaViewerViewModel(get(), get(), get(IoDispatcher)) }
}
