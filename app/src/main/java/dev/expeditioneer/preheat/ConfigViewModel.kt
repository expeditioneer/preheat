package dev.expeditioneer.preheat

import android.app.Application
import android.widget.Toast
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

internal enum class ConfirmAction { FeedbackToggle, HeatDurationSave }

internal data class ConfigUiState(
    val phoneNumber: String = "",
    val startCmd: String = "",
    val stopCmd: String = "",
    val tempCmd: String = "",
    val summerCmd: String = "",
    val winterCmd: String = "",
    val statusCmd: String = "",
    val heatDurationCmd: String = "",
    val heatDuration: String = "",
    val smsFeedbackCmd: String = "",
    val maxSmsCount: String = "",
    val prepaidCredit: String = "",
    val smsCost: String = "",
    val smsFeedback: Boolean = true,
    val gpsTrackerTab: Boolean = false,
    val smsCounter: Int = 0,
    val appVersion: String = "?",
    val restoreConfirmOpen: Boolean = false,
    val errorDialog: DialogState? = null,
    val confirmDialog: ConfirmAction? = null,
)

internal class ConfigViewModel(
    private val repo: SettingsRepository,
    private val app: Application,
) : ViewModel() {

    private val keyDestNumb = app.getString(R.string.sp_destNumb)
    private val keyStartCmd = app.getString(R.string.sp_startCmd)
    private val keyStopCmd = app.getString(R.string.sp_stopCmd)
    private val keyTempCmd = app.getString(R.string.sp_tempCmd)
    private val keySummerCmd = app.getString(R.string.sp_summerCmd)
    private val keyWinterCmd = app.getString(R.string.sp_winterCmd)
    private val keyStatusCmd = app.getString(R.string.sp_statusCmd)
    private val keyHeatDurationCmd = app.getString(R.string.sp_heatDurationCmd)
    private val keyHeatDuration = app.getString(R.string.sp_heatDuration)
    private val keySmsFeedbackCmd = app.getString(R.string.sp_smsFeedbackCmd)
    private val keyMaxSmsCount = app.getString(R.string.sp_maxSMScount)
    private val keyPrepaidCredit = app.getString(R.string.sp_prepaidCredit)
    private val keySmsCost = app.getString(R.string.sp_SMScost)
    private val keySmsFeedback = app.getString(R.string.sp_smsFeedback)
    private val keyGpsTrackerTab = app.getString(R.string.sp_GPStrackerTab)
    private val keySendBtnEnabled = app.getString(R.string.sp_sendBtnEnabled)
    private val keySendBtnEnabledGPS = app.getString(R.string.sp_sendBtnEnabledGPS)
    private val keySmsCounter = app.getString(R.string.sp_smsCounter)
    private val keyLastCommand = app.getString(R.string.sp_lastCommand)
    private val keyLastCommandTimestamp = app.getString(R.string.sp_lastCommandTimestamp)

    private val defPhoneNumber = app.getString(R.string.cfg_phonenumber)
    private val defStartCmd = app.getString(R.string.cfg_startcmd)
    private val defStopCmd = app.getString(R.string.cfg_stopcmd)
    private val defTempCmd = app.getString(R.string.cfg_tempcmd)
    private val defSummerCmd = app.getString(R.string.cfg_summercmd)
    private val defWinterCmd = app.getString(R.string.cfg_wintercmd)
    private val defStatusCmd = app.getString(R.string.cfg_statuscmd)
    private val defHeatDurationCmd = app.getString(R.string.cfg_heattimercmd)
    private val defHeatDuration = app.getString(R.string.cfg_heatduration)
    private val defSmsFeedbackCmd = app.getString(R.string.cfg_SMSfeedbackcmd)
    private val defMaxSmsCount = app.getString(R.string.cfg_maxSMScount)
    private val defPrepaidCredit = app.getString(R.string.cfg_prepaidCredit)
    private val defSmsCost = app.getString(R.string.cfg_SMScost)

    private val _uiState = MutableStateFlow(ConfigUiState())
    val uiState: StateFlow<ConfigUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repo.booleanFlow(keySmsFeedback, default = true).collect { v ->
                _uiState.update { it.copy(smsFeedback = v) }
            }
        }
        viewModelScope.launch {
            repo.booleanFlow(keyGpsTrackerTab, default = false).collect { v ->
                _uiState.update { it.copy(gpsTrackerTab = v) }
            }
        }
        viewModelScope.launch {
            repo.intFlow(keySmsCounter, default = 0).collect { v ->
                _uiState.update { it.copy(smsCounter = v) }
            }
        }

        val version = runCatching {
            app.packageManager.getPackageInfo(app.packageName, 0).versionName ?: "?"
        }.getOrDefault("?")
        _uiState.update { it.copy(appVersion = version) }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    phoneNumber = repo.getString(keyDestNumb, defPhoneNumber),
                    startCmd = repo.getString(keyStartCmd, defStartCmd),
                    stopCmd = repo.getString(keyStopCmd, defStopCmd),
                    tempCmd = repo.getString(keyTempCmd, defTempCmd),
                    summerCmd = repo.getString(keySummerCmd, defSummerCmd),
                    winterCmd = repo.getString(keyWinterCmd, defWinterCmd),
                    statusCmd = repo.getString(keyStatusCmd, defStatusCmd),
                    heatDurationCmd = repo.getString(keyHeatDurationCmd, defHeatDurationCmd),
                    heatDuration = repo.getString(keyHeatDuration, defHeatDuration),
                    smsFeedbackCmd = repo.getString(keySmsFeedbackCmd, defSmsFeedbackCmd),
                    maxSmsCount = repo.getString(keyMaxSmsCount, defMaxSmsCount),
                    prepaidCredit = repo.getString(keyPrepaidCredit, defPrepaidCredit),
                    smsCost = repo.getString(keySmsCost, defSmsCost),
                )
            }
            if (isPhoneNumberCorrect(_uiState.value.phoneNumber)) {
                repo.setBoolean(keySendBtnEnabled, true)
            }
        }
    }

    // Per-field auto-save setters. UI calls these from the
    // ModalBottomSheet's "Save" tap — validation has already happened
    // there, but each setter re-checks defensively so the persisted
    // value never bypasses the invariants.

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

    fun setStartCmd(v: String) = persistString(keyStartCmd, v) { copy(startCmd = v) }
    fun setStopCmd(v: String) = persistString(keyStopCmd, v) { copy(stopCmd = v) }
    fun setTempCmd(v: String) = persistString(keyTempCmd, v) { copy(tempCmd = v) }
    fun setSummerCmd(v: String) = persistString(keySummerCmd, v) { copy(summerCmd = v) }
    fun setWinterCmd(v: String) = persistString(keyWinterCmd, v) { copy(winterCmd = v) }
    fun setStatusCmd(v: String) = persistString(keyStatusCmd, v) { copy(statusCmd = v) }
    fun setHeatDurationCmd(v: String) = persistString(keyHeatDurationCmd, v) { copy(heatDurationCmd = v) }
    fun setSmsFeedbackCmd(v: String) = persistString(keySmsFeedbackCmd, v) { copy(smsFeedbackCmd = v) }

    fun setMaxSmsCount(v: String) {
        val count = v.toIntOrNull() ?: 0
        if (count > 0 && count < 16000 && v.length <= 5) {
            _uiState.update { it.copy(maxSmsCount = v) }
            viewModelScope.launch { repo.setString(keyMaxSmsCount, v) }
        }
    }

    fun setPrepaidCredit(v: String) {
        val n = v.toFloatOrNull() ?: 0f
        if (n > 0f && n < Float.MAX_VALUE) {
            _uiState.update { it.copy(prepaidCredit = v) }
            viewModelScope.launch { repo.setString(keyPrepaidCredit, v) }
        }
    }

    fun setSmsCost(v: String) {
        val n = v.toFloatOrNull() ?: 0f
        if (n > 0f && n < Float.MAX_VALUE) {
            _uiState.update { it.copy(smsCost = v) }
            viewModelScope.launch { repo.setString(keySmsCost, v) }
        }
    }

    fun setHeatDuration(v: String) {
        val count = v.toIntOrNull() ?: 0
        if (count > 0 && count < 999 && v.length <= 3) {
            _uiState.update {
                it.copy(
                    heatDuration = v,
                    confirmDialog = ConfirmAction.HeatDurationSave,
                )
            }
        }
    }

    private inline fun persistString(
        key: String,
        v: String,
        crossinline updater: ConfigUiState.() -> ConfigUiState,
    ) {
        _uiState.update(updater)
        viewModelScope.launch { repo.setString(key, v) }
    }

    fun onSmsFeedbackToggleClick() {
        _uiState.update { it.copy(confirmDialog = ConfirmAction.FeedbackToggle) }
    }

    fun confirmFeedbackToggle() {
        viewModelScope.launch {
            val newValue = !_uiState.value.smsFeedback
            repo.setBoolean(keySmsFeedback, newValue)
            val msg = repo.getString(keySmsFeedbackCmd, defSmsFeedbackCmd) + if (newValue) "ON" else "OFF"
            sendHeaterCommand(
                context = app,
                repo = repo,
                msg = msg,
                destNumbKey = keyDestNumb,
                sendBtnEnabledKey = keySendBtnEnabled,
                smsCounterKey = keySmsCounter,
                maxCountKey = keyMaxSmsCount,
                maxCountDefault = defMaxSmsCount,
                lastCommandKey = keyLastCommand,
                lastCommandTimestampKey = keyLastCommandTimestamp,
                commandLabel = "Feedback",
                onError = { text ->
                    _uiState.update {
                        it.copy(
                            errorDialog = DialogState(
                                title = app.getString(R.string.com_cnfgErrTitle),
                                text = text,
                            ),
                        )
                    }
                },
                errorNoDest = app.getString(R.string.com_cnfgErrTxt),
                errorLimitPrefix = app.getString(R.string.com_smsCounterExceed),
                errorSendFailed = app.getString(R.string.com_sendFailed),
            )
            _uiState.update { it.copy(confirmDialog = null) }
        }
    }

    fun cancelFeedbackToggle() {
        _uiState.update { it.copy(confirmDialog = null) }
    }

    fun confirmHeatDurationSave() {
        viewModelScope.launch {
            val padded = _uiState.value.heatDuration.padStart(3, '0')
            repo.setString(keyHeatDuration, padded)
            val msg = repo.getString(keyHeatDurationCmd, defHeatDurationCmd) + padded
            sendHeaterCommand(
                context = app,
                repo = repo,
                msg = msg,
                destNumbKey = keyDestNumb,
                sendBtnEnabledKey = keySendBtnEnabled,
                smsCounterKey = keySmsCounter,
                maxCountKey = keyMaxSmsCount,
                maxCountDefault = defMaxSmsCount,
                lastCommandKey = keyLastCommand,
                lastCommandTimestampKey = keyLastCommandTimestamp,
                commandLabel = "Timer",
                onError = { text ->
                    _uiState.update {
                        it.copy(
                            errorDialog = DialogState(
                                title = app.getString(R.string.com_cnfgErrTitle),
                                text = text,
                            ),
                        )
                    }
                },
                errorNoDest = app.getString(R.string.com_cnfgErrTxt),
                errorLimitPrefix = app.getString(R.string.com_smsCounterExceed),
                errorSendFailed = app.getString(R.string.com_sendFailed),
            )
            _uiState.update { it.copy(heatDuration = padded, confirmDialog = null) }
        }
    }

    fun cancelHeatDurationSave() {
        viewModelScope.launch {
            val stored = repo.getString(keyHeatDuration, defHeatDuration)
            _uiState.update { it.copy(heatDuration = stored, confirmDialog = null) }
        }
    }

    fun onGpsTrackerToggle(checked: Boolean) {
        _uiState.update {
            it.copy(
                gpsTrackerTab = checked,
                errorDialog = DialogState(
                    title = app.getString(R.string.com_warning),
                    text = app.getString(R.string.com_Restartwarning),
                ),
            )
        }
        viewModelScope.launch {
            repo.setBoolean(keyGpsTrackerTab, checked)
        }
    }

    fun dismissErrorDialog() {
        _uiState.update { it.copy(errorDialog = null) }
    }

    fun resetSmsCounter() {
        viewModelScope.launch {
            repo.setInt(keySmsCounter, 0)
            Toast.makeText(
                app,
                app.getString(R.string.com_reset_txt),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    fun openRestoreConfirm() {
        _uiState.update { it.copy(restoreConfirmOpen = true) }
    }

    fun dismissRestoreConfirm() {
        _uiState.update { it.copy(restoreConfirmOpen = false) }
    }

    fun restoreDefaults() {
        viewModelScope.launch {
            // Legacy invariant: preserve the SMS counter through the
            // wipe, then force the two send-button-enabled flags off so
            // the user must re-enter a phone number before any further
            // SMS can be triggered.
            val counter = repo.getInt(keySmsCounter, 0)
            repo.clearAll()
            repo.setInt(keySmsCounter, counter)
            repo.setBoolean(keySendBtnEnabled, false)
            repo.setBoolean(keySendBtnEnabledGPS, false)
            _uiState.update { it.copy(restoreConfirmOpen = false) }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MyApp)
                ConfigViewModel(app.settingsRepository, app)
            }
        }
    }
}
