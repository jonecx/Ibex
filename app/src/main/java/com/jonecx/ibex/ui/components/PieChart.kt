package com.jonecx.ibex.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class PieChartSegment(
    val label: String,
    val value: Float,
    val color: Color,
)

@Composable
fun PieChart(
    segments: List<PieChartSegment>,
    modifier: Modifier = Modifier,
    chartSize: Dp = 200.dp,
    strokeWidth: Dp = 32.dp,
    animationDurationMs: Int = 800,
) {
    val total = segments.sumOf { it.value.toDouble() }.toFloat()
    if (total <= 0f) return

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(segments) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(durationMillis = animationDurationMs))
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(chartSize),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(chartSize)) {
                val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Butt)
                val inset = strokeWidth.toPx() / 2
                var startAngle = -90f

                segments.forEach { segment ->
                    val sweep = (segment.value / total) * 360f * animationProgress.value
                    drawArc(
                        color = segment.color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = stroke,
                        topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                        size = androidx.compose.ui.geometry.Size(
                            size.width - strokeWidth.toPx(),
                            size.height - strokeWidth.toPx(),
                        ),
                    )
                    startAngle += sweep
                }
            }
        }

        Spacer(modifier = Modifier.size(24.dp))

        PieChartLegend(segments = segments, total = total)
    }
}

@Composable
fun PieChartLegend(
    segments: List<PieChartSegment>,
    total: Float,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        segments.forEach { segment ->
            val percentage = if (total > 0f) (segment.value / total * 100f) else 0f
            PieChartLegendItem(
                color = segment.color,
                label = segment.label,
                percentage = percentage,
            )
        }
    }
}

@Composable
private fun PieChartLegendItem(
    color: Color,
    label: String,
    percentage: Float,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(12.dp),
            shape = CircleShape,
            color = color,
            content = {},
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "%.1f%%".format(percentage),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
