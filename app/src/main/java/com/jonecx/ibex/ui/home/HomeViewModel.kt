package com.jonecx.ibex.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jonecx.ibex.data.model.FileSourceType
import com.jonecx.ibex.data.model.SourceStats
import com.jonecx.ibex.data.model.StorageUsage
import com.jonecx.ibex.data.repository.HomeSourceStatsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val stats: Map<FileSourceType, SourceStats> = emptyMap(),
    val storageUsage: StorageUsage? = null,
)

class HomeViewModel(
    private val repository: HomeSourceStatsRepository,
    private val dispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    // Stats are a tile enhancement; on failure the tiles simply render without a subtitle.
    private fun loadStats() {
        viewModelScope.launch(dispatcher) {
            runCatching { repository.loadStats() }
                .onSuccess { home ->
                    _uiState.update { it.copy(stats = home.sources, storageUsage = home.storageUsage) }
                }
        }
    }
}
