package com.hermesandroid.relay.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hermesandroid.relay.R
import com.hermesandroid.relay.ui.icons.RelayIcons

/** Real application destinations; sample tasks from the web study are not app data. */
enum class ConnectDestination(
    @param:StringRes val label: Int,
    val outline: ImageVector,
    val selectedIcon: ImageVector,
) {
    Chat(R.string.screen_chat_label, RelayIcons.Chat, RelayIcons.ChatSelected),
    Manage(R.string.screen_manage_label, RelayIcons.Dashboard, RelayIcons.ManageSelected),
    Connections(R.string.screen_connections_label, RelayIcons.Link, RelayIcons.ConnectionsSelected),
}

@Composable
fun ConnectNavigationBar(
    selected: ConnectDestination,
    onSelect: (ConnectDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier, color = MaterialTheme.colorScheme.surface) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
            Row(
                Modifier.fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                    .selectableGroup()
                    .padding(horizontal = 4.dp),
            ) {
                ConnectDestination.entries.forEach { destination ->
                    val active = selected == destination
                    val color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    Column(
                        modifier = Modifier.weight(1f)
                            .selectable(selected = active, role = Role.Tab, onClick = { if (!active) onSelect(destination) })
                            .heightIn(min = 64.dp)
                            .padding(horizontal = 2.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                    ) {
                        Icon(
                            if (active) destination.selectedIcon else destination.outline,
                            contentDescription = null, tint = color, modifier = Modifier.size(26.dp),
                        )
                        Text(
                            stringResource(destination.label), color = color,
                            style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}
