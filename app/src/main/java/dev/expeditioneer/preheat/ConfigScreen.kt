package dev.expeditioneer.preheat

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

private enum class ConfigField(
    @StringRes val labelRes: Int,
    val keyboardType: KeyboardType = KeyboardType.Text,
) {
    PhoneNumber(R.string.cfg_tvphonenumber, KeyboardType.Phone),
    StartCmd(R.string.cfg_tvstartcmd),
    StopCmd(R.string.cfg_tvstopcmd),
    TempCmd(R.string.cfg_tvtempcmd),
    SummerCmd(R.string.cfg_tvsummercmd),
    WinterCmd(R.string.cfg_tvwintercmd),
    StatusCmd(R.string.cfg_tvstatuscmd),
    HeatDurationCmd(R.string.cfg_tvheatdurationcmd),
    HeatDuration(R.string.cfg_tvheatduration, KeyboardType.Number),
    SmsFeedbackCmd(R.string.cfg_tvSMSfeedbackcmd),
    MaxSmsCount(R.string.cfg_tvMaxSMScount, KeyboardType.Number),
    PrepaidCredit(R.string.cfg_tvPrepaidCredit, KeyboardType.Decimal),
    SmsCost(R.string.cfg_tvSMScost, KeyboardType.Decimal);

    fun validate(value: String): Boolean = when (this) {
        PhoneNumber -> isPhoneNumberCorrect(value)
        MaxSmsCount -> (value.toIntOrNull() ?: 0) in 1..15999 && value.length <= 5
        PrepaidCredit, SmsCost -> {
            val n = value.toFloatOrNull() ?: 0f
            n > 0f && n < Float.MAX_VALUE
        }
        HeatDuration -> {
            val n = value.toIntOrNull() ?: 0
            n in 1..998 && value.length <= 3
        }
        else -> value.isNotEmpty()
    }

    fun valueOf(uiState: ConfigUiState): String = when (this) {
        PhoneNumber -> uiState.phoneNumber
        StartCmd -> uiState.startCmd
        StopCmd -> uiState.stopCmd
        TempCmd -> uiState.tempCmd
        SummerCmd -> uiState.summerCmd
        WinterCmd -> uiState.winterCmd
        StatusCmd -> uiState.statusCmd
        HeatDurationCmd -> uiState.heatDurationCmd
        HeatDuration -> uiState.heatDuration
        SmsFeedbackCmd -> uiState.smsFeedbackCmd
        MaxSmsCount -> uiState.maxSmsCount
        PrepaidCredit -> uiState.prepaidCredit
        SmsCost -> uiState.smsCost
    }

    fun commit(viewModel: ConfigViewModel, value: String) {
        when (this) {
            PhoneNumber -> viewModel.setPhoneNumber(value)
            StartCmd -> viewModel.setStartCmd(value)
            StopCmd -> viewModel.setStopCmd(value)
            TempCmd -> viewModel.setTempCmd(value)
            SummerCmd -> viewModel.setSummerCmd(value)
            WinterCmd -> viewModel.setWinterCmd(value)
            StatusCmd -> viewModel.setStatusCmd(value)
            HeatDurationCmd -> viewModel.setHeatDurationCmd(value)
            HeatDuration -> viewModel.setHeatDuration(value)
            SmsFeedbackCmd -> viewModel.setSmsFeedbackCmd(value)
            MaxSmsCount -> viewModel.setMaxSmsCount(value)
            PrepaidCredit -> viewModel.setPrepaidCredit(value)
            SmsCost -> viewModel.setSmsCost(value)
        }
    }

    fun supportingText(): String? = when (this) {
        PhoneNumber -> "z.B. +49 1234 567890"
        MaxSmsCount -> "Bereich: 1 – 15 999"
        PrepaidCredit, SmsCost -> "Dezimalzahl, z.B. 5.50"
        HeatDuration -> "Sekunden, 1 – 998"
        else -> null
    }
}

@Composable
internal fun ConfigScreen(
    viewModel: ConfigViewModel = viewModel(factory = ConfigViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsState()
    val sendPermission = rememberSendSmsPermission()
    var editing by remember { mutableStateOf<ConfigField?>(null) }

    val okButton = stringResource(R.string.com_pos_btn)
    val cancelButton = stringResource(R.string.com_cancelbutton)
    val warningTitle = stringResource(R.string.com_warning)
    val smsSendWarning = stringResource(R.string.com_SMSsendwarning)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Section(title = stringResource(R.string.ui_section_connection)) {
            SettingRow(
                label = stringResource(R.string.cfg_tvphonenumber),
                value = uiState.phoneNumber,
                onClick = { editing = ConfigField.PhoneNumber },
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.cfb_cbGPStracker)) },
                trailingContent = {
                    Switch(
                        checked = uiState.gpsTrackerTab,
                        onCheckedChange = viewModel::onGpsTrackerToggle,
                    )
                },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            )
        }

        Section(title = stringResource(R.string.ui_section_heater_commands)) {
            SettingRow(stringResource(R.string.cfg_tvstartcmd), uiState.startCmd) { editing = ConfigField.StartCmd }
            HorizontalDivider()
            SettingRow(stringResource(R.string.cfg_tvstopcmd), uiState.stopCmd) { editing = ConfigField.StopCmd }
            HorizontalDivider()
            SettingRow(stringResource(R.string.cfg_tvtempcmd), uiState.tempCmd) { editing = ConfigField.TempCmd }
            HorizontalDivider()
            SettingRow(stringResource(R.string.cfg_tvsummercmd), uiState.summerCmd) { editing = ConfigField.SummerCmd }
            HorizontalDivider()
            SettingRow(stringResource(R.string.cfg_tvwintercmd), uiState.winterCmd) { editing = ConfigField.WinterCmd }
            HorizontalDivider()
            SettingRow(stringResource(R.string.cfg_tvstatuscmd), uiState.statusCmd) { editing = ConfigField.StatusCmd }
        }

        Section(title = stringResource(R.string.ui_section_heating_timer)) {
            SettingRow(stringResource(R.string.cfg_tvheatdurationcmd), uiState.heatDurationCmd) { editing = ConfigField.HeatDurationCmd }
            HorizontalDivider()
            SettingRow(stringResource(R.string.cfg_tvheatduration), uiState.heatDuration) { editing = ConfigField.HeatDuration }
        }

        Section(title = stringResource(R.string.ui_section_limits_billing)) {
            SettingRow(stringResource(R.string.cfg_tvMaxSMScount), uiState.maxSmsCount) { editing = ConfigField.MaxSmsCount }
            HorizontalDivider()
            SettingRow(stringResource(R.string.cfg_tvPrepaidCredit), uiState.prepaidCredit) { editing = ConfigField.PrepaidCredit }
            HorizontalDivider()
            SettingRow(stringResource(R.string.cfg_tvSMScost), uiState.smsCost) { editing = ConfigField.SmsCost }
        }

        Section(title = stringResource(R.string.ui_section_notifications)) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.cfg_tvSMSfeedback)) },
                trailingContent = {
                    Switch(
                        checked = uiState.smsFeedback,
                        onCheckedChange = { viewModel.onSmsFeedbackToggleClick() },
                    )
                },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            )
            HorizontalDivider()
            SettingRow(stringResource(R.string.cfg_tvSMSfeedbackcmd), uiState.smsFeedbackCmd) { editing = ConfigField.SmsFeedbackCmd }
        }

        Section(title = stringResource(R.string.ui_section_reset)) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.ui_reset_sms_counter)) },
                supportingContent = {
                    Text(stringResource(R.string.ui_reset_sms_counter_supporting, uiState.smsCounter))
                },
                leadingContent = {
                    Icon(Icons.Default.RestartAlt, contentDescription = null)
                },
                modifier = Modifier.clickable(onClick = viewModel::resetSmsCounter),
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.com_restoredfltbutton)) },
                supportingContent = {
                    Text(stringResource(R.string.ui_reset_defaults_supporting))
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                },
                modifier = Modifier.clickable(onClick = viewModel::openRestoreConfirm),
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    headlineColor = MaterialTheme.colorScheme.error,
                ),
            )
        }

        Section(title = stringResource(R.string.ui_section_about)) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.ui_about_version)) },
                supportingContent = { Text(uiState.appVersion) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.ui_about_author)) },
                supportingContent = { Text(stringResource(R.string.authorname)) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            )
        }
    }

    editing?.let { field ->
        EditTextBottomSheet(
            title = stringResource(field.labelRes),
            initialValue = field.valueOf(uiState),
            keyboardType = field.keyboardType,
            validate = field::validate,
            supportingText = field.supportingText(),
            onConfirm = { newValue ->
                field.commit(viewModel, newValue)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }

    uiState.errorDialog?.let { dialog ->
        AlertDialog(
            onDismissRequest = viewModel::dismissErrorDialog,
            confirmButton = {
                TextButton(onClick = viewModel::dismissErrorDialog) { Text(okButton) }
            },
            title = { Text(dialog.title) },
            text = { Text(dialog.text) },
        )
    }

    if (uiState.restoreConfirmOpen) {
        AlertDialog(
            onDismissRequest = viewModel::dismissRestoreConfirm,
            title = { Text(stringResource(R.string.com_warning)) },
            text = { Text(stringResource(R.string.com_restore_warning)) },
            confirmButton = {
                TextButton(onClick = viewModel::restoreDefaults) {
                    Text(stringResource(R.string.com_restorebutton))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissRestoreConfirm) { Text(cancelButton) }
            },
        )
    }

    when (uiState.confirmDialog) {
        ConfirmAction.FeedbackToggle -> AlertDialog(
            onDismissRequest = viewModel::cancelFeedbackToggle,
            title = { Text(warningTitle) },
            text = { Text(smsSendWarning) },
            confirmButton = {
                TextButton(onClick = { sendPermission.guard(viewModel::confirmFeedbackToggle) }) { Text(okButton) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelFeedbackToggle) { Text(cancelButton) }
            },
        )
        ConfirmAction.HeatDurationSave -> AlertDialog(
            onDismissRequest = viewModel::cancelHeatDurationSave,
            title = { Text(warningTitle) },
            text = { Text(smsSendWarning) },
            confirmButton = {
                TextButton(onClick = { sendPermission.guard(viewModel::confirmHeatDurationSave) }) { Text(okButton) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelHeatDurationSave) { Text(cancelButton) }
            },
        )
        null -> Unit
    }
}

@Composable
private fun Section(
    title: String,
    content: @Composable () -> Unit,
) {
    SectionLabel(title)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column { content() }
    }
}

@Composable
private fun SettingRow(
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(label) },
        supportingContent = if (value.isNotEmpty()) ({ Text(value) }) else null,
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    )
}

@Preview(showBackground = true, heightDp = 1600)
@Composable
private fun ConfigScreenPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Config preview (ViewModel not available)")
        }
    }
}
