package com.jonecx.ibex.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Launches a coroutine on [dispatcher] that collects from [flow] and invokes [action] for each emission.
 * Reduces boilerplate for the common viewModelScope.launch(dispatcher) { flow.collect { ... } } pattern.
 */
fun <T> CoroutineScope.launchCollect(
    flow: Flow<T>,
    dispatcher: CoroutineDispatcher,
    action: suspend (T) -> Unit,
) {
    launch(dispatcher) { flow.collect(action) }
}
