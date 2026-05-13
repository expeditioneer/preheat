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

internal data class StandHeizungUiState(
    val sendEnabled: Boolean = false,
    val isSending: Boolean = false,
    val counter: Int = 0,
    val prepaidCredit: String = "",
    val phoneNumber: String = "",
    val lastCommand: String = "",
    val lastCommandTimestamp: Long = 0L,
    val lastMode: String = "",
    val infoDialog: DialogState? = null,
    val errorDialog: DialogState? = null,
)

internal class StandHeizungViewModel(
    private val repo: SettingsRepository,
    private val app: Application,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StandHeizungUiState())
    val uiState: StateFlow<StandHeizungUiState> = _uiState.asStateFlow()

    private val keySendBtnEnabled = app.getString(R.string.sp_sendBtnEnabled)
    private val keyDestNumb = app.getString(R.string.sp_destNumb)
    private val keySmsCounter = app.getString(R.string.sp_smsCounter)
    private val keyMaxCount = app.getString(R.string.sp_maxSMScount)
    private val keyPrepaidCredit = app.getString(R.string.sp_prepaidCredit)
    private val keyLastCommand = app.getString(R.string.sp_lastCommand)
    private val keyLastCommandTimestamp = app.getString(R.string.sp_lastCommandTimestamp)
    private val keyLastMode = app.getString(R.string.sp_lastMode)
    private val defPhoneNumber = app.getString(R.string.cfg_phonenumber)
    private val defMaxCount = app.getString(R.string.cfg_maxSMScount)
    private val defPrepaidCredit = app.getString(R.string.cfg_prepaidCredit)

    init {
        viewModelScope.launch {
            repo.booleanFlow(keySendBtnEnabled, default = false).collect { enabled ->
                _uiState.update { state ->
                    state.copy(
                        sendEnabled = enabled,
                        isSending = if (enabled) false else state.isSending,
                    )
                }
            }
        }
        viewModelScope.launch {
            repo.intFlow(keySmsCounter, default = 0).collect { c ->
                _uiState.update { it.copy(counter = c) }
            }
        }
        viewModelScope.launch {
            repo.stringFlow(keyPrepaidCredit, defPrepaidCredit).collect { credit ->
                _uiState.update { it.copy(prepaidCredit = credit) }
            }
        }
        viewModelScope.launch {
            repo.stringFlow(keyDestNumb, defPhoneNumber).collect { number ->
                _uiState.update { it.copy(phoneNumber = number) }
            }
        }
        viewModelScope.launch {
            repo.stringFlow(keyLastCommand, default = "").collect { label ->
                _uiState.update { it.copy(lastCommand = label) }
            }
        }
        viewModelScope.launch {
            repo.longFlow(keyLastCommandTimestamp, default = 0L).collect { ts ->
                _uiState.update { it.copy(lastCommandTimestamp = ts) }
            }
        }
        viewModelScope.launch {
            repo.stringFlow(keyLastMode, default = "").collect { mode ->
                _uiState.update { it.copy(lastMode = mode) }
            }
        }

        viewModelScope.launch {
            val destNumb = repo.getString(keyDestNumb, defPhoneNumber)
            if (destNumb == "0") {
                _uiState.update {
                    it.copy(
                        infoDialog = DialogState(
                            title = app.getString(R.string.ui_welcome_title),
                            text = app.getString(R.string.helptext),
                        ),
                    )
                }
                repo.setBoolean(keySendBtnEnabled, false)
            }
        }
    }

    fun send(cmdKey: String, defaultCmd: String, label: String) {
        _uiState.update { it.copy(isSending = true) }
        viewModelScope.launch {
            repo.setBoolean(keySendBtnEnabled, false)
            val msg = repo.getString(cmdKey, defaultCmd)
            sendHeaterCommand(
                context = app,
                repo = repo,
                msg = msg,
                destNumbKey = keyDestNumb,
                sendBtnEnabledKey = keySendBtnEnabled,
                smsCounterKey = keySmsCounter,
                maxCountKey = keyMaxCount,
                maxCountDefault = defMaxCount,
                lastCommandKey = keyLastCommand,
                lastCommandTimestampKey = keyLastCommandTimestamp,
                commandLabel = label,
                lastModeKey = if (label == "Summer" || label == "Winter") keyLastMode else null,
                onError = { text ->
                    _uiState.update {
                        it.copy(
                            errorDialog = DialogState(
                                title = app.getString(R.string.com_cnfgErrTitle),
                                text = text,
                            ),
                            isSending = false,
                        )
                    }
                },
                errorNoDest = app.getString(R.string.com_cnfgErrTxt),
                errorLimitPrefix = app.getString(R.string.com_smsCounterExceed),
                errorSendFailed = app.getString(R.string.com_sendFailed),
            )
        }
    }

    fun dismissInfoDialog() {
        _uiState.update { it.copy(infoDialog = null) }
    }

    fun dismissErrorDialog() {
        _uiState.update { it.copy(errorDialog = null) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MyApp)
                StandHeizungViewModel(app.settingsRepository, app)
            }
        }
    }
}
