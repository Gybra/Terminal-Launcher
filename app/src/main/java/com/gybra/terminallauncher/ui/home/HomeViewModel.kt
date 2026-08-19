package com.gybra.terminallauncher.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gybra.terminallauncher.launcher.AppRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val appRepository: AppRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = mutableUiState.asStateFlow()

    init {
        loadInstalledApps()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            val apps = try {
                appRepository.getInstalledApps()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                emptyList()
            }
            mutableUiState.value = HomeUiState(apps = apps)
        }
    }
}
