package dev.expeditioneer.preheat

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

private enum class GpsField(
    @StringRes val labelRes: Int,
    val keyboardType: KeyboardType = KeyboardType.Text,
) {
    PhoneNumber(R.string.cfg_tvphonenumberGPS, KeyboardType.Phone),
    PosReqCmd(R.string.cfg_tvPosReqCmd);

    fun validate(value: String): Boolean = when (this) {
        PhoneNumber -> isPhoneNumberCorrect(value)
        PosReqCmd -> value.isNotEmpty()
    }

    fun valueOf(uiState: GPStrackerUiState): String = when (this) {
        PhoneNumber -> uiState.phoneNumber
        PosReqCmd -> uiState.posReqCmd
    }

    fun commit(viewModel: GPStrackerViewModel, value: String) = when (this) {
        PhoneNumber -> viewModel.setPhoneNumber(value)
        PosReqCmd -> viewModel.setPosReqCmd(value)
    }
}

@Composable
internal fun GPStrackerScreen(
    viewModel: GPStrackerViewModel = viewModel(factory = GPStrackerViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsState()
    var editing by remember { mutableStateOf<GpsField?>(null) }
    val okButton = stringResource(R.string.com_pos_btn)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        StatusCard(uiState = uiState)

        Spacer(Modifier.height(24.dp))

        SectionLabel(text = stringResource(R.string.ui_section_action))
        ExtendedFloatingActionButton(
            onClick = { if (uiState.sendEnabled) viewModel.onRequestClicked() },
            icon = {
                Icon(Icons.Default.MyLocation, contentDescription = null)
            },
            text = { Text(stringResource(R.string.gt_GPSposReqBtn)) },
            expanded = true,
            containerColor = if (uiState.sendEnabled) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = if (uiState.sendEnabled) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            elevation = FloatingActionButtonDefaults.elevation(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = viewModel::showHelp) {
                Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(6.dp))
                Text(stringResource(R.string.sh_helpbutton))
            }
        }

        SectionLabel(text = stringResource(R.string.ui_section_setup))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Column {
                SettingRow(
                    label = stringResource(R.string.cfg_tvphonenumberGPS),
                    value = uiState.phoneNumber,
                    onClick = { editing = GpsField.PhoneNumber },
                )
                HorizontalDivider()
                SettingRow(
                    label = stringResource(R.string.cfg_tvPosReqCmd),
                    value = uiState.posReqCmd,
                    onClick = { editing = GpsField.PosReqCmd },
                )
            }
        }
    }

    editing?.let { field ->
        EditTextBottomSheet(
            title = stringResource(field.labelRes),
            initialValue = field.valueOf(uiState),
            keyboardType = field.keyboardType,
            validate = field::validate,
            onConfirm = { newValue ->
                field.commit(viewModel, newValue)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }

    uiState.helpDialog?.let { dialog ->
        AlertDialog(
            onDismissRequest = viewModel::dismissHelpDialog,
            confirmButton = {
                TextButton(onClick = viewModel::dismissHelpDialog) { Text(okButton) }
            },
            title = { Text(dialog.title) },
            text = { Text(dialog.text) },
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

@Composable
private fun StatusCard(uiState: GPStrackerUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val statusIcon: ImageVector
            val statusText: String
            if (uiState.sendEnabled) {
                statusIcon = Icons.Default.CheckCircle
                statusText = stringResource(R.string.ui_status_ready)
            } else {
                statusIcon = Icons.Default.WarningAmber
                statusText = stringResource(R.string.ui_status_not_configured)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.size(12.dp))
                Text(statusText, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.sh_tvgpstracker),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingRow(label: String, value: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        supportingContent = if (value.isNotEmpty()) ({ Text(value) }) else null,
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    )
}

@Preview(showBackground = true, heightDp = 720)
@Composable
private fun GPStrackerScreenPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("GPStracker preview (ViewModel not available)")
        }
    }
}
