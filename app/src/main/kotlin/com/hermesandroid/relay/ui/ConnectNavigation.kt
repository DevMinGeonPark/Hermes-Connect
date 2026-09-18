package com.hermesandroid.relay.ui

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.hermesandroid.relay.ui.components.ConnectDestination

internal fun NavController.openConnectDestination(destination: ConnectDestination) {
    val target = when (destination) {
        ConnectDestination.Chat -> Screen.Chat.route(openAgentSheet = false)
        ConnectDestination.Manage -> Screen.Manage.route
        ConnectDestination.Connections -> Screen.ConnectionsSettings.route
    }
    navigate(target) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        // A saved secondary stack can be indexed by the retained Chat start
        // destination. Restoring it here would reopen Manage instead of Chat.
        restoreState = destination != ConnectDestination.Chat
    }
}
