package app.gamenative.linux.store.steam

interface SteamDownloadGateway {
    suspend fun fetchQueueSnapshot(): SteamDownloadQueueSnapshot
    suspend fun enqueue(appId: Int): Boolean
    suspend fun pause(appId: Int): Boolean
    suspend fun cancel(appId: Int): Boolean
}

class JavaSteamDownloadManager(
    private val gateway: SteamDownloadGateway,
) : SteamDownloadManager {
    private var mirror = InMemorySteamDownloadManager()

    suspend fun bootstrapQueue(): SteamDownloadQueueSnapshot {
        mirror = InMemorySteamDownloadManager(gateway.fetchQueueSnapshot())
        return mirror.queueSnapshot()
    }

    override suspend fun enqueueApp(appId: Int) {
        if (gateway.enqueue(appId)) {
            mirror.enqueueApp(appId)
        }
    }

    override suspend fun pauseApp(appId: Int) {
        if (gateway.pause(appId)) {
            mirror.pauseApp(appId)
        }
    }

    override suspend fun cancelApp(appId: Int) {
        if (gateway.cancel(appId)) {
            mirror.cancelApp(appId)
        }
    }

    override fun queueSnapshot(): SteamDownloadQueueSnapshot = mirror.queueSnapshot()
}
