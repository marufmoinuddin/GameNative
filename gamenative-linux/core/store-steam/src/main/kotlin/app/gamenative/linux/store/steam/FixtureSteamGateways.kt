package app.gamenative.linux.store.steam

import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

class FixtureSteamLibraryGateway(
    private val fixtureFile: Path,
    private val json: Json,
) : SteamLibraryGateway {
    override suspend fun fetchOwnedApps(): List<SteamLibraryRecord> {
        val rootArray = readRootArray() ?: return emptyList()
        return rootArray.mapNotNull { element ->
            val item = element.asObjectOrNull() ?: return@mapNotNull null
            val appId = item.int("id") ?: item.int("appId") ?: return@mapNotNull null
            val name = item.string("name") ?: ""
            SteamLibraryRecord(appId = appId, name = name)
        }
    }

    override suspend fun fetchOwnedApp(appId: Int): SteamLibraryRecord? {
        return fetchOwnedApps().firstOrNull { it.appId == appId }
    }

    private fun readRootArray(): JsonArray? {
        if (!Files.exists(fixtureFile)) {
            return null
        }

        val payload = runCatching { Files.readString(fixtureFile) }.getOrNull() ?: return null
        val root = runCatching { json.parseToJsonElement(payload) }.getOrNull() ?: return null
        return root.asArrayOrNull()
    }

    private fun JsonElement.asArrayOrNull(): JsonArray? = runCatching { jsonArray }.getOrNull()
    private fun JsonElement.asObjectOrNull(): JsonObject? = runCatching { jsonObject }.getOrNull()
    private fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.content?.toIntOrNull()
    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.content
}

class FixtureSteamDownloadGateway(
    private val fixtureFile: Path,
    private val json: Json,
) : SteamDownloadGateway {
    override suspend fun fetchQueueSnapshot(): SteamDownloadQueueSnapshot = loadFixtureQueue()

    override suspend fun enqueue(appId: Int): Boolean = true

    override suspend fun pause(appId: Int): Boolean = true

    override suspend fun cancel(appId: Int): Boolean = true

    fun loadFixtureQueue(): SteamDownloadQueueSnapshot {
        if (!Files.exists(fixtureFile)) {
            return SteamDownloadQueueSnapshot(emptyList(), emptyList())
        }

        val payload = runCatching { Files.readString(fixtureFile) }.getOrNull()
            ?: return SteamDownloadQueueSnapshot(emptyList(), emptyList())
        val root = runCatching { json.parseToJsonElement(payload) }.getOrNull()
            ?: return SteamDownloadQueueSnapshot(emptyList(), emptyList())
        val objectRoot = root.asObjectOrNull() ?: return SteamDownloadQueueSnapshot(emptyList(), emptyList())

        val queued = objectRoot.intList("queuedAppIds")
        val paused = objectRoot.intList("pausedAppIds").filter { queued.contains(it) }
        return SteamDownloadQueueSnapshot(queuedAppIds = queued, pausedAppIds = paused)
    }

    private fun JsonElement.asObjectOrNull(): JsonObject? = runCatching { jsonObject }.getOrNull()

    private fun JsonObject.intList(key: String): List<Int> {
        val element = this[key] ?: return emptyList()
        val array = runCatching { element.jsonArray }.getOrNull() ?: return emptyList()
        return array.mapNotNull { item ->
            (item as? JsonPrimitive)?.content?.toIntOrNull()
        }
    }
}

class FixtureSteamCloudGateway(
    private val fixtureFile: Path,
    private val json: Json,
) : SteamCloudGateway {
    private var statusByAppId: Map<Int, CloudSyncGatewayResult> = loadStatusByAppId()

    override suspend fun syncStatus(appId: Int): CloudSyncGatewayResult? = statusByAppId[appId]

    fun reload(): Int {
        statusByAppId = loadStatusByAppId()
        return statusByAppId.size
    }

    private fun loadStatusByAppId(): Map<Int, CloudSyncGatewayResult> {
        if (!Files.exists(fixtureFile)) {
            return emptyMap()
        }

        val payload = runCatching { Files.readString(fixtureFile) }.getOrNull() ?: return emptyMap()
        val root = runCatching { json.parseToJsonElement(payload) }.getOrNull() ?: return emptyMap()

        val entries = when (root) {
            is JsonArray -> root
            is JsonObject -> root["apps"].asArrayOrNull() ?: return emptyMap()
            else -> return emptyMap()
        }

        return entries.mapNotNull { item ->
            val app = item.asObjectOrNull() ?: return@mapNotNull null
            val appId = app.int("appId") ?: app.int("id") ?: return@mapNotNull null
            val status = app.string("status")
                ?.let { runCatching { CloudSyncStatus.valueOf(it) }.getOrNull() }
                ?: CloudSyncStatus.UNKNOWN
            val details = app.string("details") ?: ""
            appId to CloudSyncGatewayResult(status = status, details = details)
        }.toMap()
    }

    private fun JsonElement.asObjectOrNull(): JsonObject? = runCatching { jsonObject }.getOrNull()

    private fun JsonElement?.asArrayOrNull(): JsonArray? {
        return this?.let { runCatching { it.jsonArray }.getOrNull() }
    }

    private fun JsonObject.int(key: String): Int? {
        return (this[key] as? JsonPrimitive)?.content?.toIntOrNull()
    }

    private fun JsonObject.string(key: String): String? {
        return (this[key] as? JsonPrimitive)?.content
    }
}
