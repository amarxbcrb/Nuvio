package com.nuvio.app.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.features.cloudstream.CloudStreamRepository
import com.nuvio.app.features.plugins.CloudStreamPluginListItem
import com.nuvio.app.features.plugins.CloudStreamHealthButton
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
    CloudStreamSettingsListState.pluginGroups.forEach { group ->
        item(key = "cloudstream-repository:${group.key}", contentType = "cloudstream-repository") {
            CloudStreamRepositoryGroupCard(group)
        }
        if (CloudStreamSettingsListState.isExpanded(group.key)) {
            val lastId = group.plugins.lastOrNull()?.metadata?.id?.value
            items(
                items = group.plugins,
                key = { plugin -> "cloudstream-plugin:${plugin.metadata.id.value}" },
                contentType = { "cloudstream-plugin" },
            ) { plugin ->
                CloudStreamPluginListItem(plugin, isLast = plugin.metadata.id.value == lastId)
            }
        }
    }
    item(key = "cloudstream-health") {
        CloudStreamHealthButton()
    }
}
