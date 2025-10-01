package com.jholachhapdevs.pdfjuggler.home.ui

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.jholachhapdevs.pdfjuggler.home.domain.usecase.GetUpdatesUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class HomeScreenModel(
    private val getUpdatesUseCase: GetUpdatesUseCase
) : ScreenModel {

    var uiState: HomeUiState by mutableStateOf(HomeUiState(loading = true))
        private set

    init {  // static initializer block like java
        loadUpdateInfo()
    }

    private fun loadUpdateInfo() {
        screenModelScope.launch { // screenModelScope is a CoroutineScope tied to the lifecycle of the ScreenModel
             // This means that any coroutines launched in this scope will be automatically cancelled when the ScreenModel is cleared
            uiState = HomeUiState(loading = true)
            try {
                val info = getUpdatesUseCase()
                uiState = HomeUiState(
                    loading = false,
                    updateInfo = info,
                    error = null
                )
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                uiState = HomeUiState(
                    loading = false,
                    updateInfo = null,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }
}