package com.nuvio.app.features.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlin.time.TimeSource

enum class BackupOperation { Export, Import }

data class BackupProgressState(
    val operation: BackupOperation,
    val stage: BackupSection?,
    /** 1-based position of [stage] among the parts actually present. */
    val stageIndex: Int,
    val stageCount: Int,
    val doneItems: Int,
    val totalItems: Int,
    val bytes: Long,
    val elapsedMs: Long,
    /** Null while no reliable estimate exists (first 1.5 s, or no progress yet). */
    val etaMs: Long?,
    /** Fraction reached once the part in progress is finished: the bar may glide toward it. */
    val nextFraction: Float = 0f,
) {
    val fraction: Float
        get() = if (totalItems <= 0) 0f else (doneItems.toFloat() / totalItems).coerceIn(0f, 1f)
}

/**
 * Remaining-time estimate from a smoothed rate:
 * smoothedRate = 0.2 * rate + 0.8 * smoothedRate, nothing shown before [minElapsedMs].
 * The rate is measured in items per millisecond between two updates, so steps are weighted by
 * their number of items, not counted as equal parts.
 */
class EtaEstimator(
    private val minElapsedMs: Long = 1_500L,
    private val alpha: Double = 0.2,
) {
    private var lastDone = 0.0
    private var lastTimeMs = 0L
    private var smoothedRate: Double? = null

    fun update(done: Double, total: Double, elapsedMs: Long): Long? {
        val dt = elapsedMs - lastTimeMs
        if (dt > 0 && done > lastDone) {
            val rate = (done - lastDone) / dt
            smoothedRate = smoothedRate?.let { alpha * rate + (1 - alpha) * it } ?: rate
            lastDone = done
            lastTimeMs = elapsedMs
        }
        if (elapsedMs < minElapsedMs) return null
        val rate = smoothedRate ?: return null
        if (rate <= 0.0) return null
        return ((total - done).coerceAtLeast(0.0) / rate).toLong()
    }
}

/** Number of items a backup entry represents: list entries count one each, a settings block counts one. */
internal fun backupItemCount(value: JsonElement?): Int = when (value) {
    null -> 0
    is JsonArray -> value.size.coerceAtLeast(1)
    is JsonObject -> value.values.sumOf { child -> if (child is JsonArray) child.size else 0 }.coerceAtLeast(1)
    else -> 1
}

internal fun backupSectionOf(key: String): BackupSection? =
    BackupSection.entries.firstOrNull { key in it.keys }

/** Tracks one export or import, published to the progress sheet. */
internal class BackupProgressTracker(
    private val operation: BackupOperation,
    /** Each key with its item count, in processing order. */
    private val plan: List<Pair<String, Int>>,
    private val sink: MutableStateFlow<BackupProgressState?>,
) {
    private val start = TimeSource.Monotonic.markNow()
    private val eta = EtaEstimator()
    private val stages = plan.mapNotNull { backupSectionOf(it.first) }.distinct()
    private val total = plan.sumOf { it.second }
    private var done = 0
    private var partial = 0
    private var bytes = 0L
    private var currentKey: String? = plan.firstOrNull()?.first

    init { publish(currentKey) }

    fun keyDone(key: String, addedBytes: Long = 0L) {
        done += plan.firstOrNull { it.first == key }?.second ?: 0
        partial = 0
        bytes += addedBytes
        currentKey = plan.getOrNull(plan.indexOfFirst { it.first == key } + 1)?.first ?: key
        publish(currentKey)
    }

    /** Items of [key] finished so far, for long parts (addons installed one by one). */
    fun partial(key: String, itemsDone: Int) {
        val weight = plan.firstOrNull { it.first == key }?.second ?: return
        partial = itemsDone.coerceIn(0, weight)
        publish(key)
    }

    private fun publish(currentKey: String?) {
        val elapsed = start.elapsedNow().inWholeMilliseconds
        val stage = currentKey?.let(::backupSectionOf)
        val currentWeight = plan.firstOrNull { it.first == currentKey }?.second ?: 0
        val shownDone = done + partial
        sink.value = BackupProgressState(
            operation = operation,
            stage = stage,
            stageIndex = (stages.indexOf(stage) + 1).coerceAtLeast(1),
            stageCount = stages.size.coerceAtLeast(1),
            doneItems = shownDone,
            totalItems = total,
            bytes = bytes,
            elapsedMs = elapsed,
            etaMs = eta.update(shownDone.toDouble(), total.toDouble(), elapsed),
            nextFraction = if (total <= 0) 0f else ((done + currentWeight).toFloat() / total).coerceIn(0f, 1f),
        )
    }
}

internal object BackupProgressRepository {
    internal val mutableState = MutableStateFlow<BackupProgressState?>(null)
    val state: StateFlow<BackupProgressState?> = mutableState.asStateFlow()
}
