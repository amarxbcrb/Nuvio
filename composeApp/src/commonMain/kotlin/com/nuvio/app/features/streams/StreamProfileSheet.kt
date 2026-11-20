package com.nuvio.app.features.streams

import androidx.compose.runtime.Composable
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.stream_profile_all
import org.jetbrains.compose.resources.stringResource

/** Name shown for [this] profile: the user's name, or "Tout" for the built-in one. */
@Composable
internal fun StreamProfile.displayName(): String =
    if (hasPreferences) name else stringResource(Res.string.stream_profile_all)
