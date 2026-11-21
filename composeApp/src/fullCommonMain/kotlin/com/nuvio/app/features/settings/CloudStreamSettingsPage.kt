package com.nuvio.app.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.runtime.key
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.expandVertically
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.features.cloudstream.CloudStreamRepository
import com.nuvio.app.features.plugins.CloudStreamPluginListItem
import androidx.compose.foundation.layout.padding
import com.nuvio.app.features.plugins.CloudStreamRepositoryGroupCard
import com.nuvio.app.features.plugins.CloudStreamSettingsListState
import com.nuvio.app.features.plugins.CloudStreamSettingsSection

internal actual fun LazyListScope.cloudStreamSettingsContent() {
    // Counts, search + filters, then « DÉPÔTS ».
    item {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CloudStreamSettingsSection()
        }
    }
    // Each repository is a card; opened, its extensions follow as rows of the same card, each its
    // own lazy item so only the rows on screen are composed.
    CloudStreamSettingsListState.ensureCollecting()
    // All repositories in one item, so they form a single card (the list puts a gap between its
    // items). An opened repository shows its extensions with an expand animation.
    item(key = "cloudstream-repositories", contentType = "cloudstream-repositories") {
        val groups = CloudStreamSettingsListState.pluginGroups
        Column(modifier = Modifier.fillMaxWidth()) {
            groups.forEachIndexed { index, group ->
                val lastGroup = index == groups.lastIndex
                val expanded = CloudStreamSettingsListState.isExpanded(group.key)
                CloudStreamRepositoryGroupCard(group, isFirst = index == 0, isLast = lastGroup)
                AnimatedVisibility(
                    visible = expanded && group.plugins.isNotEmpty(),
                    enter = expandVertically(tween(280)) + fadeIn(tween(220)),
                    exit = shrinkVertically(tween(240)) + fadeOut(tween(160)),
                ) {
                    Column {
                        val lastId = group.plugins.lastOrNull()?.metadata?.id?.value
                        group.plugins.forEach { plugin ->
                            key(plugin.metadata.id.value) {
                                // Rounded bottom only at the very end of the card.
                                CloudStreamPluginListItem(plugin, isLast = lastGroup && plugin.metadata.id.value == lastId)
                            }
                        }
                    }
                }
            }
        }
    }
}
