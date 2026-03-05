package com.jonecx.ibex.ui.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsStepSliderItem(
    title: String,
    value: Int,
    steps: List<Int>,
    onValueChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val index = steps.indexOf(value).coerceAtLeast(0)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
        )
        Slider(
            value = index.toFloat(),
            onValueChange = { onValueChanged(steps[it.toInt()]) },
            valueRange = 0f..(steps.size - 1).toFloat(),
            steps = steps.size - 2,
            modifier = Modifier.padding(top = 4.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            steps.forEach { step ->
                Text(
                    text = step.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (step == value) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}
