package com.jonecx.ibex.ui.live

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jonecx.ibex.R
import com.jonecx.ibex.data.model.VideoFeed
import com.jonecx.ibex.ui.components.IbexTopAppBar
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLiveStreamScreen(
    onNavigateBack: () -> Unit,
    onSave: (VideoFeed) -> Unit,
    modifier: Modifier = Modifier,
    streamToEdit: VideoFeed? = null,
) {
    val isEditMode = streamToEdit != null
    var title by remember { mutableStateOf(streamToEdit?.title ?: "") }
    var url by remember { mutableStateOf(streamToEdit?.url ?: "") }
    var thumbnailUrl by remember { mutableStateOf(streamToEdit?.thumbnailUrl ?: "") }
    var description by remember { mutableStateOf(streamToEdit?.description ?: "") }

    val isValid by remember {
        derivedStateOf { title.isNotBlank() && url.isNotBlank() }
    }

    Scaffold(
        topBar = {
            IbexTopAppBar(
                title = stringResource(if (isEditMode) R.string.live_edit_title else R.string.live_add_title),
                onNavigateBack = onNavigateBack,
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.live_field_title)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text(stringResource(R.string.live_field_url)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = thumbnailUrl,
                onValueChange = { thumbnailUrl = it },
                label = { Text(stringResource(R.string.live_field_thumbnail)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.live_field_description)) },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TextButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.cancel))
                }
                Button(
                    onClick = {
                        onSave(
                            VideoFeed(
                                id = streamToEdit?.id ?: UUID.randomUUID().toString(),
                                title = title.trim(),
                                url = url.trim(),
                                thumbnailUrl = thumbnailUrl.trim(),
                                description = description.trim(),
                            ),
                        )
                    },
                    enabled = isValid,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.live_save))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
