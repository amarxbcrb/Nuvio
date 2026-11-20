package com.nuvio.app.features.streams

import nuvio.composeapp.generated.resources.links_use_default
import nuvio.composeapp.generated.resources.links_default_is
import nuvio.composeapp.generated.resources.links_for_this_title
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import nuvio.composeapp.generated.resources.links_profiles_title
import nuvio.composeapp.generated.resources.links_my_profiles
import nuvio.composeapp.generated.resources.links_for_all_titles
import nuvio.composeapp.generated.resources.links_profile_settings
import nuvio.composeapp.generated.resources.links_language
import nuvio.composeapp.generated.resources.links_version
import nuvio.composeapp.generated.resources.links_subtitles
import nuvio.composeapp.generated.resources.links_favorite_sources
import nuvio.composeapp.generated.resources.links_search_in
import nuvio.composeapp.generated.resources.links_all_sources
import nuvio.composeapp.generated.resources.links_hide_sources
import nuvio.composeapp.generated.resources.links_new_profile
import nuvio.composeapp.generated.resources.links_no_filter
import nuvio.composeapp.generated.resources.links_edit_profile
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.ui.DialogButton
import com.nuvio.app.core.ui.DialogButtonStyle
import com.nuvio.app.core.ui.DialogButtons
import com.nuvio.app.core.ui.DialogSurface
import com.nuvio.app.core.ui.IosCheckMark
import com.nuvio.app.core.ui.IosGray
import com.nuvio.app.core.ui.IosGroup
import com.nuvio.app.core.ui.IosGroupDivider
import com.nuvio.app.core.ui.IosIconTile
import com.nuvio.app.core.ui.IosOrange
import com.nuvio.app.core.ui.IosRed
import com.nuvio.app.core.ui.IosRow
import com.nuvio.app.core.ui.IosSectionHeader
import com.nuvio.app.core.ui.IosSegment
import com.nuvio.app.core.ui.IosSegmentedControl
import com.nuvio.app.core.ui.IosSwitch
import com.nuvio.app.core.ui.Menu
import com.nuvio.app.core.ui.MenuItem
import com.nuvio.app.core.ui.NuvioInputField
import com.nuvio.app.core.ui.NuvioModalBottomSheet
import com.nuvio.app.core.ui.dismissNuvioBottomSheet
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.nuvioPressScale
import com.nuvio.app.core.ui.nuvioSafeBottomPadding
import com.nuvio.app.features.cloudstream.CloudStreamRepository
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.settings_stream_dub_any
import nuvio.composeapp.generated.resources.settings_stream_dub_only
import nuvio.composeapp.generated.resources.settings_stream_dub_prefer
import nuvio.composeapp.generated.resources.settings_stream_dub_subtitled_first
import nuvio.composeapp.generated.resources.stream_search_all_profile_info
import nuvio.composeapp.generated.resources.stream_search_languages_all
import nuvio.composeapp.generated.resources.stream_search_link_order
import nuvio.composeapp.generated.resources.stream_search_subtitles_player
import nuvio.composeapp.generated.resources.stream_search_title
import nuvio.composeapp.generated.resources.stream_sheet_cancel
import nuvio.composeapp.generated.resources.stream_sheet_close
import nuvio.composeapp.generated.resources.stream_sheet_create
import nuvio.composeapp.generated.resources.stream_sheet_default
import nuvio.composeapp.generated.resources.stream_sheet_default_for_new
import nuvio.composeapp.generated.resources.stream_sheet_delete
import nuvio.composeapp.generated.resources.stream_sheet_delete_confirm
import nuvio.composeapp.generated.resources.stream_sheet_done
import nuvio.composeapp.generated.resources.stream_sheet_extension_languages
import nuvio.composeapp.generated.resources.stream_sheet_favorites_count
import nuvio.composeapp.generated.resources.stream_sheet_favorites_first
import nuvio.composeapp.generated.resources.stream_sheet_favorites_only
import nuvio.composeapp.generated.resources.stream_sheet_hide_others
import nuvio.composeapp.generated.resources.stream_sheet_hide_unknown
import nuvio.composeapp.generated.resources.stream_sheet_include_subtitled
import nuvio.composeapp.generated.resources.stream_sheet_link_languages
import nuvio.composeapp.generated.resources.stream_sheet_new_profile
import nuvio.composeapp.generated.resources.stream_sheet_no_profiles
import nuvio.composeapp.generated.resources.stream_sheet_profile
import nuvio.composeapp.generated.resources.stream_sheet_profile_name
import nuvio.composeapp.generated.resources.stream_sheet_profile_settings
import nuvio.composeapp.generated.resources.stream_sheet_remembered
import nuvio.composeapp.generated.resources.stream_sheet_rename
import nuvio.composeapp.generated.resources.stream_sheet_save
import nuvio.composeapp.generated.resources.stream_sheet_search_in
import nuvio.composeapp.generated.resources.stream_sheet_show_others
import nuvio.composeapp.generated.resources.stream_sheet_single_source
import nuvio.composeapp.generated.resources.stream_sheet_sources
import nuvio.composeapp.generated.resources.stream_sheet_subtitles
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

internal val StreamDubMode.labelRes: StringResource
    get() = when (this) {
        StreamDubMode.Any -> Res.string.settings_stream_dub_any
        StreamDubMode.PreferDubbed -> Res.string.settings_stream_dub_prefer
        StreamDubMode.DubbedOnly -> Res.string.settings_stream_dub_only
        StreamDubMode.SubtitledFirst -> Res.string.settings_stream_dub_subtitled_first
    }

/** What the search of this title depends on, to know whether the links must be searched again. */
private fun searchSignature(titleKey: String?): String = listOf(
    StreamProfileRepository.profileFor(titleKey).id,
    StreamProfileRepository.dubModeFor(StreamProfileRepository.profileFor(titleKey)).wireValue,
    StreamLanguagePreferencesRepository.requestSignature(),
    StreamSearchScopeRepository.favoritesSignature(),
    StreamSearchScopeRepository.scope.value?.id.orEmpty(),
).joinToString("|")

private sealed interface ProfileEdit {
    data object Create : ProfileEdit
    data class Rename(val profile: StreamProfile) : ProfileEdit
}

/** A source of the « Sources » group: its favorite star, its links for this title and its switch. */
private data class SheetSource(val id: String, val name: String, val count: Int, val loading: Boolean)

/** Hint under a profile's name: its link order, and its subtitles when it sets them. */
@Composable
private fun profileHint(profile: StreamProfile, subtitleChoices: Map<String, String>): String {
    if (!profile.hasPreferences) return stringResource(Res.string.links_no_filter)
    val order = stringResource(StreamLanguagePreferencesRepository.preferencesFor(profile).dubMode.labelRes)
    val subtitles = remember(subtitleChoices, profile) { StreamProfileRepository.subtitleLanguageFor(profile) }
    return if (subtitles != null) "$order · ST ${subtitles.uppercase()}" else order
}

/**
 * « Profils »: the user's profiles, chosen once for every title, the settings of the chosen one
 * (language, version, subtitles) and its favorite sources. Changes apply at once; [onDismiss] says
 * whether the links must be searched again.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun StreamSearchSheet(
    sourceOptions: List<StreamSourceOption>,
    groups: List<AddonStreamGroup>,
    @Suppress("UNUSED_PARAMETER") searchOptions: List<StreamSourceOption>,
    @Suppress("UNUSED_PARAMETER") appliedScopeId: String?,
    onDismiss: (changed: Boolean) -> Unit,
) {
    val titleKey = StreamProfileRepository.currentTitleKey
    remember(titleKey) { StreamProfileRepository.activateForTitle(titleKey) }
    val initialSignature = remember { searchSignature(titleKey) }
    val profiles by StreamProfileRepository.profiles.collectAsStateWithLifecycle()
    val profile by StreamProfileRepository.active.collectAsStateWithLifecycle()
    val preferences by StreamLanguagePreferencesRepository.preferences.collectAsStateWithLifecycle()
    val subtitleChoices by StreamProfileRepository.subtitleChoices.collectAsStateWithLifecycle()
    val pinnedSourceIds by rememberPinnedStreamSourceIds()
    val favoritesOnly by StreamSearchScopeRepository.favoritesOnly.collectAsStateWithLifecycle()
    var edit by remember { mutableStateOf<ProfileEdit?>(null) }
    var deleting by remember { mutableStateOf<StreamProfile?>(null) }
    var showAllSources by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val finish: () -> Unit = {
        if (StreamSearchScopeRepository.favoritesOnly.value && PinnedStreamSourcesRepository.pinnedSourceIds.value.isEmpty()) {
            StreamSearchScopeRepository.setFavoritesOnly(false)
        }
        onDismiss(searchSignature(titleKey) != initialSignature)
    }
    val close: () -> Unit = {
        coroutineScope.launch { dismissNuvioBottomSheet(sheetState = sheetState, onDismiss = finish) }
    }

    NuvioModalBottomSheet(onDismissRequest = close, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 700.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = nuvioSafeBottomPadding(16.dp)),
        ) {
            SheetHeader(title = stringResource(Res.string.links_profiles_title), onClose = close, onDone = close)

            val defaultProfile by StreamProfileRepository.defaultProfile.collectAsStateWithLifecycle()
            val titleProfiles by StreamProfileRepository.titleProfiles.collectAsStateWithLifecycle()
            val ownProfile = remember(titleProfiles, profiles, titleKey) { StreamProfileRepository.hasOwnProfile(titleKey) }
            // Choosing here only changes this title; the default for every title is set in Settings.
            IosSectionHeader(
                title = stringResource(Res.string.links_my_profiles),
                trailing = stringResource(Res.string.links_for_this_title),
            )
            // Two tiles a row: the user's profiles, « Tout », then « + Nouveau profil ».
            val tiles: List<StreamProfile?> = profiles + StreamProfile.ALL + null
            tiles.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    row.forEach { tile ->
                        if (tile == null) {
                            NewProfileTile(onClick = { edit = ProfileEdit.Create }, modifier = Modifier.weight(1f).fillMaxHeight())
                        } else {
                            ProfileTile(
                                name = tile.displayName(),
                                hint = profileHint(tile, subtitleChoices),
                                selected = tile == profile,
                                onClick = { StreamProfileRepository.select(tile) },
                                onLongClick = if (tile.hasPreferences) {
                                    { edit = ProfileEdit.Rename(tile) }
                                } else {
                                    null
                                },
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            if (ownProfile && titleKey != null) {
                IosGroup {
                    IosRow(
                        title = stringResource(Res.string.links_use_default, defaultProfile.displayName()),
                        titleColor = MaterialTheme.nuvio.colors.accent,
                        onClick = { StreamProfileRepository.clearForTitle(titleKey) },
                    )
                }
            } else {
                SheetNote(stringResource(Res.string.links_default_is, defaultProfile.displayName()))
            }

            if (!profile.hasPreferences) {
                SheetNote(stringResource(Res.string.stream_search_all_profile_info))
            } else {
                IosSectionHeader(title = stringResource(Res.string.links_profile_settings, profile.name))
                IosGroup {
                    val allLanguages = stringResource(Res.string.stream_search_languages_all)
                    MenuValueRow(
                        title = stringResource(Res.string.links_language),
                        value = if (preferences.linkLanguages.isEmpty()) {
                            allLanguages
                        } else {
                            preferences.linkLanguages.sorted().map { localizedLanguageName(it) }.joinToString(", ")
                        },
                        options = listOf<Pair<String?, String>>(null to allLanguages) +
                            StreamLanguageChoices.map { (code, _) -> code to localizedLanguageName(code) },
                        isSelected = { code -> if (code == null) preferences.linkLanguages.isEmpty() else code in preferences.linkLanguages },
                        multiSelect = true,
                        onSelect = { code ->
                            if (code == null) {
                                StreamLanguagePreferencesRepository.update { it.copy(linkLanguages = emptySet()) }
                            } else {
                                StreamLanguagePreferencesRepository.toggleLinkLanguage(code)
                            }
                        },
                    )
                    IosGroupDivider()
                    MenuValueRow(
                        title = stringResource(Res.string.links_version),
                        value = stringResource(preferences.dubMode.labelRes),
                        options = StreamDubMode.entries.map { mode -> mode to stringResource(mode.labelRes) },
                        isSelected = { it == preferences.dubMode },
                        onSelect = { mode -> StreamLanguagePreferencesRepository.update { it.copy(dubMode = mode) } },
                    )
                    IosGroupDivider()
                    val subtitleLanguage = remember(subtitleChoices, profile) { StreamProfileRepository.subtitleLanguageFor(profile) }
                    val playerSubtitles = stringResource(Res.string.stream_search_subtitles_player)
                    MenuValueRow(
                        title = stringResource(Res.string.links_subtitles),
                        value = subtitleLanguage?.let { localizedLanguageName(it) } ?: playerSubtitles,
                        options = listOf<Pair<String?, String>>(null to playerSubtitles) +
                            StreamLanguageChoices.map { (code, _) -> code to localizedLanguageName(code) },
                        isSelected = { it == subtitleLanguage },
                        onSelect = { code -> StreamProfileRepository.setSubtitleLanguage(profile, code) },
                    )
                }

                // Favorite sources of the profile; « Toutes les sources » lists every one to star it.
                val sources = remember(sourceOptions, groups) {
                    sourceOptions
                        .filterNot { it.id.startsWith("debrid:") }
                        .map { option ->
                            val group = groups.firstOrNull { it.addonId == option.id }
                            SheetSource(option.id, option.name, group?.streams?.size ?: 0, group?.isLoading == true)
                        }
                        .sortedByDescending { it.count }
                }
                fun isFavorite(id: String) = pinnedSourceIds.any { it == id || it.startsWith("$id|") }
                val favorites = sources.filter { isFavorite(it.id) }
                IosSectionHeader(
                    title = stringResource(Res.string.links_favorite_sources),
                    trailing = favorites.size.takeIf { it > 0 }?.toString(),
                )
                IosGroup {
                    val favoritesFirst = stringResource(Res.string.stream_sheet_favorites_first)
                    val favoritesOnlyLabel = stringResource(Res.string.stream_sheet_favorites_only)
                    MenuValueRow(
                        title = stringResource(Res.string.links_search_in),
                        value = if (favoritesOnly) favoritesOnlyLabel else favoritesFirst,
                        options = listOf(false to favoritesFirst, true to favoritesOnlyLabel),
                        isSelected = { it == favoritesOnly },
                        onSelect = { only -> StreamSearchScopeRepository.setFavoritesOnly(only) },
                    )
                    val listed = if (showAllSources) sources else favorites
                    listed.forEach { source ->
                        IosGroupDivider()
                        FavoriteSourceRow(source = source, favorite = isFavorite(source.id))
                    }
                    if (sources.size > favorites.size || showAllSources) {
                        IosGroupDivider()
                        IosRow(
                            title = if (showAllSources) {
                                stringResource(Res.string.links_hide_sources)
                            } else {
                                stringResource(Res.string.links_all_sources, sources.size)
                            },
                            titleColor = MaterialTheme.nuvio.colors.accent,
                            onClick = { showAllSources = !showAllSources },
                            trailing = { Chevron() },
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    when (val current = edit) {
        null -> Unit
        ProfileEdit.Create -> ProfileNameDialog(
            title = stringResource(Res.string.links_new_profile),
            initialName = "",
            confirmLabel = stringResource(Res.string.stream_sheet_create),
            onDismiss = { edit = null },
            onConfirm = { name ->
                edit = null
                StreamProfileRepository.createProfile(name)?.let(StreamProfileRepository::select)
            },
        )
        is ProfileEdit.Rename -> ProfileNameDialog(
            title = stringResource(Res.string.links_edit_profile, current.profile.name),
            initialName = current.profile.name,
            confirmLabel = stringResource(Res.string.stream_sheet_save),
            onDismiss = { edit = null },
            onConfirm = { name ->
                edit = null
                StreamProfileRepository.renameProfile(current.profile, name)
            },
            onDelete = {
                edit = null
                deleting = current.profile
            },
        )
    }
    deleting?.let { target ->
        DialogSurface(
            onDismissRequest = { deleting = null },
            title = stringResource(Res.string.stream_sheet_delete),
            message = stringResource(Res.string.stream_sheet_delete_confirm, target.name),
        ) {
            DialogButtons {
                DialogButton(text = stringResource(Res.string.stream_sheet_cancel), onClick = { deleting = null })
                DialogButton(
                    text = stringResource(Res.string.stream_sheet_delete),
                    style = DialogButtonStyle.Destructive,
                    onClick = {
                        deleting = null
                        StreamProfileRepository.deleteProfile(target)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProfileTile(
    name: String,
    hint: String,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.nuvio.colors.accent
    val shape = RoundedCornerShape(20.dp)
    val fill by animateColorAsState(if (selected) accent.copy(alpha = 0.30f) else Color.White.copy(alpha = 0.07f), tween(220), label = "profile_tile")
    Column(
        modifier = modifier
            .nuvioPressScale(pressedScale = 0.96f)
            .clip(shape)
            .background(fill)
            .border(1.dp, if (selected) accent.copy(alpha = 0.8f) else Color.Transparent, shape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            hint,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.55f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun NewProfileTile(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = modifier
            .nuvioPressScale(pressedScale = 0.96f)
            .clip(shape)
            .border(1.dp, Color.White.copy(alpha = 0.18f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(22.dp))
        Text(stringResource(Res.string.links_new_profile), style = MaterialTheme.typography.titleSmall, color = Color.White.copy(alpha = 0.8f), maxLines = 2)
    }
}

/** ★ name  count: the star makes the source a favorite of the profile. */
@Composable
private fun FavoriteSourceRow(source: SheetSource, favorite: Boolean) {
    IosRow(
        title = source.name,
        onClick = { PinnedStreamSourcesRepository.setGroupFavorite(source.id, !favorite) },
        leading = {
            Icon(
                imageVector = if (favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = null,
                tint = if (favorite) Color(0xFFFF4D5E) else MaterialTheme.nuvio.colors.textMuted,
                modifier = Modifier.size(24.dp),
            )
        },
        trailing = {
            if (source.count > 0) {
                Text(source.count.toString(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.nuvio.colors.textMuted)
            }
        },
    )
}

/** Close (✕), centred title, done (✓ in the accent colour). */
@Composable
private fun SheetHeader(title: String, onClose: () -> Unit, onDone: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundIconButton(
            icon = Icons.Rounded.Close,
            label = stringResource(Res.string.stream_sheet_close),
            background = Color.White.copy(alpha = 0.10f),
            tint = MaterialTheme.nuvio.colors.textPrimary,
            onClick = onClose,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.nuvio.colors.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
        )
        RoundIconButton(
            icon = Icons.Rounded.Check,
            label = stringResource(Res.string.stream_sheet_done),
            background = MaterialTheme.nuvio.colors.accent,
            tint = MaterialTheme.nuvio.colors.onAccent,
            onClick = onDone,
        )
    }
}

@Composable
private fun RoundIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    background: Color,
    tint: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .nuvioPressScale(pressedScale = 0.9f)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun SheetNote(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.nuvio.colors.textMuted,
        modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
    )
}

@Composable
private fun Chevron() {
    Icon(
        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.nuvio.colors.textMuted,
        modifier = Modifier.size(22.dp),
    )
}

/**
 * « Ordre des liens ..... Doublé d'abord ⌃⌄ »: the value opens an iOS-like menu. With
 * [multiSelect] the menu stays open and each choice is ticked or unticked.
 */
@Composable
private fun <T> MenuValueRow(
    title: String,
    value: String,
    options: List<Pair<T, String>>,
    isSelected: (T) -> Boolean,
    onSelect: (T) -> Unit,
    multiSelect: Boolean = false,
) {
    var expanded by remember { mutableStateOf(false) }
    IosRow(
        title = title,
        onClick = { expanded = true },
        trailing = {
            Box {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.nuvio.colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 170.dp),
                    )
                    Icon(
                        imageVector = Icons.Rounded.UnfoldMore,
                        contentDescription = null,
                        tint = MaterialTheme.nuvio.colors.textMuted,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Menu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    options.forEach { (option, label) ->
                        MenuItem(
                            text = label,
                            selected = isSelected(option),
                            onClick = {
                                onSelect(option)
                                if (!multiSelect) expanded = false
                            },
                        )
                    }
                }
            }
        },
    )
}

/** "Toutes", or the language names ("Français, Arabe"). */
private fun Set<String>.languageSummary(all: String): String =
    if (isEmpty()) all else sorted().joinToString(", ") { streamLanguageDisplayName(it) }

@Composable
private fun ProfileNameDialog(
    title: String,
    initialName: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var name by remember { mutableStateOf(initialName) }
    DialogSurface(onDismissRequest = onDismiss, title = title) {
        NuvioInputField(
            value = name,
            onValueChange = { name = it.take(24) },
            placeholder = stringResource(Res.string.stream_sheet_profile_name),
        )
        Spacer(Modifier.height(12.dp))
        DialogButtons {
            if (onDelete != null) {
                DialogButton(
                    text = stringResource(Res.string.stream_sheet_delete),
                    style = DialogButtonStyle.Destructive,
                    onClick = onDelete,
                )
            }
            DialogButton(text = stringResource(Res.string.stream_sheet_cancel), onClick = onDismiss)
            DialogButton(
                text = confirmLabel,
                style = DialogButtonStyle.Primary,
                enabled = name.isNotBlank(),
                onClick = { onConfirm(name) },
            )
        }
    }
}
