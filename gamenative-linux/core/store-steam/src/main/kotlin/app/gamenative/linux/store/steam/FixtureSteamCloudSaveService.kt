package app.gamenative.linux.store.steam

import java.nio.file.Path
import kotlinx.serialization.json.Json

class FixtureSteamCloudSaveService(
    private val fixtureFile: Path,
    private val json: Json = Json {
        ignoreUnknownKeys = true
    },
) : SteamCloudSaveService {
    private val gateway = FixtureSteamCloudGateway(
        fixtureFile = fixtureFile,
        json = json,
    )
    private val delegate = JavaSteamCloudSaveService(gateway)

    override suspend fun syncApp(appId: Int): CloudSyncResult = delegate.syncApp(appId)

    fun reloadFixture(): SteamCloudFixtureSnapshot {
        return SteamCloudFixtureSnapshot(entries = gateway.reload())
    }
}

data class SteamCloudFixtureSnapshot(
    val entries: Int,
)