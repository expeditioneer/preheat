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

internal data class GPStrackerUiState(
    val phoneNumber: String = "",
    val posReqCmd: String = "",
    val sendEnabled: Boolean = false,
    val helpDialog: DialogState? = null,
    val errorDialog: DialogState? = null,
)

internal class GPStrackerViewModel(
    private val repo: SettingsRepository,
    private val app: Application,
) : ViewModel() {

    private val keyDestNumb = app.getString(R.string.sp_destNumbGPS)
    private val keySendBtnEnabled = app.getString(R.string.sp_sendBtnEnabledGPS)
    private val keyPosReqCmd = app.getString(R.string.sp_GPSposReqCmd)
    private val defPhoneNumber = app.getString(R.string.cfg_phonenumber)
    private val defPosReqCmd = app.getString(R.string.cfg_GPSposReqCmd)

    private val _uiState = MutableStateFlow(GPStrackerUiState())
    val uiState: StateFlow<GPStrackerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repo.booleanFlow(keySendBtnEnabled, default = false).collect { enabled ->
                _uiState.update { it.copy(sendEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            val phoneNumber = repo.getString(keyDestNumb, defPhoneNumber)
            val posReqCmd = repo.getString(keyPosReqCmd, defPosReqCmd)
            _uiState.update { it.copy(phoneNumber = phoneNumber, posReqCmd = posReqCmd) }

            if (isPhoneNumberCorrect(phoneNumber)) {
                repo.setBoolean(keySendBtnEnabled, true)
            }
        }
    }

    fun setPhoneNumber(v: String) {
        if (!isPhoneNumberCorrect(v)) {
            _uiState.update {
                it.copy(
                    errorDialog = DialogState(
                        title = app.getString(R.string.com_warning),
                        text = app.getString(R.string.cfg_checknumbertext),
                    ),
                )
            }
            return
        }
        _uiState.update { it.copy(phoneNumber = v) }
        viewModelScope.launch {
            repo.setString(keyDestNumb, v)
            repo.setBoolean(keySendBtnEnabled, true)
        }
    }

    fun setPosReqCmd(v: String) {
        _uiState.update { it.copy(posReqCmd = v) }
        viewModelScope.launch { repo.setString(keyPosReqCmd, v) }
    }

    fun onRequestClicked() {
        // Mirror legacy click handler: bounce sendBtnEnabledGPS
        // false -> true. The actual sendSMS2numb call lived in a /* */
        // block in v2.2 and is still inert — Phase 4b will hook this
        // up to the real SubscriptionManager send.
        viewModelScope.launch {
            repo.setBoolean(keySendBtnEnabled, false)
            repo.setBoolean(keySendBtnEnabled, true)
        }
    }

    fun showHelp() {
        _uiState.update {
            it.copy(
                helpDialog = DialogState(
                    title = app.getString(R.string.sh_helpbutton),
                    text = app.getString(R.string.helptextGPS),
                ),
            )
        }
    }

    fun dismissHelpDialog() {
        _uiState.update { it.copy(helpDialog = null) }
    }

    fun dismissErrorDialog() {
        _uiState.update { it.copy(errorDialog = null) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MyApp)
                GPStrackerViewModel(app.settingsRepository, app)
            }
        }
    }
}
