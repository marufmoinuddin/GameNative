package app.gamenative.linux.cli

import app.gamenative.linux.store.steam.SteamDownloadGateway
import app.gamenative.linux.store.steam.SteamDownloadQueueSnapshot

class CliQueueDownloadGateway : SteamDownloadGateway {
    private val queued = linkedSetOf<Int>()
    private val paused = linkedSetOf<Int>()

    override suspend fun fetchQueueSnapshot(): SteamDownloadQueueSnapshot {
        return SteamDownloadQueueSnapshot(
            queuedAppIds = queued.toList(),
            pausedAppIds = paused.toList(),
        )
    }

    override suspend fun enqueue(appId: Int): Boolean {
        paused.remove(appId)
        queued.add(appId)
        return true
    }

    override suspend fun pause(appId: Int): Boolean {
        if (queued.contains(appId)) {
            paused.add(appId)
        }
        return true
    }

    override suspend fun cancel(appId: Int): Boolean {
        queued.remove(appId)
        paused.remove(appId)
        return true
    }
}
