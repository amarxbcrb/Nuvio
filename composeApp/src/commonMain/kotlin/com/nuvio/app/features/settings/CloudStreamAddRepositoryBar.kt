package com.nuvio.app.features.settings

import androidx.compose.foundation.background
import nuvio.composeapp.generated.resources.cloudstream_add_repository_paste
import nuvio.composeapp.generated.resources.cloudstream_add_repository_title
import com.nuvio.app.core.ui.nuvioGlassPanel
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Add
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
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

/**
 * « AJOUTER UN DÉPÔT » panel: link field with a « Coller » button inside, and a round « + ».
 * Solid panel (opaque card, or liquid glass when that style is on and a backdrop exists), so it
 * stays readable over the list it is pinned above.
 */
@Composable
fun CloudStreamAddRepositoryBar(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
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
    val hairline = Color.White.copy(alpha = 0.08f)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .nuvioGlassPanel(RectangleShape, tintAlpha = 0.70f)
            .drawBehind { drawRect(hairline, size = Size(size.width, 1.dp.toPx())) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = stringResource(Res.string.cloudstream_add_repository_title),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            color = tokens.colors.textMuted,
        )
        Spacer(Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val pill = RoundedCornerShape(50)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp)
                    .clip(pill)
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), pill)
                    .padding(start = 18.dp, end = 6.dp),
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
                Spacer(Modifier.width(8.dp))
                Row(
                    modifier = Modifier
                        .height(48.dp)
                        .clip(pill)
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable {
                            clipboard.getText()?.text?.trim()?.takeIf { it.isNotEmpty() }?.let {
                                url = it
                                message = null
                            }
                        }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.ContentPaste, contentDescription = null, tint = tokens.colors.textPrimary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(Res.string.cloudstream_add_repository_paste),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = tokens.colors.textPrimary,
                    )
                }
            }
            val enabled = url.isNotBlank() && !busy
            val addLabel = stringResource(Res.string.cloudstream_add_repository_action)
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .shadow(if (enabled) 14.dp else 0.dp, CircleShape, ambientColor = tokens.colors.accent, spotColor = tokens.colors.accent)
                    .clip(CircleShape)
                    .background(tokens.colors.accent.copy(alpha = if (enabled) 1f else 0.45f))
                    .clickable(enabled = enabled) { submit() }
                    .semantics { contentDescription = addLabel },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
            }
        }
        message?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = tokens.colors.textMuted,
            )
        }
    }
}
