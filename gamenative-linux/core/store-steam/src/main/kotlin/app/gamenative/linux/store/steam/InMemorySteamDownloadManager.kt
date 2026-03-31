package app.gamenative.linux.store.steam

class InMemorySteamDownloadManager(
    initialSnapshot: SteamDownloadQueueSnapshot = SteamDownloadQueueSnapshot(emptyList(), emptyList()),
) : SteamDownloadManager {
    private val queued = linkedSetOf<Int>()
    private val paused = linkedSetOf<Int>()

    init {
        queued.addAll(initialSnapshot.queuedAppIds)
        paused.addAll(initialSnapshot.pausedAppIds.filter { queued.contains(it) })
    }

    override suspend fun enqueueApp(appId: Int) {
        paused.remove(appId)
        queued.add(appId)
    }

    override suspend fun pauseApp(appId: Int) {
        if (queued.contains(appId)) {
            paused.add(appId)
        }
    }

    override suspend fun cancelApp(appId: Int) {
        queued.remove(appId)
        paused.remove(appId)
    }

    override fun queueSnapshot(): SteamDownloadQueueSnapshot {
        return SteamDownloadQueueSnapshot(
            queuedAppIds = queued.toList(),
            pausedAppIds = paused.toList(),
        )
    }
}
