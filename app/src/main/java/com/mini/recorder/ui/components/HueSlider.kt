package com.mini.recorder.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private val Rainbow = listOf(
    Color(0xFFFF0000),
    Color(0xFFFFFF00),
    Color(0xFF00FF00),
    Color(0xFF00FFFF),
    Color(0xFF0000FF),
    Color(0xFFFF00FF),
    Color(0xFFFF0000)
)

/**
 * 彩虹漸層 Hue 滑桿，無圓形滑鈕，以細指示線標示目前色相。
 * [hue] 範圍 0..359。
 */
@Composable
fun HueSlider(
    hue: Int,
    onHueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
    ) {
        val maxWidthPx = constraints.maxWidth
        fun hueFromX(x: Float): Int =
            ((x / maxWidthPx.coerceAtLeast(1)) * 360f).coerceIn(0f, 359f).roundToInt()

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Brush.horizontalGradient(Rainbow))
        )

        val indicatorX = (hue / 360f) * maxWidthPx
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset {
                    val linePx = 3.dp.roundToPx()
                    IntOffset(
                        (indicatorX - linePx / 2f).roundToInt()
                            .coerceIn(0, (maxWidthPx - linePx).coerceAtLeast(0)),
                        0
                    )
                }
                .width(3.dp)
                .height(26.dp)
                .background(MaterialTheme.colorScheme.onSurface)
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset: Offset -> onHueChange(hueFromX(offset.x)) }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, _ ->
                        onHueChange(hueFromX(change.position.x))
                    }
                }
        )
    }
}
