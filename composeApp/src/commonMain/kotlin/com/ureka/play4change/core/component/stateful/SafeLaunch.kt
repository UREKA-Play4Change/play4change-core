package com.ureka.play4change.core.component.stateful

import com.ureka.play4change.core.component.base.ComponentState
import com.ureka.play4change.core.network.NetworkException
import com.ureka.play4change.core.network.toUiError
import com.ureka.play4change.core.network.toNetworkError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

fun <S : ComponentState> StatefulComponent<S>.safeLaunch(
    scope: CoroutineScope,
    block: suspend () -> Unit
) {
    updateState { copyBase(isLoading = true, error = null) }
    scope.launch {
        try {
            block()
            updateState { copyBase(isLoading = false) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: NetworkException) {
            updateState { copyBase(isLoading = false, error = e.error.toUiError()) }
        } catch (e: Exception) {
            updateState { copyBase(isLoading = false, error = e.toNetworkError().toUiError()) }
        }
    }
}
