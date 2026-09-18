package com.hermesandroid.relay.viewmodel

import android.content.Context
import com.hermesandroid.relay.R
import com.hermesandroid.relay.util.localizedString
import com.hermesandroid.relay.data.SupervisedModePolicy

/**
 * Fail-closed dispatch policy for Android Supervised Mode.
 *
 * This intentionally runs before demo handling, route selection, slash.exec,
 * command.dispatch, steering, and queueing. Kotlin's default trim recognizes
 * Unicode whitespace, preventing an indented slash command from bypassing the
 * client restriction.
 */
internal fun supervisedMessageBlockReason(
    policy: SupervisedModePolicy,
    text: String,
    context: Context? = null,
): String? {
    if (!policy.enabled) return null
    if (!policy.isConfigured) {
        return context?.localizedString(R.string.runtime_supervised_profile_required)
            ?: "Supervised mode is unavailable until the parent selects a profile."
    }
    if (text.trimStart().startsWith('/')) {
        return context?.localizedString(R.string.runtime_supervised_slash_blocked)
            ?: "Slash commands are unavailable in supervised mode."
    }
    return null
}
