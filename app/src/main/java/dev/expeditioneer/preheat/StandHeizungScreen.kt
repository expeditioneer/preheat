package dev.expeditioneer.preheat

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
internal fun StandHeizungScreen(
    viewModel: StandHeizungViewModel = viewModel(factory = StandHeizungViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsState()
    val sendPermission = rememberSendSmsPermission()

    // Resolve string keys / defaults for the send actions
    val keyStartCmd = stringResource(R.string.sp_startCmd)
    val keyStopCmd = stringResource(R.string.sp_stopCmd)
    val keyTempCmd = stringResource(R.string.sp_tempCmd)
    val keySummerCmd = stringResource(R.string.sp_summerCmd)
    val keyWinterCmd = stringResource(R.string.sp_winterCmd)
    val keyStatusCmd = stringResource(R.string.sp_statusCmd)
    val defStartCmd = stringResource(R.string.cfg_startcmd)
    val defStopCmd = stringResource(R.string.cfg_stopcmd)
    val defTempCmd = stringResource(R.string.cfg_tempcmd)
    val defSummerCmd = stringResource(R.string.cfg_summercmd)
    val defWinterCmd = stringResource(R.string.cfg_wintercmd)
    val defStatusCmd = stringResource(R.string.cfg_statuscmd)
    val okButton = stringResource(R.string.com_pos_btn)

    Column(modifier = Modifier.fillMaxSize()) {
        // Determinate-less progress under the top app bar, visible only
        // while a send is in flight. Drops out of the layout when idle.
        if (uiState.isSending) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        } else {
            Spacer(Modifier.height(4.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            StatusCard(uiState = uiState)

            Spacer(Modifier.height(24.dp))

            // Primary actions: Start + Stop, prominent and equal-weight.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PrimaryAction(
                    icon = Icons.Default.PlayArrow,
                    label = stringResource(R.string.sh_startbutton),
                    enabled = uiState.sendEnabled,
                    modifier = Modifier.weight(1f),
                ) { sendPermission.guard { viewModel.send(keyStartCmd, defStartCmd, "Start") } }
                PrimaryAction(
                    icon = Icons.Default.Stop,
                    label = stringResource(R.string.sh_stopbutton),
                    enabled = uiState.sendEnabled,
                    modifier = Modifier.weight(1f),
                ) { sendPermission.guard { viewModel.send(keyStopCmd, defStopCmd, "Stop") } }
            }

            SectionLabel(stringResource(R.string.ui_section_mode))
            // SingleChoice segmented row: the selected segment marks the
            // most recently sent mode command. This is a local
            // approximation — the heater itself does not echo its state.
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = uiState.lastMode == "Summer",
                    onClick = {
                        sendPermission.guard { viewModel.send(keySummerCmd, defSummerCmd, "Summer") }
                    },
                    enabled = uiState.sendEnabled,
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = {
                        Icon(
                            Icons.Default.LightMode,
                            contentDescription = null,
                            modifier = Modifier.size(SegmentedButtonDefaults.IconSize),
                        )
                    },
                    label = { Text(stringResource(R.string.sh_summerbutton)) },
                )
                SegmentedButton(
                    selected = uiState.lastMode == "Winter",
                    onClick = {
                        sendPermission.guard { viewModel.send(keyWinterCmd, defWinterCmd, "Winter") }
                    },
                    enabled = uiState.sendEnabled,
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = {
                        Icon(
                            Icons.Default.AcUnit,
                            contentDescription = null,
                            modifier = Modifier.size(SegmentedButtonDefaults.IconSize),
                        )
                    },
                    label = { Text(stringResource(R.string.sh_winterbutton)) },
                )
            }

            SectionLabel(stringResource(R.string.ui_section_queries))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SecondaryAction(
                    icon = Icons.Default.Thermostat,
                    label = stringResource(R.string.sh_tempbutton),
                    enabled = uiState.sendEnabled,
                    modifier = Modifier.weight(1f),
                ) { sendPermission.guard { viewModel.send(keyTempCmd, defTempCmd, "Temp") } }
                SecondaryAction(
                    icon = Icons.Default.HelpOutline,
                    label = stringResource(R.string.sh_statusbutton),
                    enabled = uiState.sendEnabled,
                    modifier = Modifier.weight(1f),
                ) { sendPermission.guard { viewModel.send(keyStatusCmd, defStatusCmd, "Status") } }
            }
        }
    }

    uiState.infoDialog?.let { dialog ->
        WelcomeSheet(
            title = dialog.title,
            text = dialog.text,
            onDismiss = viewModel::dismissInfoDialog,
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WelcomeSheet(
    title: String,
    text: String,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.com_pos_btn))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StatusCard(uiState: StandHeizungUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val statusIcon: ImageVector
            val statusText: String
            when {
                uiState.isSending -> {
                    statusIcon = Icons.Default.HourglassTop
                    statusText = stringResource(R.string.ui_status_sending)
                }
                uiState.sendEnabled -> {
                    statusIcon = Icons.Default.CheckCircle
                    statusText = stringResource(R.string.ui_status_ready)
                }
                else -> {
                    statusIcon = Icons.Default.WarningAmber
                    statusText = stringResource(R.string.ui_status_not_configured)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.size(12.dp))
                Column {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (uiState.phoneNumber.isNotEmpty() && uiState.phoneNumber != "0") {
                        Text(
                            text = "→ ${uiState.phoneNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (uiState.lastCommand.isNotEmpty() && uiState.lastCommandTimestamp > 0L) {
                Spacer(Modifier.height(8.dp))
                val relative = DateUtils.getRelativeTimeSpanString(
                    uiState.lastCommandTimestamp,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                ).toString()
                Text(
                    text = "${stringResource(R.string.ui_last_command_prefix)} ${uiState.lastCommand} · $relative",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                StatPill(
                    label = stringResource(R.string.com_smsCounter),
                    value = uiState.counter.toString(),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.size(12.dp))
                StatPill(
                    label = stringResource(R.string.com_PrepaidCreditTitle),
                    value = uiState.prepaidCredit,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun PrimaryAction(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(96.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SecondaryAction(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(64.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.size(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
        )
    }
}

@Preview(showBackground = true, heightDp = 720)
@Composable
private fun StandHeizungScreenPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("StandHeizung preview (ViewModel not available)")
        }
    }
}
