package com.nuvio.app.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.nuvioGlassControl
import com.nuvio.app.features.cloudstream.AddCloudStreamRepositoryResult
import com.nuvio.app.features.cloudstream.CloudStreamRepository
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.cloudstream_add_repository_action
import nuvio.composeapp.generated.resources.cloudstream_add_repository_added
import nuvio.composeapp.generated.resources.cloudstream_add_repository_placeholder
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/**
 * True when the screen shows [CloudStreamAddRepositoryBar] fixed at the bottom; the page content
 * then leaves the add field out of its list.
 */
val LocalCloudStreamAddBarHosted = compositionLocalOf { false }

/** « Coller le lien d'un dépôt » field and « Ajouter » button. */
@Composable
fun CloudStreamAddRepositoryBar(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val tokens = MaterialTheme.nuvio
    fun submit() {
        val requested = url.trim()
        if (requested.isEmpty() || busy) return
        busy = true
        message = null
        scope.launch {
            message = when (val result = CloudStreamRepository.addRepository(requested)) {
                is AddCloudStreamRepositoryResult.Success -> {
                    url = ""
                    getString(Res.string.cloudstream_add_repository_added, result.repository.name)
                }
                is AddCloudStreamRepositoryResult.Error -> result.message
            }
            busy = false
        }
    }
    Column(modifier = modifier.fillMaxWidth()) {
        message?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = tokens.colors.textMuted,
                modifier = Modifier.padding(start = 8.dp, bottom = 6.dp),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val pill = RoundedCornerShape(50)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .nuvioGlassControl(pill)
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Link, contentDescription = null, tint = tokens.colors.textMuted, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (url.isEmpty()) {
                        Text(
                            text = stringResource(Res.string.cloudstream_add_repository_placeholder),
                            style = MaterialTheme.typography.bodyLarge,
                            color = tokens.colors.textMuted,
                            maxLines = 1,
                        )
                    }
                    BasicTextField(
                        value = url,
                        onValueChange = { url = it; message = null },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = tokens.colors.textPrimary),
                        cursorBrush = SolidColor(tokens.colors.accent),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            val enabled = url.isNotBlank() && !busy
            Box(
                modifier = Modifier
                    .height(56.dp)
                    .clip(pill)
                    .background(tokens.colors.accent.copy(alpha = if (enabled) 1f else 0.45f))
                    .clickable(enabled = enabled) { submit() }
                    .padding(horizontal = 26.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.cloudstream_add_repository_action),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.ui.graphics.Color.White,
                )
            }
        }
    }
}
