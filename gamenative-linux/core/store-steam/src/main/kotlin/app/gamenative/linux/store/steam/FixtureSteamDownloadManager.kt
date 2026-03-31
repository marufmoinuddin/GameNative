package app.gamenative.linux.store.steam

import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.json.Json
import kotlinx.coroutines.runBlocking

class FixtureSteamDownloadManager(
    private val fixtureFile: Path,
    private val json: Json = Json {
        ignoreUnknownKeys = true
    },
) : SteamDownloadManager {
    private val gateway = FixtureSteamDownloadGateway(
        fixtureFile = fixtureFile,
        json = json,
    )
    private val delegate = JavaSteamDownloadManager(gateway)

    init {
        runBlocking {
            delegate.bootstrapQueue()
        }
    }

    override suspend fun enqueueApp(appId: Int) {
        delegate.enqueueApp(appId)
    }

    override suspend fun pauseApp(appId: Int) {
        delegate.pauseApp(appId)
    }

    override suspend fun cancelApp(appId: Int) {
        delegate.cancelApp(appId)
    }

    override fun queueSnapshot(): SteamDownloadQueueSnapshot = delegate.queueSnapshot()

    fun replayFixtureQueue(): SteamDownloadQueueSnapshot {
        return runBlocking {
            delegate.bootstrapQueue()
        }
    }
}