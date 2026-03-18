package com.jonecx.ibex.ui.explorer.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jonecx.ibex.R
import com.jonecx.ibex.data.model.SortDirection
import com.jonecx.ibex.data.model.SortField
import com.jonecx.ibex.data.model.SortOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortBottomSheet(
    currentSortOption: SortOption,
    onSortOptionSelected: (SortOption) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.sort_by),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(12.dp))

            DirectionChips(
                selected = currentSortOption.direction,
                onDirectionSelected = { direction ->
                    onSortOptionSelected(currentSortOption.copy(direction = direction))
                },
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()

            SortField.entries.forEach { field ->
                SortFieldRow(
                    label = sortFieldLabel(field),
                    isSelected = currentSortOption.field == field,
                    onClick = {
                        onSortOptionSelected(currentSortOption.copy(field = field))
                    },
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DirectionChips(
    selected: SortDirection,
    onDirectionSelected: (SortDirection) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SortDirection.entries.forEach { direction ->
            FilterChip(
                selected = selected == direction,
                onClick = { onDirectionSelected(direction) },
                label = {
                    Text(text = sortDirectionLabel(direction))
                },
            )
        }
    }
}

@Composable
private fun SortFieldRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = isSelected, onClick = onClick)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun sortFieldLabel(field: SortField): String = when (field) {
    SortField.NAME -> stringResource(R.string.sort_name)
    SortField.SIZE -> stringResource(R.string.sort_size)
    SortField.DATE_MODIFIED -> stringResource(R.string.sort_date_modified)
    SortField.DATE_CREATED -> stringResource(R.string.sort_date_created)
}

@Composable
private fun sortDirectionLabel(direction: SortDirection): String = when (direction) {
    SortDirection.ASCENDING -> "▲ ${stringResource(R.string.sort_ascending)}"
    SortDirection.DESCENDING -> "▼ ${stringResource(R.string.sort_descending)}"
}
