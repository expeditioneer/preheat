package dev.expeditioneer.preheat

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SmsManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

internal data class DialogState(val title: String, val text: String)

/**
 * Material 3 section header used across the screens. Consistent
 * vertical padding ensures the gap between content blocks reads the
 * same on Heater, GPS-Tracker, and Config tabs.
 */
@Composable
internal fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 8.dp),
    )
}

/**
 * Runtime guard for the SEND_SMS permission. Wraps the lambda passed to
 * its `guard` method in a permission check: if the permission is
 * already granted the lambda runs immediately, otherwise the system
 * permission dialog is shown and the lambda is invoked on grant. On
 * denial the lambda is dropped.
 */
internal class SendSmsPermission internal constructor(
    private val isGranted: () -> Boolean,
    private val request: (() -> Unit) -> Unit,
) {
    fun guard(action: () -> Unit) {
        if (isGranted()) action() else request(action)
    }
}

/**
 * Reusable Material 3 modal bottom sheet for editing a single text
 * value. Holds the draft locally, gates the Save button on
 * `validate(draft)`, and forwards the confirmed value to onConfirm.
 * imePadding + navigationBarsPadding keep the sheet usable with the
 * soft keyboard under edge-to-edge.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditTextBottomSheet(
    title: String,
    initialValue: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    validate: (String) -> Boolean = { it.isNotEmpty() },
    supportingText: String? = null,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    var draft by remember { mutableStateOf(initialValue) }
    val isValid = validate(draft)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .imePadding()
                .navigationBarsPadding(),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                isError = !isValid,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                singleLine = true,
                supportingText = supportingText?.let { hint -> { Text(hint) } },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.com_cancelbutton)) }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onConfirm(draft) },
                    enabled = isValid,
                ) { Text(stringResource(R.string.com_pos_btn)) }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
internal fun rememberSendSmsPermission(): SendSmsPermission {
    val context = LocalContext.current
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val action = pendingAction
        pendingAction = null
        if (granted) action?.invoke()
    }
    return remember(context, launcher) {
        SendSmsPermission(
            isGranted = {
                context.checkSelfPermission(Manifest.permission.SEND_SMS) ==
                    PackageManager.PERMISSION_GRANTED
            },
            request = { action ->
                pendingAction = action
                launcher.launch(Manifest.permission.SEND_SMS)
            },
        )
    }
}

/**
 * Reject "0", empty, common single-char garbage, and anything with a
 * closing parenthesis. Used by the destination-number focus-change
 * handler and the save flow.
 */
internal fun isPhoneNumberCorrect(number: String): Boolean = when (number) {
    "0", "", ";", ",", ".", ":", "/", "(" -> false
    else -> !number.contains(")")
}

/**
 * Send an SMS with the configured command text to the configured
 * destination, gated on a per-month SMS budget. The counter is
 * incremented on success; configuration errors are reported via the
 * onError callback so the caller can surface them in the UI.
 */
@Suppress("LongParameterList")
internal suspend fun sendHeaterCommand(
    context: Context,
    repo: SettingsRepository,
    msg: String,
    destNumbKey: String,
    sendBtnEnabledKey: String,
    smsCounterKey: String,
    maxCountKey: String,
    maxCountDefault: String,
    lastCommandKey: String,
    lastCommandTimestampKey: String,
    commandLabel: String,
    lastModeKey: String? = null,
    onError: (String) -> Unit,
    errorNoDest: String,
    errorLimitPrefix: String,
    errorSendFailed: String,
) {
    val destNumb = repo.getString(destNumbKey, "0")

    // Optimistic: re-enable buttons before the validation step so the
    // UI does not get stuck disabled on the error paths below.
    repo.setBoolean(sendBtnEnabledKey, true)

    val maxCountStr = repo.getString(maxCountKey, maxCountDefault)
    val maxNum = maxCountStr.toIntOrNull() ?: 0
    val counter = repo.getInt(smsCounterKey, 0)

    when {
        destNumb == "0" -> {
            onError(errorNoDest)
            repo.setBoolean(sendBtnEnabledKey, false)
        }
        maxNum != 0 && counter >= maxNum -> {
            onError("$errorLimitPrefix $maxCountStr")
            repo.setBoolean(sendBtnEnabledKey, false)
        }
        else -> {
            val sent = try {
                val smsManager = context.getSystemService(SmsManager::class.java)
                smsManager.sendTextMessage(destNumb, null, msg, null, null)
                true
            } catch (e: SecurityException) {
                onError("$errorSendFailed (${e.javaClass.simpleName})")
                false
            } catch (e: IllegalArgumentException) {
                onError("$errorSendFailed (${e.javaClass.simpleName})")
                false
            }
            if (sent) {
                repo.setInt(smsCounterKey, counter + 1)
                repo.setString(lastCommandKey, commandLabel)
                repo.setLong(lastCommandTimestampKey, System.currentTimeMillis())
                if (lastModeKey != null) {
                    repo.setString(lastModeKey, commandLabel)
                }
            } else {
                repo.setBoolean(sendBtnEnabledKey, false)
            }
        }
    }
}
