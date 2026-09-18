package com.hermesandroid.relay.ui.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.hermesandroid.relay.R
import com.hermesandroid.relay.data.EndpointCandidate
import com.hermesandroid.relay.data.displayLabel

/** Translate presentation labels without changing route identity or security keys. */
@Composable
internal fun connectionDisplayLabel(value: String): String =
    connectionDisplayLabel(LocalContext.current, value)

internal fun connectionDisplayLabel(context: Context, value: String): String {
    val resource = when (value) {
        "Dashboard & Gateway", "Chat & Manage" -> R.string.ui_label_dashboard_gateway
        "Direct API", "API fallback" -> R.string.api_fallback_title
        "Relay tools" -> R.string.settings_power_tools
        "Public" -> R.string.cw_role_public
        "Dashboard" -> R.string.cw_dashboard
        "HTTPS Dashboard" -> R.string.ui_label_https_dashboard
        "Hermes Reach · Experimental" -> R.string.hermes_reach_title
        "Plain" -> R.string.cw_plain_label
        "Encrypted" -> R.string.transport_badge_glyph_encrypted
        "Mixed" -> R.string.ui_label_mixed
        "Self-hosted OIDC" -> R.string.ui_label_self_hosted_oidc
        "Password" -> R.string.ui_label_password
        else -> null
    }
    if (resource != null) return context.getString(resource)
    for (suffix in listOf(" (HTTP)", " (HTTPS)")) {
        if (value.endsWith(suffix)) {
            val base = value.removeSuffix(suffix)
            return connectionDisplayLabel(context, base) + suffix
        }
    }
    if (value.startsWith("Custom route (") && value.endsWith(")")) {
        return context.getString(R.string.ui_label_custom_route, value.removePrefix("Custom route (").dropLast(1))
    }
    return value
}

@Composable
internal fun EndpointCandidate.localizedDisplayLabel(): String = when {
    !isBuiltInRole() && !displayName.isNullOrBlank() -> displayName.trim()
    else -> connectionDisplayLabel(displayLabel())
}

private fun EndpointCandidate.isBuiltInRole(): Boolean = role.lowercase() in setOf(
    "lan", "tailscale", "public", "https", "dashboard", "authenticated_dashboard",
    "plugin_proxy", "plugin-proxy", "outbound_broker", "broker", "relay_broker",
)
