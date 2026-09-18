package com.hermesandroid.relay.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.hermesandroid.relay.R
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hermesandroid.relay.data.AgentDisplay
import com.hermesandroid.relay.data.Profile
import com.hermesandroid.relay.data.SupervisedAttachmentCategory
import com.hermesandroid.relay.data.SupervisedModePolicy
import com.hermesandroid.relay.data.SupervisedParentAuthStatus
import com.hermesandroid.relay.data.SupervisedParentAuthStore
import com.hermesandroid.relay.data.SupervisedParentEnrollment
import com.hermesandroid.relay.data.SupervisedSessionActions
import com.hermesandroid.relay.data.SupervisedVisibilityPreset
import com.hermesandroid.relay.ui.components.avatar.LocalAvailablePets
import com.hermesandroid.relay.ui.components.avatar.SphereAvatar
import com.hermesandroid.relay.ui.theme.AppThemes
import com.hermesandroid.relay.ui.theme.ThemeMode
import com.hermesandroid.relay.ui.theme.appearanceShapeScale
import com.hermesandroid.relay.ui.mayEnableSupervisedMode
import com.hermesandroid.relay.ui.theme.LocalBrand
import com.hermesandroid.relay.ui.theme.appearanceRoundedCornerShape
import com.hermesandroid.relay.ui.theme.gradientBorder
import com.hermesandroid.relay.viewmodel.ConnectionViewModel
import kotlinx.coroutines.launch

/**
 * The settings surface available while supervised mode is locked.
 *
 * This is a separate allowlisted composition rather than a filtered copy of
 * [SettingsScreen]. New full-settings categories therefore stay unavailable
 * until they are deliberately added here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupervisedSettingsScreen(
    connectionViewModel: ConnectionViewModel,
    policy: SupervisedModePolicy,
    onPolicyChange: (SupervisedModePolicy) -> Unit,
    onBack: (() -> Unit)?,
    onNavigateToAppearance: () -> Unit,
    onParentAccessGranted: () -> Unit,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val activeConnection by connectionViewModel.activeConnection.collectAsState()
    val effectiveProfile by connectionViewModel.effectiveDisplayProfile.collectAsState()
    val profileAlias by connectionViewModel.profileDisplayAlias.collectAsState()
    val isDarkTheme = LocalBrand.current.isDark
    val parentAuthStore = remember(context) { SupervisedParentAuthStore(context) }
    val parentAuthStatus by produceState<SupervisedParentAuthStatus?>(
        initialValue = null,
        key1 = parentAuthStore,
    ) {
        parentAuthStore.statusFlow.collect { value = it }
    }
    var authError by remember { mutableStateOf<String?>(null) }
    var parentAuthDialog by remember { mutableStateOf<ParentAuthDialog?>(null) }
    var pendingEnrollment by remember { mutableStateOf<SupervisedParentEnrollment?>(null) }
    var showAbout by remember { mutableStateOf(false) }

    fun requestParentAccess() {
        when (parentAuthStatus) {
            SupervisedParentAuthStatus.Configured -> parentAuthDialog = ParentAuthDialog.Verify
            SupervisedParentAuthStatus.Missing -> {
                authError = resources.getString(R.string.supervised_legacy_locked)
            }
            SupervisedParentAuthStatus.Corrupt -> {
                authError = resources.getString(R.string.supervised_parent_data_locked)
            }
            null -> authError = resources.getString(R.string.supervised_parent_loading)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.supervised_back))
                        }
                    }
                },
                title = { Text(stringResource(R.string.supervised_settings)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val pinnedProfile = effectiveProfile?.takeIf {
                it.name.equals(policy.pinnedProfileName, ignoreCase = true)
            }
            val agentName = AgentDisplay.profileDisplayName(pinnedProfile)
                ?: profileAlias?.takeIf { pinnedProfile != null }
                ?: policy.pinnedProfileName?.let(::profileLabel)
                ?: stringResource(R.string.supervised_chat_unavailable)
            SupervisedSummaryCard(
                agentName = agentName,
                connectionLabel = activeConnection?.label,
                policy = policy,
                isDarkTheme = isDarkTheme,
            )

            SupervisedSectionLabel(stringResource(R.string.supervised_appearance))
            SupervisedNavigationRow(
                icon = Icons.Filled.Palette,
                title = stringResource(R.string.supervised_appearance),
                subtitle = stringResource(R.string.supervised_appearance_summary),
                onClick = onNavigateToAppearance,
                isDarkTheme = isDarkTheme,
            )

            SupervisedSectionLabel(stringResource(R.string.supervised_help))
            SupervisedNavigationRow(
                icon = Icons.Filled.Info,
                title = stringResource(R.string.supervised_about_title),
                subtitle = stringResource(R.string.supervised_about_summary),
                onClick = { showAbout = true },
                isDarkTheme = isDarkTheme,
            )

            SupervisedSectionLabel(stringResource(R.string.supervised_parent))
            SupervisedNavigationRow(
                icon = Icons.Filled.Lock,
                title = stringResource(R.string.supervised_parent_access),
                subtitle = stringResource(R.string.supervised_parent_unlock_summary),
                onClick = ::requestParentAccess,
                isDarkTheme = isDarkTheme,
            )
            authError?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text(stringResource(R.string.supervised_app_name)) },
            text = {
                Text(
                    stringResource(R.string.supervised_about_body),
                )
            },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) { Text(stringResource(R.string.supervised_close)) }
            },
        )
    }

    when (parentAuthDialog) {
        ParentAuthDialog.Verify -> SupervisedParentVerifyDialog(
            store = parentAuthStore,
            onDismiss = { parentAuthDialog = null },
            onVerified = {
                parentAuthDialog = null
                authError = null
                onParentAccessGranted()
            },
            onUseRecoveryCode = { parentAuthDialog = ParentAuthDialog.Recovery },
        )
        ParentAuthDialog.Recovery -> SupervisedParentRecoveryDialog(
            store = parentAuthStore,
            onDismiss = { parentAuthDialog = null },
            onReset = { enrollment ->
                parentAuthDialog = null
                pendingEnrollment = enrollment
            },
        )
        else -> Unit
    }
    pendingEnrollment?.let { enrollment ->
        SupervisedParentRecoveryCodeDialog(
            enrollment = enrollment,
            onDone = {
                pendingEnrollment = null
                authError = null
                onParentAccessGranted()
            },
        )
    }
}

/** Restricted appearance editor backed by the supervised policy, not global theme settings. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupervisedAppearanceSettingsScreen(
    connectionViewModel: ConnectionViewModel,
    policy: SupervisedModePolicy,
    onPolicyChange: (SupervisedModePolicy) -> Unit,
    onBack: () -> Unit,
) {
    val isDarkTheme = LocalBrand.current.isDark
    val appearanceShape by connectionViewModel.appearanceShape.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.supervised_back))
                    }
                },
                title = { Text(stringResource(R.string.supervised_appearance)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.supervised_theme_scope),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SupervisedCard(isDarkTheme) {
                SupervisedThemeControls(policy, onPolicyChange, appearanceShape)
            }

            if (
                policy.appearance.allowProfileIconChanges ||
                policy.appearance.allowBackgroundChanges
            ) {
                SupervisedSectionLabel(stringResource(R.string.supervised_agent_look))
                SupervisedCard(isDarkTheme) {
                    SupervisedAgentLookControls(
                        connectionViewModel = connectionViewModel,
                        allowProfileIconChanges = policy.appearance.allowProfileIconChanges,
                        allowBackgroundChanges = policy.appearance.allowBackgroundChanges,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** Parent-only editor for the client-side supervised policy. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupervisedControlsScreen(
    connectionViewModel: ConnectionViewModel,
    policy: SupervisedModePolicy,
    profiles: List<Profile>,
    onPolicyChange: (SupervisedModePolicy) -> Unit,
    onBack: () -> Unit,
    onReturnToSupervisedView: () -> Unit,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val parentAuthStore = remember(context) { SupervisedParentAuthStore(context) }
    val parentAuthStatus by produceState<SupervisedParentAuthStatus?>(
        initialValue = null,
        key1 = parentAuthStore,
    ) {
        parentAuthStore.statusFlow.collect { value = it }
    }
    val isDarkTheme = LocalBrand.current.isDark
    val appearanceShape by connectionViewModel.appearanceShape.collectAsState()
    var showProfilePicker by remember { mutableStateOf(false) }
    var sessionActionsExpanded by remember { mutableStateOf(false) }
    var enableAuthError by remember { mutableStateOf<String?>(null) }
    var parentAuthDialog by remember { mutableStateOf<ParentAuthDialog?>(null) }
    var pendingEnrollment by remember { mutableStateOf<SupervisedParentEnrollment?>(null) }
    var enableAfterEnrollment by remember { mutableStateOf(false) }
    var showRemoveCredentialConfirm by remember { mutableStateOf(false) }
    var removeCredentialBusy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun requestFirstEnable() {
        if (!policy.isConfigured) {
            enableAuthError = resources.getString(R.string.supervised_choose_profile_first)
            return
        }
        when (parentAuthStatus) {
            SupervisedParentAuthStatus.Missing -> {
                enableAfterEnrollment = true
                parentAuthDialog = ParentAuthDialog.Setup
            }
            SupervisedParentAuthStatus.Configured -> parentAuthDialog = ParentAuthDialog.Verify
            SupervisedParentAuthStatus.Corrupt -> {
                enableAuthError = resources.getString(R.string.supervised_parent_data_reset)
            }
            null -> enableAuthError = resources.getString(R.string.supervised_parent_loading)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.supervised_back))
                    }
                },
                title = { Text(stringResource(R.string.supervised_mode)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SupervisedCard(isDarkTheme) {
                SupervisedSwitchRow(
                    title = stringResource(R.string.supervised_enable),
                    subtitle = when {
                        policy.pinnedProfileName.isNullOrBlank() ->
                            stringResource(R.string.supervised_choose_profile_hint)
                        parentAuthStatus == SupervisedParentAuthStatus.Missing ->
                            stringResource(R.string.supervised_set_credential_hint)
                        else ->
                            stringResource(R.string.supervised_approved_surfaces)
                    },
                    checked = policy.enabled,
                    enabled = policy.enabled ||
                        (!policy.pinnedProfileName.isNullOrBlank() &&
                            parentAuthStatus in setOf(
                                SupervisedParentAuthStatus.Missing,
                                SupervisedParentAuthStatus.Configured,
                            )),
                    onCheckedChange = { enabled ->
                        if (enabled) requestFirstEnable()
                        else onPolicyChange(policy.copy(enabled = false))
                    },
                )
                enableAuthError?.let { message ->
                    Text(
                        message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                HorizontalDivider()
                SupervisedValueRow(
                    title = stringResource(R.string.supervised_agent_profile),
                    value = policy.pinnedProfileName?.let(::profileLabel) ?: stringResource(R.string.supervised_choose_profile),
                    onClick = { showProfilePicker = true },
                )
            }

            if (policy.enabled) {
                SupervisedNavigationRow(
                    icon = Icons.Filled.Lock,
                    title = stringResource(R.string.supervised_return),
                    subtitle = stringResource(R.string.supervised_return_summary),
                    onClick = onReturnToSupervisedView,
                    isDarkTheme = isDarkTheme,
                )
            }

            Text(
                stringResource(R.string.supervised_client_boundary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.supervised_credential_boundary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SupervisedSectionLabel(stringResource(R.string.supervised_appearance_title))
            SupervisedCard(isDarkTheme) {
                SupervisedThemeControls(policy, onPolicyChange, appearanceShape)
                HorizontalDivider()
                SupervisedSwitchRow(
                    title = stringResource(R.string.supervised_show_pet),
                    subtitle = stringResource(R.string.supervised_show_pet_summary),
                    checked = policy.appearance.showPet,
                    onCheckedChange = {
                        onPolicyChange(policy.copy(appearance = policy.appearance.copy(showPet = it)))
                    },
                )
                HorizontalDivider()
                SupervisedSwitchRow(
                    title = stringResource(R.string.supervised_allow_icon_changes),
                    subtitle = stringResource(R.string.supervised_icon_parent_override),
                    checked = policy.appearance.allowProfileIconChanges,
                    onCheckedChange = {
                        onPolicyChange(
                            policy.copy(
                                appearance = policy.appearance.copy(allowProfileIconChanges = it),
                            ),
                        )
                    },
                )
                HorizontalDivider()
                SupervisedSwitchRow(
                    title = stringResource(R.string.supervised_allow_background_changes),
                    subtitle = stringResource(R.string.supervised_background_parent_override),
                    checked = policy.appearance.allowBackgroundChanges,
                    onCheckedChange = {
                        onPolicyChange(
                            policy.copy(
                                appearance = policy.appearance.copy(allowBackgroundChanges = it),
                            ),
                        )
                    },
                )
            }

            SupervisedSectionLabel(stringResource(R.string.supervised_parent_set_look))
            SupervisedCard(isDarkTheme) {
                Text(
                    stringResource(R.string.supervised_parent_controls_remain),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SupervisedAgentLookControls(
                    connectionViewModel = connectionViewModel,
                    allowProfileIconChanges = true,
                    allowBackgroundChanges = true,
                )
            }

            SupervisedSectionLabel(stringResource(R.string.supervised_allowed_features))
            SupervisedCard(isDarkTheme) {
                SupervisedSwitchRow(stringResource(R.string.supervised_attachments), stringResource(R.string.supervised_attachments_summary), policy.capabilities.attachments) {
                    onPolicyChange(policy.copy(capabilities = policy.capabilities.copy(attachments = it)))
                }
                HorizontalDivider()
                SupervisedSwitchRow(stringResource(R.string.supervised_voice), stringResource(R.string.supervised_voice_summary), policy.capabilities.voice) {
                    onPolicyChange(policy.copy(capabilities = policy.capabilities.copy(voice = it)))
                }
                HorizontalDivider()
                SupervisedSwitchRow(stringResource(R.string.supervised_generated_images), stringResource(R.string.supervised_generated_images_summary), policy.capabilities.generatedImages) {
                    onPolicyChange(policy.copy(capabilities = policy.capabilities.copy(generatedImages = it)))
                }
                HorizontalDivider()
                SupervisedSwitchRow(stringResource(R.string.supervised_history), stringResource(R.string.supervised_history_summary), policy.capabilities.conversationHistory) {
                    onPolicyChange(policy.copy(capabilities = policy.capabilities.copy(conversationHistory = it)))
                }
                HorizontalDivider()
                SupervisedSwitchRow(stringResource(R.string.supervised_share_images), stringResource(R.string.supervised_share_images_summary), policy.capabilities.shareGeneratedImages) {
                    onPolicyChange(policy.copy(capabilities = policy.capabilities.copy(shareGeneratedImages = it)))
                }
                if (policy.capabilities.attachments) {
                    HorizontalDivider()
                    Text(stringResource(R.string.supervised_attachment_count), style = MaterialTheme.typography.titleSmall)
                    val countOptions = listOf(1, 2, 4, 8)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        countOptions.forEachIndexed { index, count ->
                            SegmentedButton(
                                selected = policy.capabilities.attachmentMaxCount == count,
                                onClick = {
                                    onPolicyChange(
                                        policy.copy(
                                            capabilities = policy.capabilities.copy(
                                                attachmentMaxCount = count,
                                            ),
                                        ),
                                    )
                                },
                                shape = SegmentedButtonDefaults.itemShape(index, countOptions.size),
                            ) { Text(count.toString()) }
                        }
                    }
                    Text(stringResource(R.string.supervised_attachment_size), style = MaterialTheme.typography.titleSmall)
                    val sizeOptions = listOf(5, 10, 25, 50)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        sizeOptions.forEachIndexed { index, sizeMb ->
                            SegmentedButton(
                                selected = policy.capabilities.attachmentMaxFileMb == sizeMb,
                                onClick = {
                                    onPolicyChange(
                                        policy.copy(
                                            capabilities = policy.capabilities.copy(
                                                attachmentMaxFileMb = sizeMb,
                                            ),
                                        ),
                                    )
                                },
                                shape = SegmentedButtonDefaults.itemShape(index, sizeOptions.size),
                            ) { Text(stringResource(R.string.supervised_size_mb, sizeMb)) }
                        }
                    }
                    Text(stringResource(R.string.supervised_attachment_types), style = MaterialTheme.typography.titleSmall)
                    SupervisedAttachmentCategory.entries.forEach { category ->
                        val enabled = category in policy.capabilities.attachmentCategories
                        SupervisedSwitchRow(
                            title = category.displayLabel(),
                            checked = enabled,
                            onCheckedChange = { checked ->
                                val updated = if (checked) {
                                    policy.capabilities.attachmentCategories + category
                                } else {
                                    policy.capabilities.attachmentCategories - category
                                }
                                if (updated.isNotEmpty()) {
                                    onPolicyChange(
                                        policy.copy(
                                            capabilities = policy.capabilities.copy(
                                                attachmentCategories = updated,
                                            ),
                                        ),
                                    )
                                }
                            },
                        )
                    }
                }
            }

            SupervisedSectionLabel(stringResource(R.string.supervised_conversation_actions))
            SupervisedCard(isDarkTheme) {
                CapabilitySwitch(stringResource(R.string.supervised_new_chat), policy.capabilities.newChat) {
                    onPolicyChange(policy.copy(capabilities = policy.capabilities.copy(newChat = it)))
                }
                CapabilitySwitch(stringResource(R.string.supervised_cancel_response), policy.capabilities.cancelResponse) {
                    onPolicyChange(policy.copy(capabilities = policy.capabilities.copy(cancelResponse = it)))
                }
                CapabilitySwitch(stringResource(R.string.supervised_steer_response), policy.capabilities.steerResponse) {
                    onPolicyChange(policy.copy(capabilities = policy.capabilities.copy(steerResponse = it)))
                }
                CapabilitySwitch(stringResource(R.string.supervised_retry_response), policy.capabilities.retryResponse) {
                    onPolicyChange(policy.copy(capabilities = policy.capabilities.copy(retryResponse = it)))
                }
                CapabilitySwitch(stringResource(R.string.supervised_copy_responses), policy.capabilities.copyResponses) {
                    onPolicyChange(policy.copy(capabilities = policy.capabilities.copy(copyResponses = it)))
                }
                CapabilitySwitch(stringResource(R.string.supervised_quote_replies), policy.capabilities.quoteReplies) {
                    onPolicyChange(policy.copy(capabilities = policy.capabilities.copy(quoteReplies = it)))
                }
                CapabilitySwitch(stringResource(R.string.supervised_edit_resend), policy.capabilities.editAndResend, divider = false) {
                    onPolicyChange(policy.copy(capabilities = policy.capabilities.copy(editAndResend = it)))
                }
            }

            SupervisedSectionLabel(stringResource(R.string.supervised_session_options))
            SupervisedCard(isDarkTheme) {
                val actions = policy.capabilities.sessionActions
                SupervisedValueRow(
                    title = stringResource(R.string.supervised_history_actions),
                    value = sessionActionsSummary(actions),
                    onClick = { sessionActionsExpanded = !sessionActionsExpanded },
                )
                Text(
                    if (policy.capabilities.conversationHistory) {
                        stringResource(R.string.supervised_history_actions_enabled)
                    } else {
                        stringResource(R.string.supervised_history_actions_disabled)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (sessionActionsExpanded) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = actions.allEnabled,
                            onClick = {
                                onPolicyChange(
                                    policy.copy(
                                        capabilities = policy.capabilities.copy(
                                            sessionActions = actions.withAll(true),
                                        ),
                                    ),
                                )
                            },
                            label = { Text(stringResource(R.string.supervised_allow_all)) },
                        )
                        FilterChip(
                            selected = actions.noneEnabled,
                            onClick = {
                                onPolicyChange(
                                    policy.copy(
                                        capabilities = policy.capabilities.copy(
                                            sessionActions = actions.withAll(false),
                                        ),
                                    ),
                                )
                            },
                            label = { Text(stringResource(R.string.supervised_allow_none)) },
                        )
                    }
                    SessionActionSwitch(stringResource(R.string.supervised_pin_unpin), actions.pin) {
                        onPolicyChange(policy.withSessionActions(actions.copy(pin = it)))
                    }
                    SessionActionSwitch(stringResource(R.string.supervised_rename), actions.rename) {
                        onPolicyChange(policy.withSessionActions(actions.copy(rename = it)))
                    }
                    SessionActionSwitch(stringResource(R.string.supervised_archive_restore), actions.archive) {
                        onPolicyChange(policy.withSessionActions(actions.copy(archive = it)))
                    }
                    SessionActionSwitch(stringResource(R.string.supervised_share_transcript), actions.shareTranscript) {
                        onPolicyChange(policy.withSessionActions(actions.copy(shareTranscript = it)))
                    }
                    SessionActionSwitch(stringResource(R.string.supervised_delete), actions.delete, divider = false) {
                        onPolicyChange(policy.withSessionActions(actions.copy(delete = it)))
                    }
                }
            }

            SupervisedSectionLabel(stringResource(R.string.supervised_visible_elements))
            SupervisedCard(isDarkTheme) {
                val presets = SupervisedVisibilityPreset.entries
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    presets.forEachIndexed { index, preset ->
                        SegmentedButton(
                            selected = policy.visibility.preset == preset,
                            onClick = {
                                onPolicyChange(policy.copy(visibility = policy.visibility.copy(preset = preset)))
                            },
                            shape = SegmentedButtonDefaults.itemShape(index, presets.size),
                        ) { Text(preset.displayLabel()) }
                    }
                }
                Text(
                    when (policy.visibility.preset) {
                        SupervisedVisibilityPreset.Simple -> stringResource(R.string.supervised_simple_summary)
                        SupervisedVisibilityPreset.Transparent -> stringResource(R.string.supervised_transparent_summary)
                        SupervisedVisibilityPreset.Custom -> stringResource(R.string.supervised_custom_summary)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (policy.visibility.preset == SupervisedVisibilityPreset.Custom) {
                    HorizontalDivider()
                    VisibilitySwitch(stringResource(R.string.supervised_agent_identity), policy.visibility.showAgentIdentity) {
                        onPolicyChange(policy.copy(visibility = policy.visibility.copy(showAgentIdentity = it)))
                    }
                    VisibilitySwitch(stringResource(R.string.supervised_model_name), policy.visibility.showModelName) {
                        onPolicyChange(policy.copy(visibility = policy.visibility.copy(showModelName = it)))
                    }
                    VisibilitySwitch(stringResource(R.string.supervised_profile_name), policy.visibility.showProfileName) {
                        onPolicyChange(policy.copy(visibility = policy.visibility.copy(showProfileName = it)))
                    }
                    VisibilitySwitch(stringResource(R.string.supervised_connection_status), policy.visibility.showConnectionStatus) {
                        onPolicyChange(policy.copy(visibility = policy.visibility.copy(showConnectionStatus = it)))
                    }
                    VisibilitySwitch(stringResource(R.string.supervised_technical_route), policy.visibility.showTechnicalRoute) {
                        onPolicyChange(policy.copy(visibility = policy.visibility.copy(showTechnicalRoute = it)))
                    }
                    VisibilitySwitch(stringResource(R.string.supervised_timestamps), policy.visibility.showTimestamps) {
                        onPolicyChange(policy.copy(visibility = policy.visibility.copy(showTimestamps = it)))
                    }
                    VisibilitySwitch(stringResource(R.string.supervised_working_status), policy.visibility.showWorkingStatus) {
                        onPolicyChange(policy.copy(visibility = policy.visibility.copy(showWorkingStatus = it)))
                    }
                    VisibilitySwitch(stringResource(R.string.supervised_tool_names), policy.visibility.showToolNames) {
                        onPolicyChange(policy.copy(visibility = policy.visibility.copy(showToolNames = it)))
                    }
                    VisibilitySwitch(stringResource(R.string.supervised_tool_details), policy.visibility.showToolDetails) {
                        onPolicyChange(policy.copy(visibility = policy.visibility.copy(showToolDetails = it)))
                    }
                    VisibilitySwitch(stringResource(R.string.supervised_reasoning), policy.visibility.showReasoning) {
                        onPolicyChange(policy.copy(visibility = policy.visibility.copy(showReasoning = it)))
                    }
                    VisibilitySwitch(stringResource(R.string.supervised_usage), policy.visibility.showUsage, divider = false) {
                        onPolicyChange(policy.copy(visibility = policy.visibility.copy(showUsage = it)))
                    }
                }
            }

            SupervisedSectionLabel(stringResource(R.string.supervised_parent_access))
            SupervisedCard(isDarkTheme) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(stringResource(R.string.supervised_app_credential), style = MaterialTheme.typography.titleSmall)
                        Text(
                            when (parentAuthStatus) {
                                SupervisedParentAuthStatus.Configured -> stringResource(R.string.supervised_credential_configured)
                                SupervisedParentAuthStatus.Missing -> stringResource(R.string.supervised_credential_missing)
                                SupervisedParentAuthStatus.Corrupt -> stringResource(R.string.supervised_credential_corrupt)
                                null -> stringResource(R.string.supervised_loading_access)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                when (parentAuthStatus) {
                    SupervisedParentAuthStatus.Missing -> OutlinedButton(
                        onClick = {
                            enableAfterEnrollment = false
                            parentAuthDialog = ParentAuthDialog.Setup
                        },
                    ) { Text(stringResource(R.string.supervised_set_credential)) }
                    SupervisedParentAuthStatus.Configured -> {
                        OutlinedButton(onClick = { parentAuthDialog = ParentAuthDialog.Change }) {
                            Text(stringResource(R.string.supervised_change_credential))
                        }
                        TextButton(onClick = { parentAuthDialog = ParentAuthDialog.Recovery }) {
                            Text(stringResource(R.string.supervised_reset_recovery))
                        }
                        TextButton(onClick = { showRemoveCredentialConfirm = true }) {
                            Text(
                                stringResource(R.string.supervised_remove_credential),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    else -> Unit
                }
                HorizontalDivider()
                SupervisedSwitchRow(
                    title = stringResource(R.string.supervised_relock_background),
                    subtitle = stringResource(R.string.supervised_shared_device_recommendation),
                    checked = policy.parentAccess.relockOnBackground,
                    onCheckedChange = {
                        onPolicyChange(
                            policy.copy(
                                parentAccess = policy.parentAccess.copy(relockOnBackground = it),
                            ),
                        )
                    },
                )
                HorizontalDivider()
                Text(stringResource(R.string.supervised_automatic_relock), style = MaterialTheme.typography.titleSmall)
                val timeoutOptions = listOf(1, 5, 15, 60)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    timeoutOptions.forEachIndexed { index, minutes ->
                        SegmentedButton(
                            selected = policy.parentAccess.timeoutMinutes == minutes,
                            onClick = {
                                onPolicyChange(
                                    policy.copy(
                                        parentAccess = policy.parentAccess.copy(
                                            timeoutMinutes = minutes,
                                        ),
                                    ),
                                )
                            },
                            shape = SegmentedButtonDefaults.itemShape(index, timeoutOptions.size),
                        ) { Text(if (minutes == 60) stringResource(R.string.supervised_one_hour) else stringResource(R.string.supervised_minutes, minutes)) }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showProfilePicker) {
        AlertDialog(
            onDismissRequest = { showProfilePicker = false },
            title = { Text(stringResource(R.string.supervised_choose_agent_profile)) },
            text = {
                Column {
                    profiles.forEach { profile ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onPolicyChange(policy.copy(pinnedProfileName = profile.name))
                                    showProfilePicker = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = policy.pinnedProfileName == profile.name,
                                onClick = null,
                            )
                            Text(AgentDisplay.profileDisplayName(profile) ?: profileLabel(profile.name))
                        }
                    }
                    if (profiles.isEmpty()) {
                        Text(stringResource(R.string.supervised_profiles_unavailable))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProfilePicker = false }) { Text(stringResource(R.string.supervised_close)) }
            },
        )
    }

    if (showRemoveCredentialConfirm) {
        RemoveParentCredentialDialog(
            busy = removeCredentialBusy,
            onDismiss = { showRemoveCredentialConfirm = false },
            onConfirm = {
                removeCredentialBusy = true
                scope.launch {
                    val result = parentAuthStore.clearCredentialAndDisablePolicies()
                    removeCredentialBusy = false
                    result.fold(
                        onSuccess = {
                            showRemoveCredentialConfirm = false
                            enableAuthError = null
                            onBack()
                        },
                        onFailure = {
                            enableAuthError = resources.getString(R.string.supervised_remove_failed)
                        },
                    )
                }
            },
        )
    }

    when (parentAuthDialog) {
        ParentAuthDialog.Verify -> SupervisedParentVerifyDialog(
            store = parentAuthStore,
            onDismiss = { parentAuthDialog = null },
            onVerified = {
                parentAuthDialog = null
                enableAuthError = null
                if (mayEnableSupervisedMode(policy, parentCredentialConfirmed = true)) {
                    onPolicyChange(policy.copy(enabled = true))
                }
            },
            onUseRecoveryCode = { parentAuthDialog = ParentAuthDialog.Recovery },
        )
        ParentAuthDialog.Setup -> SupervisedParentSetupDialog(
            store = parentAuthStore,
            currentSecretRequired = false,
            onDismiss = {
                parentAuthDialog = null
                enableAfterEnrollment = false
            },
            onEnrolled = { enrollment ->
                parentAuthDialog = null
                pendingEnrollment = enrollment
            },
        )
        ParentAuthDialog.Change -> SupervisedParentSetupDialog(
            store = parentAuthStore,
            currentSecretRequired = true,
            onDismiss = { parentAuthDialog = null },
            onEnrolled = { enrollment ->
                parentAuthDialog = null
                pendingEnrollment = enrollment
            },
        )
        ParentAuthDialog.Recovery -> SupervisedParentRecoveryDialog(
            store = parentAuthStore,
            onDismiss = { parentAuthDialog = null },
            onReset = { enrollment ->
                parentAuthDialog = null
                pendingEnrollment = enrollment
            },
        )
        null -> Unit
    }
    pendingEnrollment?.let { enrollment ->
        SupervisedParentRecoveryCodeDialog(
            enrollment = enrollment,
            onDone = {
                pendingEnrollment = null
                enableAuthError = null
                if (enableAfterEnrollment && mayEnableSupervisedMode(policy, parentCredentialConfirmed = true)) {
                    onPolicyChange(policy.copy(enabled = true))
                }
                enableAfterEnrollment = false
            },
        )
    }
}

@Composable
internal fun RemoveParentCredentialDialog(
    busy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(stringResource(R.string.supervised_remove_title)) },
        text = {
            Text(
                stringResource(R.string.supervised_remove_warning),
            )
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = onConfirm) {
                Text(
                    if (busy) stringResource(R.string.supervised_removing) else stringResource(R.string.supervised_remove),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(enabled = !busy, onClick = onDismiss) { Text(stringResource(R.string.supervised_cancel)) }
        },
    )
}

@Composable
private fun SupervisedSummaryCard(
    agentName: String,
    connectionLabel: String?,
    policy: SupervisedModePolicy,
    isDarkTheme: Boolean,
) {
    SupervisedCard(isDarkTheme) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.padding(start = 12.dp)) {
                Text(agentName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                connectionLabel?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        val features = buildList {
            if (policy.capabilities.attachments) add(stringResource(R.string.supervised_attachments))
            if (policy.capabilities.voice) add(stringResource(R.string.supervised_voice))
            if (policy.capabilities.generatedImages) add(stringResource(R.string.supervised_generated_images))
        }
        if (features.isNotEmpty()) {
            Text(
                features.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SupervisedCard(
    isDarkTheme: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .gradientBorder(
                shape = appearanceRoundedCornerShape(12.dp),
                isDarkTheme = isDarkTheme,
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
private fun SupervisedSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
    )
}

@Composable
private fun SupervisedNavigationRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isDarkTheme: Boolean,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .gradientBorder(
                shape = appearanceRoundedCornerShape(12.dp),
                isDarkTheme = isDarkTheme,
            )
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@Composable
private fun SupervisedSwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, enabled = enabled, onCheckedChange = null)
    }
}

@Composable
private fun CapabilitySwitch(
    title: String,
    checked: Boolean,
    divider: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    SupervisedSwitchRow(title = title, checked = checked, onCheckedChange = onCheckedChange)
    if (divider) HorizontalDivider()
}

@Composable
private fun VisibilitySwitch(
    title: String,
    checked: Boolean,
    divider: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) = CapabilitySwitch(title, checked, divider, onCheckedChange)

@Composable
private fun SupervisedValueRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
    }
}

@Composable
private fun SupervisedVisibilityPreset.displayLabel(): String = when (this) {
    SupervisedVisibilityPreset.Simple -> stringResource(R.string.supervised_simple)
    SupervisedVisibilityPreset.Transparent -> stringResource(R.string.supervised_transparent)
    SupervisedVisibilityPreset.Custom -> stringResource(R.string.supervised_custom)
}

private fun profileLabel(value: String): String = value
    .replace('_', ' ')
    .replace('-', ' ')
    .replaceFirstChar { it.uppercase() }

@Composable
private fun SupervisedAttachmentCategory.displayLabel(): String = when (this) {
    SupervisedAttachmentCategory.Images -> stringResource(R.string.supervised_images)
    SupervisedAttachmentCategory.Documents -> stringResource(R.string.supervised_documents)
    SupervisedAttachmentCategory.Audio -> stringResource(R.string.supervised_audio)
    SupervisedAttachmentCategory.Video -> stringResource(R.string.supervised_video)
}

private fun SupervisedModePolicy.withSessionActions(
    actions: SupervisedSessionActions,
): SupervisedModePolicy = copy(
    capabilities = capabilities.copy(sessionActions = actions),
)

@Composable
private fun sessionActionsSummary(actions: SupervisedSessionActions): String = when {
    actions.allEnabled -> stringResource(R.string.supervised_all_allowed)
    actions.noneEnabled -> stringResource(R.string.supervised_none_allowed)
    else -> stringResource(R.string.supervised_allowed_count, actions.enabledCount, SupervisedSessionActions.TOTAL)
}

private enum class ParentAuthDialog {
    Verify,
    Setup,
    Change,
    Recovery,
}

@Composable
private fun SessionActionSwitch(
    title: String,
    checked: Boolean,
    divider: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) = CapabilitySwitch(title, checked, divider, onCheckedChange)

@Composable
private fun SupervisedThemeControls(
    policy: SupervisedModePolicy,
    onPolicyChange: (SupervisedModePolicy) -> Unit,
    appearanceShapeId: String,
) {
    val selectedTheme = AppThemes.byId(policy.appearance.appThemeId)
    val previewDark = selectedTheme.resolveDark(
        policy.appearance.themePreference,
        isSystemInDarkTheme(),
    )

    AppearanceLivePreview(
        palette = selectedTheme.paletteFor(previewDark),
        shapeScale = appearanceShapeScale(appearanceShapeId),
        restricted = true,
    )

    Text(stringResource(R.string.supervised_theme), style = MaterialTheme.typography.titleSmall)
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppThemes.ALL.forEach { theme ->
            ThemeSwatchChip(
                appTheme = theme,
                selected = selectedTheme.id == theme.id,
                onClick = {
                    onPolicyChange(
                        policy.copy(
                            appearance = policy.appearance.copy(appThemeId = theme.id),
                        ),
                    )
                },
            )
        }
    }

    if (selectedTheme.mode == ThemeMode.BOTH) {
        val modeOptions = listOf("auto" to stringResource(R.string.supervised_system), "light" to stringResource(R.string.supervised_light), "dark" to stringResource(R.string.supervised_dark))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            modeOptions.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = policy.appearance.themePreference == option.first,
                    onClick = {
                        onPolicyChange(
                            policy.copy(
                                appearance = policy.appearance.copy(themePreference = option.first),
                            ),
                        )
                    },
                    shape = SegmentedButtonDefaults.itemShape(index, modeOptions.size),
                ) { Text(option.second) }
            }
        }
    } else {
        Text(
            if (selectedTheme.mode == ThemeMode.LIGHT_ONLY) stringResource(R.string.supervised_fixed_light) else stringResource(R.string.supervised_fixed_dark),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

}

@Composable
private fun SupervisedAgentLookControls(
    connectionViewModel: ConnectionViewModel,
    allowProfileIconChanges: Boolean,
    allowBackgroundChanges: Boolean,
) {
    val localProfileIcon by connectionViewModel.localProfileIcon.collectAsState()
    val backgroundEnabled by connectionViewModel.backgroundVisualizationEnabled.collectAsState()
    val backgroundAvatar by connectionViewModel.backgroundAvatar.collectAsState()
    val availableBackgrounds = LocalAvailablePets.current
    val iconPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(connectionViewModel::setProfileIcon)
    }

    if (allowProfileIconChanges) {
        Text(stringResource(R.string.supervised_agent_icon), style = MaterialTheme.typography.titleSmall)
        Text(
            if (localProfileIcon.isNullOrBlank()) {
                stringResource(R.string.supervised_profile_icon_current)
            } else {
                stringResource(R.string.supervised_profile_icon_local)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { iconPicker.launch("image/*") }) {
                Text(stringResource(R.string.supervised_choose_image))
            }
            if (!localProfileIcon.isNullOrBlank()) {
                TextButton(onClick = connectionViewModel::clearProfileIcon) {
                    Text(stringResource(R.string.supervised_use_profile_icon))
                }
            }
        }
    }

    if (allowProfileIconChanges && allowBackgroundChanges) HorizontalDivider()

    if (allowBackgroundChanges) {
        Text(stringResource(R.string.supervised_background), style = MaterialTheme.typography.titleSmall)
        Text(
            stringResource(R.string.supervised_background_choices),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = !backgroundEnabled,
                onClick = { connectionViewModel.setBackgroundVisualizationEnabled(false) },
                label = { Text(stringResource(R.string.supervised_off)) },
            )
            FilterChip(
                selected = backgroundEnabled && backgroundAvatar == SphereAvatar.id,
                onClick = { connectionViewModel.setBackgroundAvatar(SphereAvatar.id) },
                label = { Text(stringResource(R.string.supervised_sphere)) },
            )
            availableBackgrounds.forEach { avatar ->
                FilterChip(
                    selected = backgroundEnabled && backgroundAvatar == avatar.id,
                    onClick = { connectionViewModel.setBackgroundAvatar(avatar.id) },
                    label = { Text(avatar.label) },
                )
            }
        }
    }
}
