package com.nuvio.app.features.cloudstream

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/**
 * CloudStream repositories and installed extensions saved with the profile settings of the Nuvio
 * account, like add-ons and plugins: on another device, the repositories are added back and the
 * extensions reinstalled by themselves. Nothing is ever removed on a device by the sync.
 */
object CloudStreamAccountSync {
    private val log = Logger.withTag("CloudStreamAccountSync")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var applyJob: Job? = null

    /** What the account had, until it has been applied here (kept in what is sent back meanwhile). */
    private var pendingRemote: CloudStreamSyncPayload? = null

    @Serializable
    internal data class CloudStreamSyncPayload(
        @SerialName("repositories") val repositories: List<String> = emptyList(),
        @SerialName("installed") val installed: List<String> = emptyList(),
        @SerialName("disabled") val disabled: List<String> = emptyList(),
        @SerialName("security_accepted") val securityAccepted: Boolean = false,
    )

    private fun local(): CloudStreamSyncPayload {
        val state = CloudStreamRepository.uiState.value
        return CloudStreamSyncPayload(
            repositories = state.repositories.map { it.manifest.sourceUrl },
            installed = state.plugins.filter { it.isInstalled }.map { it.metadata.id.value },
            disabled = state.plugins.filter { it.isInstalled && !it.enabled }.map { it.metadata.id.value },
            securityAccepted = state.securityWarningAccepted,
        )
    }

    /** Changes worth sending to the account. */
    val changes: Flow<String> = CloudStreamRepository.uiState
        .map { "cloudstream:" + local().hashCode() }
        .distinctUntilChanged()

    fun export(): JsonObject {
        val mine = local()
        val merged = pendingRemote?.let { remote ->
            CloudStreamSyncPayload(
                repositories = (mine.repositories + remote.repositories).distinct(),
                installed = (mine.installed + remote.installed).distinct(),
                disabled = (mine.disabled + remote.disabled).distinct(),
                securityAccepted = mine.securityAccepted || remote.securityAccepted,
            )
        } ?: mine
        return json.encodeToJsonElement(CloudStreamSyncPayload.serializer(), merged).jsonObject
    }

    fun apply(payload: JsonObject) {
        if (payload.isEmpty()) return
        val remote = runCatching { json.decodeFromJsonElement(CloudStreamSyncPayload.serializer(), payload) }.getOrNull() ?: return
        if (remote.repositories.isEmpty() && remote.installed.isEmpty()) return
        pendingRemote = remote
        applyJob?.cancel()
        applyJob = scope.launch {
            runCatching {
                CloudStreamRepository.initialize()
                if (remote.securityAccepted && !CloudStreamRepository.uiState.value.securityWarningAccepted) {
                    CloudStreamRepository.acceptSecurityWarning()
                }
                val known = CloudStreamRepository.uiState.value.repositories.map { it.manifest.sourceUrl }.toSet()
                remote.repositories.filterNot { it in known }.forEach { url ->
                    val result = CloudStreamRepository.addRepository(url)
                    if (result is AddCloudStreamRepositoryResult.Error) log.w { "Repository not added: ${result.message}" }
                }
                // The repositories list their extensions in the background: give them a moment.
                for (attempt in 0 until 20) {
                    val ids = CloudStreamRepository.uiState.value.plugins.map { it.metadata.id.value }.toSet()
                    if (remote.installed.all { it in ids }) break
                    delay(1_000)
                }
                val state = CloudStreamRepository.uiState.value
                val installedHere = state.plugins.filter { it.isInstalled }.map { it.metadata.id.value }.toSet()
                val available = state.plugins.map { it.metadata.id.value }.toSet()
                val toInstall = remote.installed.filter { it !in installedHere && it in available }
                if (toInstall.isNotEmpty() && CloudStreamRepository.uiState.value.securityWarningAccepted) {
                    CloudStreamRepository.installAndEnablePlugins(toInstall)
                    remote.disabled.filter { it in toInstall }.forEach { CloudStreamRepository.setPluginEnabled(it, false) }
                }
            }.onFailure { log.w(it) { "CloudStream sync could not be applied" } }
            pendingRemote = null
        }
    }
}
