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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.withFrameMillis
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.NuvioModalBottomSheet
import com.nuvio.app.core.ui.accentBrush
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.themePalette
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_cancel
import nuvio.composeapp.generated.resources.settings_backup_progress_decimal
import nuvio.composeapp.generated.resources.settings_backup_progress_elapsed
import nuvio.composeapp.generated.resources.settings_backup_progress_eta
import nuvio.composeapp.generated.resources.settings_backup_progress_eta_minutes
import nuvio.composeapp.generated.resources.settings_backup_progress_exporting
import nuvio.composeapp.generated.resources.settings_backup_progress_importing
import nuvio.composeapp.generated.resources.settings_backup_progress_items
import nuvio.composeapp.generated.resources.settings_backup_progress_size
import nuvio.composeapp.generated.resources.settings_backup_progress_size_kb
import nuvio.composeapp.generated.resources.settings_backup_progress_size_mb
import nuvio.composeapp.generated.resources.settings_backup_progress_stage
import org.jetbrains.compose.resources.stringResource

/**
 * Progress of a backup export or import: part being processed, bar, percentage, remaining time
 * (smoothed, hidden for the first 1.5 s), items, size, elapsed time and a cancel button.
 * Swiping it away is disabled: the only way out is « Annuler » or the end of the operation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BackupProgressSheet(
    state: BackupProgressState,
    onCancel: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden },
    )
    val tokens = MaterialTheme.nuvio
    val decimal = stringResource(Res.string.settings_backup_progress_decimal)
    // The bar never stands still: it catches up quickly with the real progress, then glides
    // slowly toward the end of the part in progress (without reaching it) until the next update.
    val latest by rememberUpdatedState(state)
    var shown by remember { mutableFloatStateOf(state.fraction) }
    LaunchedEffect(Unit) {
        var last = withFrameMillis { it }
        while (true) {
            val now = withFrameMillis { it }
            val dt = ((now - last) / 1000f).coerceIn(0f, 0.1f)
            last = now
            val target = latest.fraction
            val ceiling = maxOf(target, latest.nextFraction * 0.97f)
            shown = when {
                shown < target -> (shown + ((target - shown) * 8f + 0.02f) * dt).coerceAtMost(target)
                shown < ceiling -> shown + (ceiling - shown) * 0.35f * dt
                else -> shown
            }
        }
    }
    NuvioModalBottomSheet(onDismissRequest = {}, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = stringResource(
                    if (state.operation == BackupOperation.Export) Res.string.settings_backup_progress_exporting
                    else Res.string.settings_backup_progress_importing,
                ),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = tokens.colors.textPrimary,
            )
            state.stage?.let { stage ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        Res.string.settings_backup_progress_stage,
                        stringResource(stage.labelRes),
                        state.stageIndex,
                        state.stageCount,
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = tokens.colors.textMuted,
                )
            }
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.10f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(shown)
                        .height(10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.themePalette.accentBrush()),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${(shown * 100).toInt()} %",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = tokens.colors.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                state.etaMs?.let { eta ->
                    val seconds = ((eta + 999) / 1000).coerceAtLeast(1)
                    Text(
                        text = if (seconds < 60) {
                            stringResource(Res.string.settings_backup_progress_eta, seconds.toInt())
                        } else {
                            stringResource(Res.string.settings_backup_progress_eta_minutes, ((seconds + 59) / 60).toInt())
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = tokens.colors.textMuted,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.05f)),
            ) {
                ProgressInfoRow(
                    label = stringResource(Res.string.settings_backup_progress_items),
                    value = "${groupDigits(state.doneItems)} / ${groupDigits(state.totalItems)}",
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = tokens.colors.borderSubtle)
                ProgressInfoRow(
                    label = stringResource(Res.string.settings_backup_progress_size),
                    value = if (state.bytes < 1_000_000L) {
                        stringResource(Res.string.settings_backup_progress_size_kb, oneDecimal(state.bytes / 1_000.0, decimal))
                    } else {
                        stringResource(Res.string.settings_backup_progress_size_mb, oneDecimal(state.bytes / 1_000_000.0, decimal))
                    },
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = tokens.colors.borderSubtle)
                ProgressInfoRow(
                    label = stringResource(Res.string.settings_backup_progress_elapsed),
                    value = clock(state.elapsedMs),
                )
            }
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.10f))
                    .clickable(onClick = onCancel),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.action_cancel),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = tokens.colors.textPrimary,
                )
            }
        }
    }
}

@Composable
private fun ProgressInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.nuvio.colors.textMuted)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.nuvio.colors.textPrimary)
    }
}

/** 1284 -> "1 284" (narrow no-break space). */
internal fun groupDigits(value: Int): String =
    value.toString().reversed().chunked(3).joinToString(" ").reversed()

private fun oneDecimal(value: Double, separator: String): String {
    val tenths = kotlin.math.round(value * 10).toLong()
    return "${tenths / 10}$separator${tenths % 10}"
}

private fun clock(ms: Long): String {
    val total = ms / 1000
    val minutes = total / 60
    val seconds = total % 60
    return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
}
