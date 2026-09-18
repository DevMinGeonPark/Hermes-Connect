package com.hermesandroid.relay.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.hermesandroid.relay.ui.theme.LocalBrand
import com.hermesandroid.relay.viewmodel.VoiceState
import kotlin.math.cos
import kotlin.math.sin

/** Decorative view of live voice state and amplitude; it never starts audio or a timer. */
@Composable
fun JavisVoiceSignal(state: VoiceState, amplitude: Float, modifier: Modifier = Modifier, outputAudioActive: Boolean = false) {
    val palette = LocalBrand.current
    val accent = if (state == VoiceState.Error) palette.danger else palette.cyan
    val level = amplitude.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f
    val active = state == VoiceState.Listening || (state == VoiceState.Speaking && outputAudioActive)
    Canvas(modifier.clearAndSetSemantics { }) {
        val radius = size.minDimension * 0.39f
        val hairline = (radius * 0.005f).coerceAtLeast(1f)
        drawCircle(accent.copy(alpha = 0.045f), radius * 0.84f)
        listOf(1f, 0.91f, 0.73f).forEach { scale ->
            drawCircle(accent.copy(alpha = 0.30f), radius * scale, style = Stroke(hairline))
        }
        repeat(36) { index ->
            val angle = index * Math.PI * 2 / 36
            val start = Offset(center.x + cos(angle).toFloat() * radius * 1.06f,
                center.y + sin(angle).toFloat() * radius * 1.06f)
            val end = Offset(center.x + cos(angle).toFloat() * radius * 1.09f,
                center.y + sin(angle).toFloat() * radius * 1.09f)
            drawLine(accent.copy(alpha = 0.25f), start, end, hairline)
        }
        val heights = listOf(0.10f, 0.22f, 0.34f, 0.48f, 0.31f, 0.18f, 0.08f)
        heights.forEachIndexed { index, height ->
            val x = center.x + (index - 3) * radius * 0.095f
            val half = radius * height * if (active) (0.35f + level * 0.65f) else 0.35f
            drawLine(accent, Offset(x, center.y - half), Offset(x, center.y + half),
                strokeWidth = radius * 0.025f, cap = StrokeCap.Round)
        }
    }
}
