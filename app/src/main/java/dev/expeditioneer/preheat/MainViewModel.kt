package dev.expeditioneer.preheat

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class MainUiState(
    val gpsTabEnabled: Boolean = false,
)

internal class MainViewModel(
    private val repo: SettingsRepository,
    private val app: Application,
) : ViewModel() {

    private val keyGpsTrackerTab = app.getString(R.string.sp_GPStrackerTab)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.update {
                it.copy(gpsTabEnabled = repo.getBoolean(keyGpsTrackerTab, default = false))
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MyApp)
                MainViewModel(app.settingsRepository, app)
            }
        }
    }
}
