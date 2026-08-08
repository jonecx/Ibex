package com.jonecx.ibex.ui.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jonecx.ibex.analytics.AnalyticsManager
import com.jonecx.ibex.data.model.StorageBreakdown
import com.jonecx.ibex.data.repository.StorageAnalyzer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StorageAnalysisUiState(
    val isLoading: Boolean = true,
    val breakdown: StorageBreakdown? = null,
    val error: Throwable? = null,
)

class StorageAnalysisViewModel(
    private val storageAnalyzer: StorageAnalyzer,
    private val analyticsManager: AnalyticsManager,
    private val dispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StorageAnalysisUiState())
    val uiState: StateFlow<StorageAnalysisUiState> = _uiState.asStateFlow()

    init {
        analyze(isRetry = false)
    }

    // Public entry point (e.g. the retry button); the initial run is triggered from init.
    fun analyze() = analyze(isRetry = true)

    private fun analyze(isRetry: Boolean) {
        analyticsManager.trackStorageAnalysisStart(isRetry = isRetry)
        _uiState.update { it.copy(isLoading = true, error = null) }
        val startMs = System.currentTimeMillis()
        viewModelScope.launch(dispatcher) {
            try {
                val breakdown = storageAnalyzer.analyze()
                analyticsManager.trackStorageAnalysisComplete(
                    success = true,
                    durationMs = System.currentTimeMillis() - startMs,
                    usedBytes = breakdown.usedBytes,
                    totalBytes = breakdown.totalBytes,
                    categoryCount = breakdown.categories.size,
                )
                _uiState.update { it.copy(isLoading = false, breakdown = breakdown) }
            } catch (e: Exception) {
                analyticsManager.trackStorageAnalysisComplete(
                    success = false,
                    durationMs = System.currentTimeMillis() - startMs,
                    errorCode = e.javaClass.simpleName,
                )
                _uiState.update { it.copy(isLoading = false, error = e) }
            }
        }
    }
}
