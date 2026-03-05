package com.jonecx.ibex.ui.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jonecx.ibex.data.model.StorageBreakdown
import com.jonecx.ibex.data.repository.StorageAnalyzer
import com.jonecx.ibex.di.MainDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StorageAnalysisUiState(
    val isLoading: Boolean = true,
    val breakdown: StorageBreakdown? = null,
    val error: Throwable? = null,
)

@HiltViewModel
class StorageAnalysisViewModel @Inject constructor(
    private val storageAnalyzer: StorageAnalyzer,
    @MainDispatcher private val dispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StorageAnalysisUiState())
    val uiState: StateFlow<StorageAnalysisUiState> = _uiState.asStateFlow()

    init {
        analyze()
    }

    fun analyze() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch(dispatcher) {
            try {
                val breakdown = storageAnalyzer.analyze()
                _uiState.update { it.copy(isLoading = false, breakdown = breakdown) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e) }
            }
        }
    }
}
