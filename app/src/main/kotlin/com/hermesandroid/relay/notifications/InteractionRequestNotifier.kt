package com.hermesandroid.relay.notifications

import com.hermesandroid.relay.util.localizedString
import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.hermesandroid.relay.MainActivity
import com.hermesandroid.relay.R
import com.hermesandroid.relay.network.upstream.GatewayAsk

/**
 * Action-required notifications for Gateway turns blocked on user input.
 *
 * Notification copy is deliberately generic. Approval commands, clarification
 * questions, secret prompts, environment-variable names, and password requests
 * stay inside the authenticated chat surface and never appear on the lock
 * screen. A stable tag derived from the durable session and request identity
 * makes replay/reconnect delivery replace the existing notification.
 */
object InteractionRequestNotifier {

    private const val TAG = "InteractionNotifier"
    internal const val CHANNEL_ID = "chat_interactions"
    private const val GROUP_KEY = "gateway-interactions"
    internal const val NOTIFICATION_ID = 3823
    internal const val DEFAULT_PROFILE_ROUTE_VALUE = "__server_default__"

    internal fun shouldPost(
        alertsEnabled: Boolean,
        appForeground: Boolean,
        hasPermission: Boolean,
    ): Boolean = alertsEnabled && !appForeground && hasPermission

    internal fun requestKey(sessionId: String, ask: GatewayAsk, profile: String? = null): String {
        val requestIdentity = ask.requestId?.takeIf { it.isNotBlank() } ?: "session"
        return "${profile ?: DEFAULT_PROFILE_ROUTE_VALUE}:$sessionId:${ask.kind.name}:$requestIdentity"
    }

    internal fun notificationTag(
        sessionId: String,
        ask: GatewayAsk,
        profile: String? = null,
    ): String = "gateway-interaction:${requestKey(sessionId, ask, profile)}"

    internal fun chatRoute(sessionId: String, profile: String? = null): String =
        "chat?sessionId=${Uri.encode(sessionId)}&profile=${Uri.encode(profile ?: DEFAULT_PROFILE_ROUTE_VALUE)}"

    internal fun safeTitle(context: Context, ask: GatewayAsk): String = when (ask.kind) {
        GatewayAsk.Kind.APPROVAL -> context.localizedString(R.string.runtime_interaction_approval_title)
        GatewayAsk.Kind.CLARIFY -> context.localizedString(R.string.runtime_interaction_clarify_title)
        GatewayAsk.Kind.SUDO,
        GatewayAsk.Kind.SECRET,
        -> context.localizedString(R.string.runtime_interaction_secret_title)
    }

    internal fun safeBody(context: Context, ask: GatewayAsk): String = when (ask.kind) {
        GatewayAsk.Kind.APPROVAL -> context.localizedString(R.string.runtime_interaction_approval_body)
        GatewayAsk.Kind.CLARIFY -> context.localizedString(R.string.runtime_interaction_clarify_body)
        GatewayAsk.Kind.SUDO,
        GatewayAsk.Kind.SECRET,
        -> context.localizedString(R.string.runtime_interaction_secret_body)
    }

    internal fun safeExpandedBody(
        context: Context,
        sessionId: String,
        ask: GatewayAsk,
        profile: String? = null,
    ): String {
        val action = when (ask.kind) {
            GatewayAsk.Kind.APPROVAL ->
                context.localizedString(R.string.runtime_interaction_approval_detail)
            GatewayAsk.Kind.CLARIFY -> context.localizedString(R.string.runtime_interaction_clarify_detail)
            GatewayAsk.Kind.SUDO -> context.localizedString(R.string.runtime_interaction_sudo_detail)
            GatewayAsk.Kind.SECRET -> context.localizedString(R.string.runtime_interaction_secret_detail)
        }
        val profileLabel = profile?.takeIf { it.isNotBlank() } ?: context.localizedString(R.string.runtime_interaction_server_default)
        val sessionLabel = sessionId.takeLast(12)
        return context.localizedString(R.string.runtime_interaction_detail, action, profileLabel, sessionLabel)
    }

    internal fun actionLabel(context: Context, ask: GatewayAsk): String = when (ask.kind) {
        GatewayAsk.Kind.APPROVAL -> context.localizedString(R.string.runtime_interaction_approval_action)
        GatewayAsk.Kind.CLARIFY -> context.localizedString(R.string.runtime_interaction_clarify_action)
        GatewayAsk.Kind.SUDO,
        GatewayAsk.Kind.SECRET,
        -> context.localizedString(R.string.runtime_interaction_secret_action)
    }

    @SuppressLint("MissingPermission", "NotificationPermission")
    fun notify(
        context: Context,
        sessionId: String,
        ask: GatewayAsk,
        profile: String? = null,
        alertsEnabled: Boolean,
        appForeground: Boolean,
    ): Boolean {
        ensureChannel(context)
        if (!shouldPost(alertsEnabled, appForeground, hasPostNotificationsPermission(context))) {
            return false
        }

        val tag = notificationTag(sessionId, ask, profile)
        val requestKey = requestKey(sessionId, ask, profile)
        val requestCode = requestKey.hashCode()
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            data = Uri.Builder()
                .scheme("hermes-relay")
                .authority("interaction")
                .appendPath(requestKey)
                .build()
            putExtra(MainActivity.EXTRA_NAV_ROUTE, chatRoute(sessionId, profile))
        }
        val tapPending = PendingIntent.getActivity(
            context,
            requestCode,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val title = safeTitle(context, ask)
        val body = safeBody(context, ask)
        val expandedBody = safeExpandedBody(context, sessionId, ask, profile)
        val publicVersion = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.localizedString(R.string.runtime_interaction_public_title))
            .setContentText(context.localizedString(R.string.runtime_interaction_public_body))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedBody))
            .setContentIntent(tapPending)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .setGroup(GROUP_KEY)
            .addAction(0, actionLabel(context, ask), tapPending)
            .build()

        return runCatching {
            NotificationManagerCompat.from(context).notify(tag, NOTIFICATION_ID, notification)
            true
        }.onFailure {
            Log.w(TAG, "notify failed", it)
        }.getOrDefault(false)
    }

    fun cancel(context: Context, sessionId: String, ask: GatewayAsk, profile: String? = null) {
        runCatching {
            NotificationManagerCompat.from(context)
                .cancel(notificationTag(sessionId, ask, profile), NOTIFICATION_ID)
        }.onFailure {
            Log.w(TAG, "cancel failed", it)
        }
    }

    /**
     * Clear only this feature's notifications. Android keeps notifications
     * across process death, so the active-notification scan is also used when
     * MainActivity returns without an in-memory request registry.
     */
    fun cancelAll(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        runCatching {
            manager.activeNotifications
                .filter { it.notification.channelId == CHANNEL_ID }
                .forEach { manager.cancel(it.tag, it.id) }
        }.onFailure {
            Log.w(TAG, "cancelAll failed", it)
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.localizedString(R.string.runtime_interaction_channel),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.localizedString(R.string.runtime_interaction_channel_desc)
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                setShowBadge(true)
            },
        )
    }

    private fun hasPostNotificationsPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }
}
