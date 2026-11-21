package com.nuvio.app.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.features.addons.AddonRepository
import com.nuvio.app.features.cloudstream.CloudStreamRepository
import com.nuvio.app.features.plugins.PluginRepository
import com.nuvio.app.features.search.SearchHistoryRepository
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.compose_settings_page_addons
import nuvio.composeapp.generated.resources.compose_settings_page_plugins
import nuvio.composeapp.generated.resources.extensions_addons_short
import nuvio.composeapp.generated.resources.extensions_catalog_footnote
import nuvio.composeapp.generated.resources.extensions_cloudstream_short
import nuvio.composeapp.generated.resources.extensions_cloudstream_title
import nuvio.composeapp.generated.resources.extensions_plugins_short
import nuvio.composeapp.generated.resources.extensions_recent_searches_short
import nuvio.composeapp.generated.resources.settings_content_discovery_section_sources
import nuvio.composeapp.generated.resources.settings_content_discovery_section_search
import nuvio.composeapp.generated.resources.settings_content_discovery_recent_searches
import org.jetbrains.compose.resources.stringResource

/**
 * Extensions: sources first (CloudStream, then addons, then plugins), search at the bottom.
 * The Home catalog source is no longer set here: it is chosen from the Home screen itself.
 */
internal fun LazyListScope.contentDiscoveryContent(
    isTablet: Boolean,
    showPluginsEntry: Boolean,
    showCloudStreamEntry: Boolean,
    onAddonsClick: () -> Unit,
    onPluginsClick: () -> Unit,
    onCloudStreamClick: () -> Unit,
) {
    item {
        val addons by AddonRepository.uiState.collectAsStateWithLifecycle()
        SettingsSection(
            title = stringResource(Res.string.settings_content_discovery_section_sources),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                if (showCloudStreamEntry) {
                    val cloudStream by CloudStreamRepository.uiState.collectAsStateWithLifecycle()
                    ExtensionSourceRow(
                        icon = Icons.Rounded.CloudQueue,
                        tint = Color(0xFFE0332B),
                        title = stringResource(Res.string.extensions_cloudstream_title),
                        description = stringResource(Res.string.extensions_cloudstream_short),
                        count = cloudStream.plugins.size,
                        isTablet = isTablet,
                        onClick = onCloudStreamClick,
                    )
                    SettingsGroupDivider(isTablet = isTablet)
                }
                ExtensionSourceRow(
                    icon = Icons.Rounded.Extension,
                    tint = Color(0xFF3B82F6),
                    title = stringResource(Res.string.compose_settings_page_addons),
                    description = stringResource(Res.string.extensions_addons_short),
                    count = addons.addons.size,
                    isTablet = isTablet,
                    onClick = onAddonsClick,
                )
                // Plus de liaison avec Jellyfin, Emby ou un autre serveur multimédia.
                if (showPluginsEntry) {
                    val plugins by PluginRepository.uiState.collectAsStateWithLifecycle()
                    SettingsGroupDivider(isTablet = isTablet)
                    ExtensionSourceRow(
                        icon = Icons.Rounded.Bolt,
                        tint = Color(0xFFA855F7),
                        title = stringResource(Res.string.compose_settings_page_plugins),
                        description = stringResource(Res.string.extensions_plugins_short),
                        count = plugins.scrapers.size,
                        isTablet = isTablet,
                        onClick = onPluginsClick,
                    )
                }
            }
            Text(
                text = stringResource(Res.string.extensions_catalog_footnote),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.nuvio.colors.textMuted,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }

    // « Langues des sources »: only CloudStream extensions of these languages are used for Home,
    // search and links (addons declare no language, so they are not filtered).
    if (showCloudStreamEntry) {
        item {
            val cloudStream by CloudStreamRepository.uiState.collectAsStateWithLifecycle()
            val languages = remember(cloudStream.plugins) {
                cloudStream.plugins
                    .filter { it.isInstalled }
                    .mapNotNull { it.metadata.language?.trim()?.lowercase()?.takeIf(String::isNotEmpty) }
                    .distinct()
            }
            CloudStreamExtensionLanguageSettings(isTablet = isTablet, availableLanguages = languages)
        }
    }

    item {
        val recentSearchesEnabled by remember {
            SearchHistoryRepository.ensureLoaded()
            SearchHistoryRepository.enabled
        }.collectAsStateWithLifecycle()

        SettingsSection(
            title = stringResource(Res.string.settings_content_discovery_section_search),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_content_discovery_recent_searches),
                    description = stringResource(Res.string.extensions_recent_searches_short),
                    checked = recentSearchesEnabled,
                    isTablet = isTablet,
                    onCheckedChange = SearchHistoryRepository::setEnabled,
                )
            }
        }
    }
}

/** Row: tinted icon tile on the left, title + one-line description, counter and chevron on the right. */
@Composable
private fun ExtensionSourceRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    description: String,
    count: Int,
    isTablet: Boolean,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = if (isTablet) 20.dp else 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(if (isTablet) 44.dp else 40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tint.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = tokens.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodyLarge,
            color = tokens.colors.textMuted,
        )
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = tokens.colors.textMuted,
        )
    }
}
