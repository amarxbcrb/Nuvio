package com.nuvio.app.features.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.DialogButton
import com.nuvio.app.core.ui.DialogButtonStyle
import com.nuvio.app.core.ui.DialogButtons
import com.nuvio.app.core.ui.DialogSurface
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.features.cloudstream.CloudStreamRepository
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_cancel
import nuvio.composeapp.generated.resources.settings_backup_choose_title
import nuvio.composeapp.generated.resources.settings_backup_export
import nuvio.composeapp.generated.resources.settings_backup_part_accounts
import nuvio.composeapp.generated.resources.settings_backup_part_addons
import nuvio.composeapp.generated.resources.settings_backup_part_appearance
import nuvio.composeapp.generated.resources.settings_backup_part_collections
import nuvio.composeapp.generated.resources.settings_backup_part_extensions
import nuvio.composeapp.generated.resources.settings_backup_part_general
import nuvio.composeapp.generated.resources.settings_backup_part_links
import nuvio.composeapp.generated.resources.settings_backup_part_player
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private val BackupSection.labelRes: StringResource
    get() = when (this) {
        BackupSection.Appearance -> Res.string.settings_backup_part_appearance
        BackupSection.Player -> Res.string.settings_backup_part_player
        BackupSection.Links -> Res.string.settings_backup_part_links
        BackupSection.Accounts -> Res.string.settings_backup_part_accounts
        BackupSection.Addons -> Res.string.settings_backup_part_addons
        BackupSection.Extensions -> Res.string.settings_backup_part_extensions
        BackupSection.Collections -> Res.string.settings_backup_part_collections
        BackupSection.General -> Res.string.settings_backup_part_general
    }

/**
 * Lets the user pick what goes in the backup file: each part of the settings, and each installed
 * CloudStream extension one by one. Everything is ticked at first.
 */
@Composable
internal fun BackupExportChoiceDialog(
    onDismiss: () -> Unit,
    onExport: (sections: Set<BackupSection>, extensionIds: Set<String>) -> Unit,
) {
    val extensions = remember {
        CloudStreamRepository.uiState.value.plugins
            .filter { it.isInstalled }
            .map { it.metadata.id.value to it.metadata.name }
            .sortedBy { it.second.lowercase() }
    }
    var sections by remember { mutableStateOf(BackupSection.entries.toSet()) }
    var extensionIds by remember { mutableStateOf(extensions.map { it.first }.toSet()) }
    var showExtensions by remember { mutableStateOf(false) }

    DialogSurface(
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.settings_backup_choose_title),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            BackupSection.entries.forEach { section ->
                val isExtensions = section == BackupSection.Extensions
                if (isExtensions && extensions.isEmpty()) return@forEach
                val label = stringResource(section.labelRes) +
                    if (isExtensions) "  (${extensionIds.size}/${extensions.size})" else ""
                ChoiceRow(
                    label = label,
                    checked = section in sections,
                    onCheckedChange = { checked -> sections = if (checked) sections + section else sections - section },
                    onLabelClick = if (isExtensions) ({ showExtensions = !showExtensions }) else null,
                )
                if (isExtensions && showExtensions && section in sections) {
                    extensions.forEach { (id, name) ->
                        ChoiceRow(
                            label = name,
                            checked = id in extensionIds,
                            onCheckedChange = { checked -> extensionIds = if (checked) extensionIds + id else extensionIds - id },
                            modifier = Modifier.padding(start = 28.dp),
                        )
                    }
                }
            }
        }
        DialogButtons {
            DialogButton(
                text = stringResource(Res.string.action_cancel),
                onClick = onDismiss,
            )
            DialogButton(
                text = stringResource(Res.string.settings_backup_export),
                enabled = sections.isNotEmpty(),
                onClick = { onExport(sections, extensionIds) },
                style = DialogButtonStyle.Primary,
            )
        }
    }
}

@Composable
private fun ChoiceRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    /** Tapping the name does this instead of ticking (opens the extension list). */
    onLabelClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(
            text = label + if (onLabelClick != null) "  ▾" else "",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.nuvio.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .clickable { onLabelClick?.invoke() ?: onCheckedChange(!checked) }
                .padding(vertical = 10.dp),
        )
    }
}
