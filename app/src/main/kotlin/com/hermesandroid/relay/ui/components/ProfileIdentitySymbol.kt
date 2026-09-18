package com.hermesandroid.relay.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hermesandroid.relay.ui.icons.RelayIcons
import com.hermesandroid.relay.ui.theme.accentColor
import com.hermesandroid.relay.ui.theme.readableContentColor
import java.util.Locale

/** Stable presentation only; never changes a server profile or its selection key. */
internal fun profileIdentityColor(name: String?, overrides: Map<String, String> = emptyMap()): Color {
    name?.let { accentColor(overrides[it]) }?.let { return it }
    val key = name.orEmpty().lowercase(Locale.ROOT)
    val colors = listOf(Color(0xFF366B9B), Color(0xFF805C8B), Color(0xFF3C7463))
    return when (key) {
        "", "default", "work", "업무" -> colors[0]
        "personal", "개인" -> colors[1]
        "research", "리서치" -> colors[2]
        else -> colors[key.fold(0u) { hash, c -> hash * 31u + c.code.toUInt() }.rem(3u).toInt()]
    }
}

@Composable
internal fun ProfileIdentitySymbol(
    name: String?,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val symbol = when (name.orEmpty().lowercase(Locale.ROOT)) {
        "work", "업무" -> RelayIcons.Briefcase
        "research", "리서치" -> RelayIcons.Research
        "", "default", "personal", "개인" -> RelayIcons.Personal
        else -> null
    }
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (symbol != null) {
            Icon(symbol, contentDescription = null, tint = readableContentColor(color), modifier = Modifier.size(24.dp))
        } else {
            Text(label.trim().firstOrNull()?.uppercase() ?: "H",
                style = MaterialTheme.typography.titleMedium, color = readableContentColor(color))
        }
    }
}
