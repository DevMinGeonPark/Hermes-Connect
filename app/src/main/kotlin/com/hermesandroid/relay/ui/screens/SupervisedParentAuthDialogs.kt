package com.hermesandroid.relay.ui.screens

import android.content.res.Resources
import android.content.ClipData
import android.content.Intent
import android.content.ClipboardManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import com.hermesandroid.relay.R
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hermesandroid.relay.data.SupervisedParentAuthResult
import com.hermesandroid.relay.data.SupervisedParentAuthStore
import com.hermesandroid.relay.data.SupervisedParentAuthenticator
import com.hermesandroid.relay.data.SupervisedParentCredentialType
import com.hermesandroid.relay.data.SupervisedParentEnrollment
import com.hermesandroid.relay.data.SupervisedParentSecretError
import kotlinx.coroutines.launch

@Composable
internal fun SupervisedParentVerifyDialog(
    store: SupervisedParentAuthenticator,
    onDismiss: () -> Unit,
    onVerified: () -> Unit,
    onUseRecoveryCode: () -> Unit,
) {
    val resources = LocalResources.current
    val storedType by store.credentialTypeFlow.collectAsState(initial = null)
    var selectedLegacyType by remember { mutableStateOf<SupervisedParentCredentialType?>(null) }
    val inputType = storedType?.takeUnless { it == SupervisedParentCredentialType.Legacy }
        ?: selectedLegacyType
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun verify(candidateText: String) {
        if (busy) return
        busy = true
        scope.launch {
            val candidate = candidateText.toCharArray()
            val result = try {
                store.verify(candidate)
            } finally {
                candidate.fill('\u0000')
            }
            busy = false
            when (result) {
                SupervisedParentAuthResult.Success -> onVerified()
                else -> error = result.toUserMessage(resources)
            }
        }
    }

    ParentAuthDialogSurface(
        step = null,
        onBack = if (storedType == SupervisedParentCredentialType.Legacy && inputType != null) {
            { selectedLegacyType = null; error = null }
        } else {
            onDismiss
        },
    ) {
        when (inputType) {
            SupervisedParentCredentialType.Pin -> PinEntryScreen(
                title = stringResource(R.string.supervised_parent_pin),
                subtitle = stringResource(R.string.supervised_enter_pin),
                busy = busy,
                error = error,
                onComplete = ::verify,
                onUseRecovery = onUseRecoveryCode,
            )
            SupervisedParentCredentialType.Password -> PasswordVerifyScreen(
                busy = busy,
                error = error,
                onSubmit = ::verify,
                onUseRecovery = onUseRecoveryCode,
            )
            else -> CredentialChoiceScreen(
                title = stringResource(R.string.supervised_legacy_credential_choice),
                subtitle = stringResource(R.string.supervised_legacy_credential_summary),
                onSelected = { selectedLegacyType = it },
            )
        }
    }
}

@Composable
internal fun SupervisedParentSetupDialog(
    store: SupervisedParentAuthenticator,
    currentSecretRequired: Boolean,
    onDismiss: () -> Unit,
    onEnrolled: (SupervisedParentEnrollment) -> Unit,
) {
    val resources = LocalResources.current
    val storedType by store.credentialTypeFlow.collectAsState(initial = null)
    var stage by remember(currentSecretRequired) {
        mutableStateOf(if (currentSecretRequired) SetupStage.VerifyCurrent else SetupStage.Choose)
    }
    var legacyInputType by remember { mutableStateOf<SupervisedParentCredentialType?>(null) }
    var currentSecret by remember { mutableStateOf("") }
    var credentialType by remember { mutableStateOf<SupervisedParentCredentialType?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun enroll(newSecretText: String) {
        val type = credentialType ?: return
        busy = true
        scope.launch {
            val current = currentSecret.toCharArray()
            val replacement = newSecretText.toCharArray()
            val result = try {
                if (currentSecretRequired) store.change(current, replacement, type)
                else store.enroll(replacement, type)
            } finally {
                current.fill('\u0000')
                replacement.fill('\u0000')
            }
            busy = false
            result.fold(onSuccess = onEnrolled, onFailure = { error = it.toUserMessage(resources) })
        }
    }

    fun verifyCurrent(candidateText: String) {
        busy = true
        scope.launch {
            val candidate = candidateText.toCharArray()
            val result = try { store.verify(candidate) } finally { candidate.fill('\u0000') }
            busy = false
            if (result == SupervisedParentAuthResult.Success) {
                currentSecret = candidateText
                error = null
                stage = SetupStage.Choose
            } else {
                error = result.toUserMessage(resources)
            }
        }
    }

    val backAction: () -> Unit = when (stage) {
        SetupStage.VerifyCurrent, SetupStage.Choose -> onDismiss
        SetupStage.Pin, SetupStage.Password -> {
            { stage = SetupStage.Choose; credentialType = null; error = null }
        }
    }
    val step = when (stage) {
        SetupStage.VerifyCurrent -> 1 to 3
        SetupStage.Choose -> if (currentSecretRequired) 2 to 3 else 1 to 2
        SetupStage.Pin, SetupStage.Password -> if (currentSecretRequired) 3 to 3 else 2 to 2
    }

    ParentAuthDialogSurface(step = step, onBack = backAction) {
        when (stage) {
            SetupStage.VerifyCurrent -> {
                val inputType = storedType?.takeUnless { it == SupervisedParentCredentialType.Legacy }
                    ?: legacyInputType
                when (inputType) {
                    SupervisedParentCredentialType.Pin -> PinEntryScreen(
                        title = stringResource(R.string.supervised_current_pin),
                        subtitle = stringResource(R.string.supervised_confirm_before_change),
                        busy = busy,
                        error = error,
                        onComplete = ::verifyCurrent,
                    )
                    SupervisedParentCredentialType.Password -> PasswordVerifyScreen(
                        title = stringResource(R.string.supervised_current_password),
                        busy = busy,
                        error = error,
                        onSubmit = ::verifyCurrent,
                    )
                    else -> CredentialChoiceScreen(
                        title = stringResource(R.string.supervised_current_credential_choice),
                        subtitle = stringResource(R.string.supervised_current_credential_summary),
                        onSelected = { legacyInputType = it },
                    )
                }
            }
            SetupStage.Choose -> CredentialChoiceScreen(
                title = if (currentSecretRequired) stringResource(R.string.supervised_choose_new_access) else stringResource(R.string.supervised_choose_access),
                subtitle = stringResource(R.string.supervised_choose_access_summary),
                onSelected = {
                    credentialType = it
                    stage = if (it == SupervisedParentCredentialType.Pin) SetupStage.Pin else SetupStage.Password
                },
            )
            SetupStage.Pin -> PinSetupScreen(
                busy = busy,
                error = error,
                onComplete = ::enroll,
            )
            SetupStage.Password -> PasswordSetupScreen(
                busy = busy,
                error = error,
                onComplete = ::enroll,
            )
        }
    }
}

@Composable
internal fun SupervisedParentRecoveryDialog(
    store: SupervisedParentAuthenticator,
    onDismiss: () -> Unit,
    onReset: (SupervisedParentEnrollment) -> Unit,
) {
    val resources = LocalResources.current
    var stage by remember { mutableStateOf(RecoveryStage.Phrase) }
    var recoveryPhrase by remember { mutableStateOf("") }
    var credentialType by remember { mutableStateOf<SupervisedParentCredentialType?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun reset(newSecretText: String) {
        val type = credentialType ?: return
        busy = true
        scope.launch {
            val recovery = recoveryPhrase.toCharArray()
            val replacement = newSecretText.toCharArray()
            val result = try {
                store.resetWithRecoveryPhrase(recovery, replacement, type)
            } finally {
                recovery.fill('\u0000')
                replacement.fill('\u0000')
            }
            busy = false
            result.fold(onSuccess = onReset, onFailure = { error = it.toUserMessage(resources) })
        }
    }

    val step = when (stage) {
        RecoveryStage.Phrase -> 1 to 3
        RecoveryStage.Choose -> 2 to 3
        RecoveryStage.Pin, RecoveryStage.Password -> 3 to 3
    }
    ParentAuthDialogSurface(
        step = step,
        onBack = when (stage) {
            RecoveryStage.Phrase -> onDismiss
            RecoveryStage.Choose -> ({ stage = RecoveryStage.Phrase })
            RecoveryStage.Pin, RecoveryStage.Password -> ({ stage = RecoveryStage.Choose })
        },
    ) {
        when (stage) {
            RecoveryStage.Phrase -> RecoveryPhraseInputScreen(
                value = recoveryPhrase,
                error = error,
                onValueChange = { recoveryPhrase = it; error = null },
                onContinue = { stage = RecoveryStage.Choose },
            )
            RecoveryStage.Choose -> CredentialChoiceScreen(
                title = stringResource(R.string.supervised_choose_new_access),
                subtitle = stringResource(R.string.supervised_recovery_replaced),
                onSelected = {
                    credentialType = it
                    stage = if (it == SupervisedParentCredentialType.Pin) RecoveryStage.Pin
                    else RecoveryStage.Password
                },
            )
            RecoveryStage.Pin -> PinSetupScreen(busy = busy, error = error, onComplete = ::reset)
            RecoveryStage.Password -> PasswordSetupScreen(busy = busy, error = error, onComplete = ::reset)
        }
    }
}

@Composable
internal fun SupervisedParentRecoveryCodeDialog(
    enrollment: SupervisedParentEnrollment,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val clipboard = remember(context) {
        context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
    }
    ParentAuthDialogSurface(step = 3 to 3, onBack = null) {
        SupervisedParentRecoveryCodeContent(
            enrollment = enrollment,
            onShare = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, enrollment.recoveryPhrase)
                }
                context.startActivity(Intent.createChooser(intent, resources.getString(R.string.supervised_share_recovery)))
            },
            onCopy = {
                clipboard.setPrimaryClip(
                    ClipData.newPlainText(resources.getString(R.string.supervised_recovery_clipboard), enrollment.recoveryPhrase),
                )
            },
            onDone = onDone,
        )
    }
}

@Composable
private fun ParentAuthDialogSurface(
    step: Pair<Int, Int>?,
    onBack: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = { onBack?.invoke() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        ParentAuthScreenSurface(step = step, onBack = onBack, content = content)
    }
}

@Composable
internal fun ParentAuthScreenSurface(
    step: Pair<Int, Int>?,
    onBack: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(64.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.supervised_back))
                    }
                } else {
                    Spacer(Modifier.size(48.dp))
                }
                step?.let { (current, total) ->
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        repeat(total) { index ->
                            Box(
                                Modifier
                                    .padding(horizontal = 3.dp)
                                    .size(width = 46.dp, height = 4.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (index < current) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant,
                                    ),
                            )
                        }
                    }
                    Text(
                        stringResource(R.string.supervised_step, current, total),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } ?: Spacer(Modifier.weight(1f))
            }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
                content()
            }
        }
    }
}

@Composable
internal fun CredentialChoiceScreen(
    title: String,
    subtitle: String,
    onSelected: (SupervisedParentCredentialType) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AuthHeading(title, subtitle)
        Spacer(Modifier.height(28.dp))
        CredentialChoiceRow(
            icon = { Icon(Icons.Filled.Dialpad, contentDescription = null) },
            title = stringResource(R.string.supervised_use_pin),
            subtitle = stringResource(R.string.supervised_pin_summary),
            onClick = { onSelected(SupervisedParentCredentialType.Pin) },
        )
        Spacer(Modifier.height(12.dp))
        CredentialChoiceRow(
            icon = { Icon(Icons.Filled.Lock, contentDescription = null) },
            title = stringResource(R.string.supervised_use_password),
            subtitle = stringResource(R.string.supervised_password_summary),
            onClick = { onSelected(SupervisedParentCredentialType.Password) },
        )
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.supervised_distinct_choices),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CredentialChoiceRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) { icon() }
            }
            Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("›", fontSize = 30.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun PinEntryScreen(
    title: String,
    subtitle: String,
    busy: Boolean,
    error: String?,
    onComplete: (String) -> Unit,
    onUseRecovery: (() -> Unit)? = null,
) {
    var pin by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AuthHeading(title, subtitle)
        Spacer(Modifier.height(28.dp))
        PinDots(pin.length)
        Spacer(Modifier.height(26.dp))
        NumericKeypad(
            enabled = !busy,
            onDigit = { digit ->
                if (pin.length < 6) {
                    val next = pin + digit
                    pin = next
                    if (next.length == 6) onComplete(next)
                }
            },
            onBackspace = { if (pin.isNotEmpty()) pin = pin.dropLast(1) },
        )
        AuthError(error)
        onUseRecovery?.let {
            TextButton(enabled = !busy, onClick = it) { Text(stringResource(R.string.supervised_use_recovery)) }
        }
    }
}

@Composable
internal fun PinSetupScreen(
    busy: Boolean,
    error: String?,
    onComplete: (String) -> Unit,
) {
    val resources = LocalResources.current
    var firstPin by remember { mutableStateOf<String?>(null) }
    var pin by remember(firstPin) { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AuthHeading(
            if (firstPin == null) stringResource(R.string.supervised_create_pin) else stringResource(R.string.supervised_confirm_pin),
            if (firstPin == null) stringResource(R.string.supervised_choose_pin) else stringResource(R.string.supervised_repeat_pin),
        )
        Spacer(Modifier.height(28.dp))
        PinDots(pin.length)
        Spacer(Modifier.height(26.dp))
        NumericKeypad(
            enabled = !busy,
            onDigit = { digit ->
                if (pin.length < 6) {
                    val next = pin + digit
                    pin = next
                    if (next.length == 6) {
                        if (firstPin == null) {
                            firstPin = next
                        } else if (firstPin == next) {
                            onComplete(next)
                        } else {
                            localError = resources.getString(R.string.supervised_pin_mismatch)
                            firstPin = null
                        }
                    }
                }
            },
            onBackspace = { if (pin.isNotEmpty()) pin = pin.dropLast(1) },
        )
        AuthError(localError ?: error)
    }
}

@Composable
private fun NumericKeypad(
    enabled: Boolean,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
) {
    val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { digit -> KeypadButton(digit, enabled) { onDigit(digit) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Spacer(Modifier.size(width = 92.dp, height = 58.dp))
            KeypadButton("0", enabled) { onDigit("0") }
            Surface(
                modifier = Modifier.size(width = 92.dp, height = 58.dp).clickable(enabled = enabled, onClick = onBackspace),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = stringResource(R.string.supervised_delete_digit))
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(width = 92.dp, height = 58.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Text(label, style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
private fun PinDots(count: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(6) { index ->
            Box(
                Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .then(
                        if (index < count) Modifier.background(MaterialTheme.colorScheme.primary)
                        else Modifier.border(2.dp, MaterialTheme.colorScheme.outline, CircleShape),
                    ),
            )
        }
    }
}

@Composable
internal fun PasswordSetupScreen(
    busy: Boolean,
    error: String?,
    onComplete: (String) -> Unit,
) {
    val resources = LocalResources.current
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var reveal by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AuthHeading(stringResource(R.string.supervised_create_password), stringResource(R.string.supervised_password_length))
        Spacer(Modifier.height(28.dp))
        PasswordField(stringResource(R.string.supervised_password), password, { password = it; localError = null }, reveal, { reveal = !reveal })
        Spacer(Modifier.height(12.dp))
        PasswordField(stringResource(R.string.supervised_confirm_password), confirmation, { confirmation = it; localError = null }, reveal, { reveal = !reveal }, ImeAction.Done)
        AuthError(localError ?: error)
        Spacer(Modifier.height(20.dp))
        Button(
            enabled = !busy && password.isNotEmpty() && confirmation.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            onClick = {
                when {
                    password != confirmation -> localError = resources.getString(R.string.supervised_password_mismatch)
                    !SupervisedParentAuthStore.validateNewSecret(
                        password.toCharArray(),
                        SupervisedParentCredentialType.Password,
                    ).valid -> localError = resources.getString(R.string.supervised_password_minimum)
                    else -> onComplete(password)
                }
            },
        ) { Text(if (busy) stringResource(R.string.supervised_saving) else stringResource(R.string.supervised_continue)) }
    }
}

@Composable
internal fun PasswordVerifyScreen(
    title: String = stringResource(R.string.supervised_parent_password),
    busy: Boolean,
    error: String?,
    onSubmit: (String) -> Unit,
    onUseRecovery: (() -> Unit)? = null,
) {
    var password by remember { mutableStateOf("") }
    var reveal by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AuthHeading(title, stringResource(R.string.supervised_enter_password))
        Spacer(Modifier.height(28.dp))
        PasswordField(stringResource(R.string.supervised_password), password, { password = it }, reveal, { reveal = !reveal }, ImeAction.Done)
        AuthError(error)
        Spacer(Modifier.height(20.dp))
        Button(
            enabled = !busy && password.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            onClick = { onSubmit(password) },
        ) { Text(if (busy) stringResource(R.string.supervised_checking) else stringResource(R.string.supervised_unlock)) }
        onUseRecovery?.let {
            TextButton(enabled = !busy, onClick = it) { Text(stringResource(R.string.supervised_use_recovery)) }
        }
    }
}

@Composable
private fun PasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    reveal: Boolean,
    onReveal: () -> Unit,
    imeAction: ImeAction = ImeAction.Next,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= 64) onValueChange(it) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        trailingIcon = {
            IconButton(onClick = onReveal) {
                Icon(
                    if (reveal) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (reveal) stringResource(R.string.supervised_hide_password) else stringResource(R.string.supervised_show_password),
                )
            }
        },
        singleLine = true,
    )
}

@Composable
private fun RecoveryPhraseInputScreen(
    value: String,
    error: String?,
    onValueChange: (String) -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AuthHeading(stringResource(R.string.supervised_enter_recovery), stringResource(R.string.supervised_recovery_six_words))
        Spacer(Modifier.height(28.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.supervised_recovery_phrase)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done),
            minLines = 2,
        )
        AuthError(error)
        Spacer(Modifier.height(20.dp))
        Button(
            enabled = value.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            onClick = onContinue,
        ) { Text(stringResource(R.string.supervised_continue)) }
    }
}

@Composable
internal fun SupervisedParentRecoveryCodeContent(
    enrollment: SupervisedParentEnrollment,
    onShare: () -> Unit = {},
    onCopy: () -> Unit = {},
    onDone: () -> Unit = {},
) {
    val words = enrollment.recoveryPhrase.split('-')
    val displayPhrase = if (words.size == 6) {
        words.take(3).joinToString("-") + "\n" + words.drop(3).joinToString("-")
    } else {
        enrollment.recoveryPhrase
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AuthHeading(
            stringResource(R.string.supervised_save_recovery),
            stringResource(R.string.supervised_recovery_only_way),
        )
        Spacer(Modifier.height(28.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            SelectionContainer {
                Text(
                    displayPhrase,
                    modifier = Modifier.padding(20.dp),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium.copy(lineHeight = 28.sp),
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(
            stringResource(R.string.supervised_recovery_cleanup),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            modifier = Modifier.fillMaxWidth().height(52.dp),
            onClick = onShare,
        ) {
            Icon(Icons.Filled.Share, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.supervised_share))
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            modifier = Modifier.fillMaxWidth().height(52.dp),
            onClick = onCopy,
        ) { Text(stringResource(R.string.supervised_copy_phrase)) }
        TextButton(onClick = onDone) { Text(stringResource(R.string.supervised_done)) }
    }
}

@Composable
private fun AuthHeading(title: String, subtitle: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AuthError(error: String?) {
    error?.let {
        Spacer(Modifier.height(14.dp))
        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun SupervisedParentAuthResult.toUserMessage(resources: Resources): String = when (this) {
    SupervisedParentAuthResult.Success -> ""
    is SupervisedParentAuthResult.Invalid -> if (attemptsBeforeDelay > 0) {
        resources.getString(R.string.supervised_credential_attempts, attemptsBeforeDelay)
    } else {
        resources.getString(R.string.supervised_credential_incorrect)
    }
    is SupervisedParentAuthResult.Throttled -> {
        val seconds = ((retryAfterMillis + 999L) / 1_000L).coerceAtLeast(1)
        resources.getString(R.string.supervised_too_many_attempts, seconds)
    }
    SupervisedParentAuthResult.Missing -> resources.getString(R.string.supervised_access_missing)
    SupervisedParentAuthResult.Corrupt -> resources.getString(R.string.supervised_parent_data_locked)
}

private fun Throwable.toUserMessage(resources: Resources): String = when (this) {
    is SupervisedParentAuthStore.ParentSecretValidationException -> resources.getString(
        when (validation.error) {
            SupervisedParentSecretError.TooLong -> R.string.supervised_maximum_length
            SupervisedParentSecretError.InvalidPin -> R.string.supervised_exact_pin
            SupervisedParentSecretError.InvalidPassword -> R.string.supervised_password_minimum
            null -> R.string.supervised_new_credential_invalid
        },
    )
    is IllegalArgumentException -> resources.getString(R.string.supervised_new_credential_invalid)
    is SupervisedParentAuthStore.ParentAuthenticationException -> authResult.toUserMessage(resources)
    else -> resources.getString(R.string.supervised_update_failed)
}

private enum class SetupStage { VerifyCurrent, Choose, Pin, Password }
private enum class RecoveryStage { Phrase, Choose, Pin, Password }
